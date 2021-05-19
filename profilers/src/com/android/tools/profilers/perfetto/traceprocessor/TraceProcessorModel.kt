/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.profilers.perfetto.traceprocessor

import com.android.tools.profiler.perfetto.proto.TraceProcessor
import com.android.tools.profiler.proto.Cpu
import com.android.tools.profilers.cpu.ThreadState
import com.android.tools.profilers.cpu.systemtrace.CounterModel
import com.android.tools.profilers.cpu.systemtrace.CpuCoreModel
import com.android.tools.profilers.cpu.systemtrace.ProcessModel
import com.android.tools.profilers.cpu.systemtrace.SchedulingEventModel
import com.android.tools.profilers.cpu.systemtrace.SystemTraceModelAdapter
import com.android.tools.profilers.cpu.systemtrace.ThreadModel
import com.android.tools.profilers.cpu.systemtrace.TraceEventModel
import java.io.Serializable
import java.util.Deque
import java.util.LinkedList
import java.util.concurrent.TimeUnit

class TraceProcessorModel(builder: Builder) : SystemTraceModelAdapter, Serializable {

  companion object {
    // generated by serialver
    @JvmStatic
    val serialVersionUID = -2228053132990163476L
  }

  private val processMap: Map<Int, ProcessModel>
  private val cpuCores: List<CpuCoreModel>

  private val danglingThreads = builder.danglingThreads

  private val startCaptureTimestamp = builder.startCaptureTimestamp
  private val endCaptureTimestamp = builder.endCaptureTimestamp

  init {
    // Build processMap
    val processMapBuilder = mutableMapOf<Int, ProcessModel>()
    for (process in builder.processById.values) {
      val updatedThreadMap = process.threadById.mapValues { entry ->
        entry.value.copy(
          traceEvents = builder.threadToEventsMap.getOrDefault(entry.key, listOf()),
          schedulingEvents = builder.threadToScheduling.getOrDefault(entry.key, listOf())
        )
      }.toSortedMap()
      val counterMap = builder.processToCounters.getOrDefault(process.id, listOf())
        .map { it.name to it }
        .toMap()
      processMapBuilder[process.id] = process.copy(threadById = updatedThreadMap, counterByName = counterMap)
    }
    processMap = processMapBuilder.toSortedMap()

    // Build cpuCores
    cpuCores = (0 until builder.cpuCoresCount).map {
      val cpuCountersMap = builder.coreToCpuCounters.getOrDefault(it, listOf())
        .map { counterModel -> counterModel.name to counterModel }
        .toMap()
      CpuCoreModel(it, builder.coreToScheduling.getOrDefault(it, listOf()), cpuCountersMap)
    }
  }

  override fun getCaptureStartTimestampUs() = startCaptureTimestamp
  override fun getCaptureEndTimestampUs() = endCaptureTimestamp

  override fun getProcessById(id: Int) = processMap[id]
  override fun getProcesses() = processMap.values.toList()
  override fun getDanglingThread(tid: Int): ThreadModel? = danglingThreads[tid]

  override fun getCpuCores() = cpuCores

  override fun getSystemTraceTechnology() = Cpu.CpuTraceType.PERFETTO

  // TODO(b/156578844): Fetch data from TraceProcessor error table to populate this.
  override fun isCapturePossibleCorrupted() = false

  class Builder {
    internal var startCaptureTimestamp = Long.MAX_VALUE
    internal var endCaptureTimestamp = Long.MIN_VALUE
    internal var cpuCoresCount = 0
    internal val processById = mutableMapOf<Int, ProcessModel>()
    internal val danglingThreads = mutableMapOf<Int, ThreadModel>()
    internal val threadToEventsMap = mutableMapOf<Int, List<TraceEventModel>>()
    internal val threadToScheduling = mutableMapOf<Int, List<SchedulingEventModel>>()
    internal val coreToScheduling = mutableMapOf<Int, List<SchedulingEventModel>>()
    internal val coreToCpuCounters = mutableMapOf<Int, List<CounterModel>>()
    internal val processToCounters = mutableMapOf<Int, List<CounterModel>>()

    fun addProcessMetadata(processMetadataResult: TraceProcessor.ProcessMetadataResult) {
      for (process in processMetadataResult.processList) {
        processById[process.id.toInt()] = ProcessModel(
          process.id.toInt(),
          process.name,
          process.threadList.map { t -> t.id.toInt() to ThreadModel(t.id.toInt(), process.id.toInt(), t.name, listOf(), listOf()) }
            .toMap().toSortedMap(),
          mapOf())
      }

      for (thread in processMetadataResult.danglingThreadList) {
        danglingThreads[thread.id.toInt()] = ThreadModel(thread.id.toInt(), 0, thread.name, emptyList(), emptyList())
      }
    }

    fun addTraceEvents(traceEventsResult: TraceProcessor.TraceEventsResult) {
      for (thread in traceEventsResult.threadList) {
        val rootIds = mutableSetOf<Long>()
        val eventToChildrenIds = mutableMapOf<Long, MutableList<Long>>()
        val eventPerId = mutableMapOf<Long, TraceEventModel>()

        for (event in thread.traceEventList) {
          if (event.depth > 0) {
            eventToChildrenIds.getOrPut(event.parentId) { mutableListOf() }.add(event.id)
          }
          else {
            rootIds.add(event.id)
          }

          val startTimestampUs = convertToUs(event.timestampNanoseconds)
          val durationTimestampUs = convertToUs(event.durationNanoseconds)
          val endTimestampUs = startTimestampUs + durationTimestampUs

          eventPerId[event.id] = TraceEventModel(event.name,
                                                 startTimestampUs,
                                                 endTimestampUs,
                                                 durationTimestampUs,
                                                 listOf())
        }

        val reconstructedTree = reconstructTraceTree(rootIds, eventToChildrenIds, eventPerId)
        threadToEventsMap[thread.threadId.toInt()] = rootIds.mapNotNull { reconstructedTree[it] }
      }
    }

    // Runs through the partially computed events to rebuild the whole trace trees, by doing a DFS from the root nodes.
    private fun reconstructTraceTree(
      rootIds: Set<Long>, eventToChildrenIds: Map<Long, List<Long>>, eventPerId: Map<Long, TraceEventModel>): Map<Long, TraceEventModel> {

      val reconstructedEventsPerId = mutableMapOf<Long, TraceEventModel>()

      val visitedAllChildren = mutableSetOf<Long>()
      val eventIdStack: Deque<Long> = LinkedList(rootIds)

      while (eventIdStack.isNotEmpty()) {
        val eventId = eventIdStack.first

        // If we have not visited this node yet, then we need to push all its children to the front of the queue
        // and continue the main loop. Next time we pass on this one, we will process it as we know all its children
        // have been processed already.
        if (!visitedAllChildren.contains(eventId)) {
          eventToChildrenIds.getOrDefault(eventId, mutableListOf()).forEach { eventIdStack.addFirst(it) }
          visitedAllChildren.add(eventId)
          continue
        }

        eventIdStack.removeFirst()
        val children = eventToChildrenIds.getOrDefault(eventId, mutableListOf())
          .map { reconstructedEventsPerId[it] ?: error("Children should have been computed already") }
          .sortedBy { it.startTimestampUs }

        val event = eventPerId[eventId] ?: error("Trace Event should be present in the map")

        val myStart = event.startTimestampUs
        val maxEndTs = children.lastOrNull()?.endTimestampUs ?: 0L
        val myCpuTime = event.cpuTimeUs

        val updatedEvent = event.copy(
          // Our end time is either the end of our last children or our start + how much time we took.
          endTimestampUs = maxOf(myStart + myCpuTime, maxEndTs),
          childrenEvents = children)
        reconstructedEventsPerId[eventId] = updatedEvent

        // Update the global start/end of the capture.
        startCaptureTimestamp = minOf(startCaptureTimestamp, updatedEvent.startTimestampUs)
        endCaptureTimestamp = maxOf(endCaptureTimestamp, updatedEvent.endTimestampUs)
      }

      return reconstructedEventsPerId
    }

    fun addSchedulingEvents(schedEvents: TraceProcessor.SchedulingEventsResult) {
      cpuCoresCount = maxOf(cpuCoresCount, schedEvents.numCores)

      val perThreadScheduling = mutableMapOf<Int, MutableList<SchedulingEventModel>>()
      val perCoreScheduling = mutableMapOf<Int, MutableList<SchedulingEventModel>>()
      for (event in schedEvents.schedEventList) {
        val startTimestampUs = convertToUs(event.timestampNanoseconds)
        val durationTimestampUs = convertToUs(event.durationNanoseconds)
        val endTimestampUs = startTimestampUs + durationTimestampUs
        startCaptureTimestamp = minOf(startCaptureTimestamp, startTimestampUs)
        endCaptureTimestamp = maxOf(endCaptureTimestamp, endTimestampUs)
        val schedEvent = SchedulingEventModel(convertSchedulingState(event.state),
                                              startTimestampUs,
                                              endTimestampUs,
                                              durationTimestampUs,
                                              durationTimestampUs,
                                              event.processId.toInt(),
                                              event.threadId.toInt(),
                                              event.cpu)

        perThreadScheduling.getOrPut(event.threadId.toInt()) { mutableListOf() }.add(schedEvent)
        perCoreScheduling.getOrPut(event.cpu) { mutableListOf() }.add(schedEvent)
      }

      perThreadScheduling.forEach {
        val previousList = threadToScheduling[it.key] ?: listOf()
        threadToScheduling[it.key] = previousList.plus(it.value).sortedBy { s -> s.startTimestampUs }
      }
      perCoreScheduling.forEach {
        val previousList = coreToScheduling[it.key] ?: listOf()
        coreToScheduling[it.key] = previousList.plus(it.value).sortedBy { s -> s.startTimestampUs }
      }
    }

    private fun convertSchedulingState(state: TraceProcessor.SchedulingEventsResult.SchedulingEvent.SchedulingState): ThreadState {
      return when (state) {
        TraceProcessor.SchedulingEventsResult.SchedulingEvent.SchedulingState.RUNNING -> ThreadState.RUNNING_CAPTURED
        TraceProcessor.SchedulingEventsResult.SchedulingEvent.SchedulingState.RUNNING_FOREGROUND -> ThreadState.RUNNING_CAPTURED
        TraceProcessor.SchedulingEventsResult.SchedulingEvent.SchedulingState.DEAD -> ThreadState.DEAD_CAPTURED
        TraceProcessor.SchedulingEventsResult.SchedulingEvent.SchedulingState.SLEEPING -> ThreadState.SLEEPING_CAPTURED
        TraceProcessor.SchedulingEventsResult.SchedulingEvent.SchedulingState.SLEEPING_UNINTERRUPTIBLE -> ThreadState.WAITING_CAPTURED
        else -> ThreadState.UNKNOWN
      }
    }

    fun addCpuCounters(result: TraceProcessor.CpuCoreCountersResult) {
      cpuCoresCount = maxOf(cpuCoresCount, result.numCores)

      result.countersPerCoreList.forEach { countersPerCore ->
        coreToCpuCounters[countersPerCore.cpu] = countersPerCore.counterList.map { counter ->
          CounterModel(counter.name,
                       counter.valueList.map { convertToUs(it.timestampNanoseconds) to it.value }
                         .toMap().toSortedMap())
        }
      }
    }

    fun addProcessCounters(counters: TraceProcessor.ProcessCountersResult) {
      processToCounters[counters.processId.toInt()] = counters.counterList.map { counter ->
        CounterModel(counter.name,
                     counter.valueList.map { convertToUs(it.timestampNanoseconds) to it.value }
                       .toMap().toSortedMap())
      }
    }

    fun build(): TraceProcessorModel {
      return TraceProcessorModel(this)
    }

    private fun convertToUs(tsNanos: Long) = TimeUnit.NANOSECONDS.toMicros(tsNanos)
  }
}
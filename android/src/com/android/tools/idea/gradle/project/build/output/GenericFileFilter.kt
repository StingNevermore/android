/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.build.output

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

enum class ParsingState {
  NORMAL, PATH
}

/**
 *  Filter that highlights absolute path as hyperlinks in console output. This filter is effective in all console output, including build
 *  output, sync output, run test output, built-in terminal emulator, etc. Therefore, we manually parse the string instead of using regex
 *  for maximum performance.
 */
class GenericFileFilter(private val project: Project, private val localFileSystem: LocalFileSystem) : Filter {
  override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
    val indexOffset = entireLength - line.length
    val items = mutableListOf<Filter.ResultItem>()
    var state = ParsingState.NORMAL
    var pathStartIndex = -1
    var i = 0
    while (i < line.length) {
      when (state) {
        ParsingState.NORMAL -> {
          when {
            line[i] == '/' -> {
              // Start parsing a Linux path
              state = ParsingState.PATH
              pathStartIndex = i
            }
            line[i] in 'A'..'Z' && (line.startsWith(":\\", startIndex = i + 1) || line.startsWith(":/", startIndex = i + 1) ) -> {
              // Start parsing a Windows path
              state = ParsingState.PATH
              pathStartIndex = i
              i += 2
            }
          }
        }
        ParsingState.PATH -> {
          fun addItem(pathEndIndex: Int, lineNumber: Int, columnNumber: Int) {
            state = ParsingState.NORMAL
            val path = line.substring(pathStartIndex, pathEndIndex)
            val file = localFileSystem.findFileByPathIfCached(path)
            if (file != null) {
              items += Filter.ResultItem(
                indexOffset + pathStartIndex,
                indexOffset + i,
                OpenFileHyperlinkInfo(project, file, lineNumber, columnNumber))
            }
          }
          when {
            line[i] == ':' -> {
              val pathEndIndex = i
              val lineNumber = line.takeWhileFromIndex(i + 1) { it.isDigit() }?.also { i += it.length }?.toInt() ?: 1
              val columnNumber =
                if (line.getOrNull(++i) == ':')
                  line.takeWhileFromIndex(++i) { it.isDigit() }?.also { i += it.length }?.toInt() ?: 1
                else
                  1
              addItem(pathEndIndex, lineNumber - 1, columnNumber - 1)
            }
            line[i].isWhitespace() -> addItem(i, 0, 0)
          }
        }
      }
      i++
    }
    if (items.isEmpty()) return null
    return Filter.Result(items)
  }
}

private fun String.takeWhileFromIndex(index: Int, predicate: (Char) -> Boolean): String? {
  for (i in index until length) {
    if (!predicate(get(i))) {
      return if (i == index) null else substring(index, i)
    }
  }
  return null
}

class GenericFileFilterProvider : ConsoleFilterProvider {
  override fun getDefaultFilters(project: Project) = arrayOf(GenericFileFilter(project, LocalFileSystem.getInstance()))
}


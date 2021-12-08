/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.explorer.adbimpl

import com.android.ddmlib.ShellCommandUnresponsiveException
import com.android.tools.idea.explorer.adbimpl.AdbFileListingEntry.EntryKind
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import org.hamcrest.core.IsInstanceOf
import org.jetbrains.ide.PooledThreadExecutor
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.awt.EventQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class AdbFileListingTest {
  @get:Rule
  var thrown = ExpectedException.none()

  @Test
  fun test_Nexus7Api23_GetRoot() {
    // Prepare
    val commands = TestShellCommands()
    TestDevices.addNexus7Api23Commands(commands)
    val device = commands.createMockDevice()
    val taskExecutor: Executor = PooledThreadExecutor.INSTANCE
    val fileListing = AdbFileListing(device, AdbDeviceCapabilities(device), taskExecutor)

    // Act
    val root = waitForFuture(fileListing.root)

    // Assert
    assertThat(root).isNotNull()
    assertThat(root.fullPath).isEqualTo("/")
    assertThat(root.name).isEqualTo("")
    assertThat(root.isDirectory).isTrue()
  }

  @Test
  fun test_Nexus7Api23__GetRootChildrenError() {
    // Prepare
    val commands = TestShellCommands()
    TestDevices.addNexus7Api23Commands(commands)
    commands.addError("ls -al /" + TestDevices.COMMAND_ERROR_CHECK_SUFFIX, ShellCommandUnresponsiveException())
    val device = commands.createMockDevice()
    val taskExecutor = PooledThreadExecutor.INSTANCE
    val fileListing = AdbFileListing(device, AdbDeviceCapabilities(device), taskExecutor)

    // Act
    val root = waitForFuture(fileListing.root)

    // Assert
    thrown.expect(ExecutionException::class.java)
    thrown.expectCause(IsInstanceOf.instanceOf(ShellCommandUnresponsiveException::class.java))
    waitForFuture(fileListing.getChildren(root))
  }

  @Test
  fun test_Nexus7Api23_GetRootChildren() {
    // Prepare
    val commands = TestShellCommands()
    TestDevices.addNexus7Api23Commands(commands)
    val device = commands.createMockDevice()
    val taskExecutor: Executor = PooledThreadExecutor.INSTANCE
    val fileListing = AdbFileListing(device, AdbDeviceCapabilities(device), taskExecutor)

    // Act
    val root = waitForFuture(fileListing.root)
    val rootEntries = waitForFuture(fileListing.getChildren(root))

    // Assert
    assertThat(rootEntries).isNotNull()
    assertThat(rootEntries.find { it.name == "acct" }).isNotNull()
    assertThat(rootEntries.find { it.name == "charger" }).isNotNull()
    assertThat(rootEntries.find { it.name == "vendor" }).isNotNull()
    assertThat(rootEntries.find { it.name == "init" }).isNull()
    assertEntry(rootEntries, "acct") { entry: AdbFileListingEntry ->
      assertThat(entry.isDirectory).isTrue()
      assertThat(entry.isFile).isFalse()
      assertThat(entry.isSymbolicLink).isFalse()
      assertThat(entry.permissions).isEqualTo("drwxr-xr-x")
      assertThat(entry.owner).isEqualTo("root")
      assertThat(entry.group).isEqualTo("root")
      assertThat(entry.date).isEqualTo("2016-11-21")
      assertThat(entry.time).isEqualTo("12:09")
      assertThat(entry.info).isNull()
    }
    assertEntry(rootEntries, "cache") { entry: AdbFileListingEntry ->
      assertThat(entry.isDirectory).isTrue()
      assertThat(entry.isFile).isFalse()
      assertThat(entry.isSymbolicLink).isFalse()
      assertThat(entry.permissions).isEqualTo("drwxrwx---")
      assertThat(entry.owner).isEqualTo("system")
      assertThat(entry.group).isEqualTo("cache")
      assertThat(entry.date).isEqualTo("2016-08-26")
      assertThat(entry.time).isEqualTo("12:12")
      assertThat(entry.info).isNull()
    }
    assertEntry(rootEntries, "charger") { entry: AdbFileListingEntry ->
      assertThat(entry.isDirectory).isFalse()
      assertThat(entry.isFile).isFalse()
      assertThat(entry.isSymbolicLink).isTrue()
      assertThat(entry.permissions).isEqualTo("lrwxrwxrwx")
      assertThat(entry.owner).isEqualTo("root")
      assertThat(entry.group).isEqualTo("root")
      assertThat(entry.date).isEqualTo("1969-12-31")
      assertThat(entry.time).isEqualTo("16:00")
      assertThat(entry.info).isEqualTo("-> /sbin/healthd")
    }
    assertEntry(rootEntries, "etc") { entry: AdbFileListingEntry ->
      assertThat(entry.isDirectory).isFalse()
      assertThat(entry.isFile).isFalse()
      assertThat(entry.isSymbolicLink).isTrue()
      assertThat(entry.permissions).isEqualTo("lrwxrwxrwx")
      assertThat(entry.owner).isEqualTo("root")
      assertThat(entry.group).isEqualTo("root")
      assertThat(entry.date).isEqualTo("2016-11-21")
      assertThat(entry.time).isEqualTo("12:09")
      assertThat(entry.info).isEqualTo("-> /system/etc")
    }
  }

  @Test
  fun test_Nexus7Api23_IsDirectoryLink() {
    // Prepare
    val commands = TestShellCommands()
    TestDevices.addNexus7Api23Commands(commands)
    val device = commands.createMockDevice()
    val taskExecutor: Executor = PooledThreadExecutor.INSTANCE
    val fileListing = AdbFileListing(device, AdbDeviceCapabilities(device), taskExecutor)

    // Act
    val root = waitForFuture(fileListing.root)
    val rootEntries = waitForFuture(fileListing.getChildren(root))

    // Assert
    assertThat(rootEntries).isNotNull()
    assertDirectoryLink(fileListing, rootEntries, "charger", false)
    assertDirectoryLink(fileListing, rootEntries, "d", true)
    assertDirectoryLink(fileListing, rootEntries, "etc", true)
    assertDirectoryLink(fileListing, rootEntries, "sdcard", true)
    assertDirectoryLink(fileListing, rootEntries, "tombstones", false)
    assertDirectoryLink(fileListing, rootEntries, "vendor", true)
  }

  @Test
  fun test_EmulatorApi25_GetRoot() {
    // Prepare
    val commands = TestShellCommands()
    TestDevices.addNexus7Api23Commands(commands)
    val device = commands.createMockDevice()
    val taskExecutor: Executor = PooledThreadExecutor.INSTANCE
    val fileListing = AdbFileListing(device, AdbDeviceCapabilities(device), taskExecutor)

    // Act
    val root = waitForFuture(fileListing.root)

    // Assert
    assertThat(root).isNotNull()
    assertThat(root.fullPath).isEqualTo("/")
    assertThat(root.name).isEqualTo("")
    assertThat(root.isDirectory).isTrue()
  }

  @Test
  fun test_EmulatorApi25_GetRootChildrenError() {
    // Prepare
    val commands = TestShellCommands()
    TestDevices.addEmulatorApi25Commands(commands)
    commands.addError("su 0 sh -c 'ls -al /'" + TestDevices.COMMAND_ERROR_CHECK_SUFFIX, ShellCommandUnresponsiveException())
    val device = commands.createMockDevice()
    val taskExecutor: Executor = PooledThreadExecutor.INSTANCE
    val fileListing = AdbFileListing(device, AdbDeviceCapabilities(device), taskExecutor)

    // Act
    val root = waitForFuture(fileListing.root)

    // Assert
    thrown.expect(ExecutionException::class.java)
    thrown.expectCause(IsInstanceOf.instanceOf(ShellCommandUnresponsiveException::class.java))
    waitForFuture(fileListing.getChildren(root))
  }

  @Test
  fun test_EmulatorApi25_GetRootChildren() {
    // Prepare
    val commands = TestShellCommands()
    TestDevices.addEmulatorApi25Commands(commands)
    val device = commands.createMockDevice()
    val taskExecutor: Executor = PooledThreadExecutor.INSTANCE
    val fileListing = AdbFileListing(device, AdbDeviceCapabilities(device), taskExecutor)

    // Act
    val root = waitForFuture(fileListing.root)
    val rootEntries = waitForFuture(fileListing.getChildren(root))

    // Assert
    assertThat(rootEntries).isNotNull()
    assertThat(rootEntries.find { it.name == "acct" }).isNotNull()
    assertThat(rootEntries.find { it.name == "charger" }).isNotNull()
    assertThat(rootEntries.find { it.name == "vendor" }).isNotNull()
    assertThat(rootEntries.find { it.name == "init" }).isNotNull()
    assertEntry(rootEntries, "acct") { entry: AdbFileListingEntry ->
      assertThat(entry.isDirectory).isTrue()
      assertThat(entry.isFile).isFalse()
      assertThat(entry.isSymbolicLink).isFalse()
      assertThat(entry.permissions).isEqualTo("drwxr-xr-x")
      assertThat(entry.owner).isEqualTo("root")
      assertThat(entry.group).isEqualTo("root")
      assertThat(entry.date).isEqualTo("2017-03-06")
      assertThat(entry.time).isEqualTo("21:15")
      assertThat(entry.info).isNull()
    }
    assertEntry(rootEntries, "cache") { entry: AdbFileListingEntry ->
      assertThat(entry.isDirectory).isTrue()
      assertThat(entry.isFile).isFalse()
      assertThat(entry.isSymbolicLink).isFalse()
      assertThat(entry.permissions).isEqualTo("drwxrwx---")
      assertThat(entry.owner).isEqualTo("system")
      assertThat(entry.group).isEqualTo("cache")
      assertThat(entry.date).isEqualTo("2016-12-10")
      assertThat(entry.time).isEqualTo("21:19")
      assertThat(entry.info).isNull()
    }
    assertEntry(rootEntries, "charger") { entry: AdbFileListingEntry ->
      assertThat(entry.isDirectory).isFalse()
      assertThat(entry.isFile).isFalse()
      assertThat(entry.isSymbolicLink).isTrue()
      assertThat(entry.permissions).isEqualTo("lrwxrwxrwx")
      assertThat(entry.owner).isEqualTo("root")
      assertThat(entry.group).isEqualTo("root")
      assertThat(entry.date).isEqualTo("1969-12-31")
      assertThat(entry.time).isEqualTo("16:00")
      assertThat(entry.info).isEqualTo("-> /sbin/healthd")
    }
    assertEntry(rootEntries, "etc") { entry: AdbFileListingEntry ->
      assertThat(entry).isNotNull()
      assertThat(entry.isDirectory).isFalse()
      assertThat(entry.isFile).isFalse()
      assertThat(entry.isSymbolicLink).isTrue()
      assertThat(entry.permissions).isEqualTo("lrwxrwxrwx")
      assertThat(entry.owner).isEqualTo("root")
      assertThat(entry.group).isEqualTo("root")
      assertThat(entry.date).isEqualTo("1969-12-31")
      assertThat(entry.time).isEqualTo("16:00")
      assertThat(entry.info).isEqualTo("-> /system/etc")
    }
  }

  @Test
  fun whenLsEscapes() {
    val commands = TestShellCommands()
    TestDevices.addWhenLsEscapesCommands(commands)
    val device = commands.createMockDevice()
    val listing = AdbFileListing(device, AdbDeviceCapabilities(device), PooledThreadExecutor.INSTANCE)
    val dir = AdbFileListingEntry(
      "/sdcard/dir",
      EntryKind.DIRECTORY,
      "drwxrwx--x",
      "root",
      "sdcard_rw",
      "2018-01-10",
      "12:56",
      "4096",
      null
    )
    assertThat(waitForFuture(listing.getChildrenRunAs(dir, null))[0].name).isEqualTo("dir with spaces")
  }

  @Test
  fun whenLsDoesNotEscape() {
    val commands = TestShellCommands()
    TestDevices.addWhenLsDoesNotEscapeCommands(commands)
    val device = commands.createMockDevice()
    val listing = AdbFileListing(device, AdbDeviceCapabilities(device), PooledThreadExecutor.INSTANCE)
    val dir = AdbFileListingEntry(
      "/sdcard/dir",
      EntryKind.DIRECTORY,
      "drwxrwx--x",
      "root",
      "sdcard_rw",
      "2018-01-10",
      "15:00",
      "4096",
      null
    )
    assertThat(waitForFuture(listing.getChildrenRunAs(dir, null))[0].name).isEqualTo("dir with spaces")
  }

  @Test
  fun test_EmulatorApi25_IsDirectoryLink() {
    // Prepare
    val commands = TestShellCommands()
    TestDevices.addEmulatorApi25Commands(commands)
    val device = commands.createMockDevice()
    val taskExecutor: Executor = PooledThreadExecutor.INSTANCE
    val fileListing = AdbFileListing(device, AdbDeviceCapabilities(device), taskExecutor)

    // Act
    val root = waitForFuture(fileListing.root)
    val rootEntries = waitForFuture(fileListing.getChildren(root))

    // Assert
    assertThat(rootEntries).isNotNull()
    assertDirectoryLink(fileListing, rootEntries, "charger", false)
    assertDirectoryLink(fileListing, rootEntries, "d", true)
    assertDirectoryLink(fileListing, rootEntries, "etc", true)
    assertDirectoryLink(fileListing, rootEntries, "sdcard", true)
    assertDirectoryLink(fileListing, rootEntries, "system", false)
    assertDirectoryLink(fileListing, rootEntries, "vendor", true)
  }

  companion object {
    private const val TIMEOUT_MILLISECONDS: Long = 30000

    private fun assertDirectoryLink(
      fileListing: AdbFileListing,
      entries: List<AdbFileListingEntry>,
      name: String,
      value: Boolean
    ) {
      val entry = checkNotNull(entries.find { it.name == name })
      assertThat(waitForFuture(fileListing.isDirectoryLink(entry))).isEqualTo(value)
    }

    private fun assertEntry(
      entries: List<AdbFileListingEntry>,
      name: String,
      consumer: Consumer<AdbFileListingEntry>
    ) {
      val entry = checkNotNull(entries.find { it.name == name })
      consumer.accept(entry)
    }

    private fun <V> waitForFuture(future: ListenableFuture<V>): V {
      assert(!EventQueue.isDispatchThread())
      return future.get(TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
    }
  }
}
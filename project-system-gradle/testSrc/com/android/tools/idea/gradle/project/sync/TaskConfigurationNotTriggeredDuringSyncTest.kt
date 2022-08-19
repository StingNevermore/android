/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.tools.idea.gradle.project.sync

import com.android.tools.idea.gradle.project.GradleExperimentalSettings
import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.idea.testing.GradleIntegrationTest
import com.android.tools.idea.testing.TestProjectToSnapshotPaths.SIMPLE_APPLICATION
import com.android.tools.idea.testing.onEdt
import com.android.tools.idea.testing.openPreparedProject
import com.android.tools.idea.testing.prepareGradleProject
import com.google.common.truth.Expect
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.testFramework.RunsInEdt
import com.intellij.util.PathUtil
import org.jetbrains.android.AndroidTestBase
import org.junit.Rule
import org.junit.Test
import java.io.File

@RunsInEdt
class TaskConfigurationNotTriggeredDuringSyncTest : GradleIntegrationTest {

  @get:Rule
  val projectRule = AndroidProjectRule.withAndroidModels().onEdt()

  @get:Rule
  val expect: Expect = Expect.createAndEnableStackTrace()

  @Test
  fun testTasksAreNotConfiguredDuringSync() {
    // We do not allow Gradle tasks configuration by default on AS.
    assertThat(GradleExperimentalSettings.getInstance().SKIP_GRADLE_TASKS_LIST).isEqualTo(true)
    val listener = object : ExternalSystemTaskNotificationListenerAdapter() {
      var messagesLog = StringBuilder()
      var finalMessage = ""

      override fun onEnd(id: ExternalSystemTaskId) {
        super.onEnd(id)
        finalMessage = messagesLog.toString()
      }

      override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) {
        super.onStatusChange(event)
        messagesLog.append(event.toString())
      }
    }
    ExternalSystemProgressNotificationManager.getInstance().addNotificationListener(listener)

    val prepared = prepareGradleProject(SIMPLE_APPLICATION, "project")

    val buildFile = prepared.resolve("app").resolve("build.gradle")
    buildFile.writeText(
      buildFile.readText() + """
          tasks.register("shouldNotBeConfigured", Test.class).configure {
              throw RuntimeException("task should not be configured")
          }
        """.trimIndent()
    )
    openPreparedProject("project") { project ->
      assertThat(listener.finalMessage.contains("Can not load tasks for project ':app'")).isFalse()
    }
  }

  override fun getBaseTestPath(): String = projectRule.fixture.tempDirPath
  override fun getTestDataDirectoryWorkspaceRelativePath(): String = "tools/adt/idea/android/testData/snapshots"
  override fun getAdditionalRepos(): Collection<File> =
    listOf(File(AndroidTestBase.getTestDataPath(), PathUtil.toSystemDependentName(SIMPLE_APPLICATION)))
}
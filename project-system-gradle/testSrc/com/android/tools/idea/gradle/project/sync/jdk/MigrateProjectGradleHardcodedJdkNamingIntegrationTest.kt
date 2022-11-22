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
package com.android.tools.idea.gradle.project.sync.jdk

import com.android.tools.idea.gradle.project.AndroidGradleProjectSettingsControlBuilder.Companion.ANDROID_STUDIO_JAVA_HOME_NAME
import com.android.tools.idea.gradle.project.AndroidGradleProjectSettingsControlBuilder.Companion.EMBEDDED_JDK_NAME
import com.android.tools.idea.gradle.project.sync.constants.JDK_11_PATH
import com.android.tools.idea.gradle.project.sync.constants.JDK_17
import com.android.tools.idea.gradle.project.sync.constants.JDK_17_PATH
import com.android.tools.idea.gradle.project.sync.model.GradleRoot
import com.android.tools.idea.gradle.project.sync.snapshots.JdkIntegrationTest
import com.android.tools.idea.gradle.project.sync.snapshots.JdkTestProject
import com.android.tools.idea.gradle.project.sync.utils.JdkTableUtils
import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.idea.testing.IntegrationTestEnvironmentRule
import com.google.common.truth.Expect
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkException
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.JAVA_HOME
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_JAVA_HOME
import com.intellij.testFramework.RunsInEdt
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@RunsInEdt
class MigrateProjectGradleHardcodedJdkNamingIntegrationTest {

  @get:Rule
  val projectRule: IntegrationTestEnvironmentRule = AndroidProjectRule.withIntegrationTestEnvironment()

  @get:Rule
  val expect: Expect = Expect.createAndEnableStackTrace()

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  private val jdkIntegrationTest = JdkIntegrationTest(projectRule, temporaryFolder, expect)

  @Test
  fun `Given projectJdk as 'Embedded JDK' When pre-sync project Then this was migrated to vendor plus version JDK naming`() =
    jdkIntegrationTest.run(
      project = JdkTestProject.SimpleApplication(
        ideaGradleJdk = JDK_17,
        ideaProjectJdk = EMBEDDED_JDK_NAME
      ),
      environment = JdkIntegrationTest.TestEnvironment(
        jdkTable = listOf(
          JdkTableUtils.Jdk(JDK_17, JDK_17_PATH),
          JdkTableUtils.Jdk(EMBEDDED_JDK_NAME, JDK_11_PATH)
        )
      )
    ) {
      syncWithAssertion(
        expectedGradleJdkName = JDK_17,
        expectedProjectJdkName = JDK_17,
        expectedJdkPath = JDK_17_PATH
      )
    }

  @Test
  fun `Given gradleJdk as 'Embedded JDK' When pre-sync project Then this was migrated to vendor plus version JDK naming`() =
    jdkIntegrationTest.run(
      project = JdkTestProject.SimpleApplication(
        ideaGradleJdk = EMBEDDED_JDK_NAME,
        ideaProjectJdk = JDK_17
      ),
      environment = JdkIntegrationTest.TestEnvironment(
        jdkTable = listOf(JdkTableUtils.Jdk(EMBEDDED_JDK_NAME, JDK_17_PATH))
      )
    ) {
      syncWithAssertion(
        expectedGradleJdkName = JDK_17,
        expectedProjectJdkName = JDK_17,
        expectedJdkPath = JDK_17_PATH
      )
    }

  @Test
  fun `Given gradleJdk and projectJdk as 'Embedded JDK' When pre-sync project Then this was migrated to vendor plus version JDK naming`() =
    jdkIntegrationTest.run(
      project = JdkTestProject.SimpleApplication(
        ideaGradleJdk = EMBEDDED_JDK_NAME,
        ideaProjectJdk = EMBEDDED_JDK_NAME
      ),
      environment = JdkIntegrationTest.TestEnvironment(
        jdkTable = listOf(JdkTableUtils.Jdk(EMBEDDED_JDK_NAME, JDK_11_PATH))
      )
    ) {
      syncWithAssertion(
        expectedGradleJdkName = JDK_17,
        expectedProjectJdkName = JDK_17,
        expectedJdkPath = JDK_17_PATH
      )
    }

  @Test
  fun `Given gradleJdk 'Embedded JDK' When pre-sync failure project Then this was migrated to vendor plus version JDK naming`() =
    jdkIntegrationTest.run(
      project = JdkTestProject.SimpleApplication(
        ideaGradleJdk = EMBEDDED_JDK_NAME
      ),
    ) {
      sync(
        assertInMemoryConfig = { assertGradleJdk(JDK_17) },
        assertOnDiskConfig = { assertGradleJdk(JDK_17) },
        assertOnFailure = { assertException(ExternalSystemJdkException::class) }
      )
    }

  @Test
  fun `Given projectJdk as 'Android Studio java home' When pre-sync project Then this was migrated to #JAVA_HOME macro`() =
    jdkIntegrationTest.run(
      project = JdkTestProject.SimpleApplication(
        ideaGradleJdk = JDK_17,
        ideaProjectJdk = ANDROID_STUDIO_JAVA_HOME_NAME
      ),
      environment = JdkIntegrationTest.TestEnvironment(
        jdkTable = listOf(
          JdkTableUtils.Jdk(JDK_17, JDK_17_PATH),
          JdkTableUtils.Jdk(EMBEDDED_JDK_NAME, JDK_11_PATH)
        )
      )
    ) {
      syncWithAssertion(
        expectedGradleJdkName = JDK_17,
        expectedProjectJdkName = JDK_17,
        expectedJdkPath = JDK_17_PATH
      )
    }

  @Test
  fun `Given gradleJdk as 'Android Studio java home' When pre-sync project Then this was migrated to #JAVA_HOME macro`() =
    jdkIntegrationTest.run(
      project = JdkTestProject.SimpleApplication(
        ideaGradleJdk = ANDROID_STUDIO_JAVA_HOME_NAME,
        ideaProjectJdk = JDK_17
      ),
      environment = JdkIntegrationTest.TestEnvironment(
        jdkTable = listOf(
          JdkTableUtils.Jdk(ANDROID_STUDIO_JAVA_HOME_NAME, JDK_17_PATH)
        ),
        environmentVariables = mapOf(JAVA_HOME to JDK_17_PATH)
      )
    ) {
      syncWithAssertion(
        expectedGradleJdkName = USE_JAVA_HOME,
        expectedProjectJdkName = JDK_17,
        expectedJdkPath = JDK_17_PATH
      )
    }

  @Test
  fun `Given gradleJdk 'Android Studio java home' When pre-sync failure project Then this was migrated to #JAVA_HOME macro`() =
    jdkIntegrationTest.run(
      project = JdkTestProject.SimpleApplication(
        ideaGradleJdk = ANDROID_STUDIO_JAVA_HOME_NAME
      ),
    ) {
      sync(
        assertInMemoryConfig = { assertGradleJdk(USE_JAVA_HOME) },
        assertOnDiskConfig = { assertGradleJdk(USE_JAVA_HOME) },
        assertOnFailure = { assertException(ExternalSystemJdkException::class) }
      )
    }

  @Test
  fun `Given gradleJdk and projectJdk as 'Android Studio java home' When pre-sync project Then this was migrated to #JAVA_HOME macro`() =
    jdkIntegrationTest.run(
      project = JdkTestProject.SimpleApplication(
        ideaGradleJdk = ANDROID_STUDIO_JAVA_HOME_NAME,
        ideaProjectJdk = ANDROID_STUDIO_JAVA_HOME_NAME
      ),
      environment = JdkIntegrationTest.TestEnvironment(
        jdkTable = listOf(JdkTableUtils.Jdk(ANDROID_STUDIO_JAVA_HOME_NAME, JDK_17_PATH)),
        environmentVariables = mapOf(JAVA_HOME to JDK_17_PATH)
      ),
    ) {
      syncWithAssertion(
        expectedGradleJdkName = USE_JAVA_HOME,
        expectedProjectJdkName = JDK_17,
        expectedJdkPath = JDK_17_PATH
      )
    }

  @Test
  fun `Given multiple roots project using hardcore gradleJvm naming When pre-sync project Then those were migrated away from hardcoded naming`() =
    jdkIntegrationTest.run(
      project = JdkTestProject.SimpleApplicationMultipleRoots(
        roots = listOf(
          GradleRoot("project_root1", EMBEDDED_JDK_NAME),
          GradleRoot("project_root2", ANDROID_STUDIO_JAVA_HOME_NAME)
        )
      ),
      environment = JdkIntegrationTest.TestEnvironment(
        jdkTable = listOf(
          JdkTableUtils.Jdk(EMBEDDED_JDK_NAME, JDK_17_PATH),
          JdkTableUtils.Jdk(ANDROID_STUDIO_JAVA_HOME_NAME, JDK_11_PATH),
        ),
        environmentVariables = mapOf(JAVA_HOME to JDK_17_PATH)
      )
    ) {
      syncWithAssertion(
        expectedGradleRootsJdkName = mapOf(
          "project_root1" to JDK_17,
          "project_root2" to USE_JAVA_HOME
        ),
        expectedProjectJdkName = JDK_17,
        expectedJdkPath = JDK_17_PATH
      )
    }
}

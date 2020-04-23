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
package com.android.tools.idea.gradle.project.sync.quickFixes

import com.android.tools.idea.Projects
import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker
import com.android.tools.idea.gradle.util.GradleProjectSettingsFinder
import com.android.tools.idea.gradle.util.GradleWrapper
import com.google.wireless.android.sdk.stats.GradleSyncStats
import com.intellij.build.issue.BuildIssueQuickFix
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jetbrains.plugins.gradle.settings.DistributionType
import java.io.IOException
import java.util.concurrent.CompletableFuture

class CreateGradleWrapperQuickFix : BuildIssueQuickFix {
  override val id = "migrate.gradle.wrapper"

  override fun runQuickFix(project: Project, dataProvider: DataProvider): CompletableFuture<*> {
    val future = CompletableFuture<Any>()
    invokeLater {
      val projectDirPath = Projects.getBaseDirPath(project)
      try {
        GradleWrapper.create(projectDirPath, project)
        val settings = GradleProjectSettingsFinder.getInstance().findGradleProjectSettings(project)
        if (settings != null) {
          settings.distributionType = DistributionType.DEFAULT_WRAPPED
        }

        GradleSyncInvoker.getInstance().requestProjectSync(project, GradleSyncStats.Trigger.TRIGGER_QF_WRAPPER_CREATED)
        future.complete(null)
      }
      catch (e: IOException) {
        Messages.showErrorDialog(project, "Failed to create Gradle wrapper: " + e.message, "Quick Fix")
        future.completeExceptionally(e)
      }
    }
    return future
  }
}
/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.stats

import com.android.tools.analytics.UsageTracker
import com.android.tools.idea.projectsystem.PROJECT_SYSTEM_SYNC_TOPIC
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.google.wireless.android.sdk.stats.IntellijProjectSizeStats
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope

open class ProjectSizeUsageTracker(project: Project) : AbstractProjectComponent(project) {

  private enum class FileType(private val fileType: com.intellij.openapi.fileTypes.FileType,
                              private val statsFileType: IntellijProjectSizeStats.FileType) {
    JAVA(JavaFileType.INSTANCE, IntellijProjectSizeStats.FileType.JAVA),
    XML(XmlFileType.INSTANCE, IntellijProjectSizeStats.FileType.XML),
    JAVA_CLASS(JavaClassFileType.INSTANCE, IntellijProjectSizeStats.FileType.DOT_CLASS),
    KOTLIN(FileTypeRegistry.getInstance().findFileTypeByName("Kotlin") ?: PlainTextFileType.INSTANCE,
           IntellijProjectSizeStats.FileType.KOTLIN);

    fun languageFileType(): com.intellij.openapi.fileTypes.FileType {
      return fileType
    }

    fun statsFileType(): IntellijProjectSizeStats.FileType {
      return statsFileType
    }
  }

  private enum class SearchScope(private val globalSearchScopeFunc: (project: Project) -> GlobalSearchScope,
                                 private val statsSearchScope: IntellijProjectSizeStats.Scope) {
    // Count files within whole project
    All(ProjectScope::getAllScope, IntellijProjectSizeStats.Scope.ALL),
    // Count all library files i.e. only jar files
    LIBRARY(ProjectScope::getLibrariesScope, IntellijProjectSizeStats.Scope.LIBRARY);

    fun globalSearchScope(project: Project): GlobalSearchScope {
      return globalSearchScopeFunc(project)
    }

    fun statsSearchScope(): IntellijProjectSizeStats.Scope {
      return statsSearchScope
    }
  }

  override fun projectOpened() {
    val connection = myProject.messageBus.connect(myProject)
    connection.subscribe<ProjectSystemSyncManager.SyncResultListener>(
      PROJECT_SYSTEM_SYNC_TOPIC,
      object : ProjectSystemSyncManager.SyncResultListener {
        override fun syncEnded(result: ProjectSystemSyncManager.SyncResult) {
          if (!result.isSuccessful && result != ProjectSystemSyncManager.SyncResult.PARTIAL_SUCCESS) {
            return
          }
          val builder = AndroidStudioEvent
            .newBuilder()
            .setKind(AndroidStudioEvent.EventKind.INTELLIJ_PROJECT_SIZE_STATS)
            .withProjectId(myProject)
          for (searchScope in SearchScope.values()) {
            for (fileType in FileType.values()) {
              val intellijProjectSizeStats = IntellijProjectSizeStats
                .newBuilder()
                .setScope(searchScope.statsSearchScope())
                .setType(fileType.statsFileType())
              if (fileType.languageFileType() is PlainTextFileType) {
                // If kotlin plugin is not enabled, we will get PlainTextFileType. In such case, we do not want to collect kotlin
                // file count since it will include so many unrelated plain text file
                intellijProjectSizeStats.count = 0
              } else {
                intellijProjectSizeStats.count = ApplicationManager.getApplication().runReadAction(
                  Computable { FileTypeIndex.getFiles(fileType.languageFileType(), searchScope.globalSearchScope(myProject)).size })
              }
              builder.addIntellijProjectSizeStats(intellijProjectSizeStats)
            }
          }
          UsageTracker.log(builder)
          connection.disconnect()

        }
      })
  }
}

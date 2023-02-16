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
package com.android.tools.idea.insights

import com.android.tools.idea.insights.analysis.StackTraceAnalyzer
import com.android.tools.idea.insights.analytics.IssueSelectionSource
import com.intellij.psi.PsiFile
import kotlinx.coroutines.flow.Flow

/** Provides lifecycle and App Insights state data. */
interface AppInsightsProjectLevelController<IssueT : Issue, out StateT : AppInsightsState<IssueT>> {

  /**
   * This flow represents the App Insights state of a host Android app module.
   *
   * The state includes:
   * * Active and available [FirebaseConnection]s of a project.
   * * Active and available issues of the app(crashes).
   * * Active and available filters used to fetch the above issues.
   *
   * It contains many pieces of data all of which can change independently resulting in a new value
   * produced, as a result it is more convenient to [map] this flow into multiple sub flows that
   * "focus" on a subset of the data you care about. e.g.
   *
   * ```kotlin
   * val connections: Flow<Selection<FirebaseConnection>> = ctrl.state.map { it.connections }.distinctUntilChanged()
   * val issues: Flow<Selection<Issue>> = ctrl.state.filters.map { it.issues }.distinctUntilChanged() }
   * val selectedIssue: Flow<Issue?> = issues.mapReady { it.selected }.readyOrNull()
   * ```
   */
  val state: Flow<StateT>

  // events
  fun refresh()
  fun selectIssue(value: IssueT?, selectionSource: IssueSelectionSource)
  fun selectVersions(values: Set<Version>)

  fun selectDevices(values: Set<Device>)
  fun selectOperatingSystems(values: Set<OperatingSystemInfo>)
  fun selectTimeInterval(value: TimeIntervalFilter)
  fun toggleFailureType(value: FailureType)

  fun enterOfflineMode()
  fun retrieveLineMatches(file: PsiFile): List<AppInsight<IssueT>>
  fun insightsInFile(
    file: PsiFile,
    analyzer: StackTraceAnalyzer,
  )
}

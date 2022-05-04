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
package com.android.tools.idea.uibuilder.surface

import com.android.tools.idea.common.error.Issue
import com.android.tools.idea.common.error.IssuePanel
import com.android.tools.idea.common.model.NlModel
import com.android.tools.idea.common.surface.LayoutScannerControl
import com.android.tools.idea.flags.StudioFlags.NELE_LAYOUT_SCANNER_ADD_INCLUDE
import com.android.tools.idea.flags.StudioFlags.NELE_LAYOUT_SCANNER_COMMON_ERROR_PANEL
import com.android.tools.idea.rendering.RenderResult
import com.android.tools.idea.uibuilder.lint.CommonLintUserDataHandler
import com.android.tools.idea.validator.LayoutValidator
import com.android.tools.idea.validator.ValidatorData
import com.android.tools.idea.validator.ValidatorHierarchy
import com.android.tools.idea.validator.ValidatorResult
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer

/**
 * Validator for [NlDesignSurface].
 * It retrieves validation results from the [RenderResult] and update the lint accordingly.
 */
class NlLayoutScanner(private val surface: NlDesignSurface, parent: Disposable): Disposable, LayoutScannerControl {

  constructor(surface: NlDesignSurface) : this(surface, surface)

  interface Listener {
    fun lintUpdated(result: ValidatorResult?)
  }

  /** Parses the layout and store all metadata required for linking issues to source [NlComponent] */
  private val layoutParser = NlScannerLayoutParser()
  /** Helper class for displaying output to lint system */
  private val lintIntegrator = AccessibilityLintIntegrator(surface.issueModel)

  /** Returns list of issues generated by linter that are specific to the layout. */
  override val issues get() = lintIntegrator.issues

  /** Render specific metrics data */
  var renderMetric = RenderResultMetricData()

  @VisibleForTesting
  val listeners = HashSet<Listener>()

  /** Tracks metric related to atf */
  @VisibleForTesting
  private val metricTracker = NlLayoutScannerMetricTracker(surface)

  /** Listener for issue panel open/close */
  @VisibleForTesting
  val issuePanelListener = object : IssuePanel.EventListener {
    override fun onPanelExpanded(isExpanded: Boolean) {
      if (isExpanded) {
        metricTracker.trackIssues(issues, renderMetric)
      }
    }

    override fun onIssueExpanded(issue: Issue?, isExpanded: Boolean) {
      if (isExpanded && issue != null) {
        metricTracker.trackFirstExpanded(issue)
      }
    }

  }

  private val atfIssueEventListener = object: NlAtfIssue.EventListener {
    override fun onApplyFixButtonClicked(issue: ValidatorData.Issue) {
      metricTracker.trackApplyFixButtonClicked(issue)
    }

    override fun onIgnoreButtonClicked(issue: ValidatorData.Issue) {
      metricTracker.trackIgnoreButtonClicked(issue)
    }
  }

  init {
    Disposer.register(parent, this)
    surface.issuePanel.addEventListener(issuePanelListener)

    // Enabling this will retrieve text character locations from TextView to improve the
    // accuracy of TextContrastCheck in ATF
    LayoutValidator.setObtainCharacterLocations(true)
  }

  override fun pause() {
    LayoutValidator.setPaused(true)
  }

  override fun resume() {
    LayoutValidator.setPaused(false)
  }

  /**
   * Validate the layout and update the lint accordingly.
   */
  override
  fun validateAndUpdateLint(renderResult: RenderResult, model: NlModel) {
    when (val validatorResult = renderResult.validatorResult) {
      is ValidatorHierarchy -> {
        if (!validatorResult.isHierarchyBuilt) {
          // Result not available
          listeners.forEach { it.lintUpdated(null) }
          return
        }
        updateLint(renderResult, LayoutValidator.validate(validatorResult), model, surface)
      }
      else -> {
        // Result not available.
        listeners.forEach { it.lintUpdated(null) }
      }
    }
  }

  @VisibleForTesting
  fun updateLint(
    renderResult: RenderResult,
    validatorResult: ValidatorResult,
    model: NlModel,
    surface: NlDesignSurface) {
    lintIntegrator.clear()
    layoutParser.clear()

    var result: ValidatorResult? = null
    try {
      val components = model.components
      if (components.isEmpty()) {
        // Result not available.
        return
      }

      var issuesWithoutSources = 0
      val root = components[0]
      layoutParser.buildViewToComponentMap(root)
      validatorResult.issues.forEach {
        if ((it.mLevel == ValidatorData.Level.ERROR || it.mLevel == ValidatorData.Level.WARNING) &&
            it.mType == ValidatorData.Type.ACCESSIBILITY) {
          val component = layoutParser.findComponent(it, validatorResult.srcMap)
          if (component == null) {
            issuesWithoutSources++
          } else {
            lintIntegrator.createIssue(it, component, model, atfIssueEventListener)
          }
        }
        // TODO: b/180069618 revisit metrics. Should log each issue.
      }

      if (NELE_LAYOUT_SCANNER_ADD_INCLUDE.get() && issuesWithoutSources > 0 && layoutParser.includeComponents.isNotEmpty()) {
        lintIntegrator.handleInclude(layoutParser, surface)
      }

      if (NELE_LAYOUT_SCANNER_COMMON_ERROR_PANEL.get()) {
        CommonLintUserDataHandler.updateAtfIssues(model.file, issues)
      }

      lintIntegrator.populateLints()
      result = validatorResult
    } finally {
      renderMetric.renderMs = renderResult.stats.renderDurationMs
      renderMetric.scanMs = validatorResult.metric.mHierarchyCreationMs
      renderMetric.componentCount = layoutParser.componentCount
      renderMetric.isRenderResultSuccess = renderResult.renderResult.isSuccess

      layoutParser.clear()
      // TODO: b/180069618 revisit metrics. Should log render result here.
      listeners.forEach { it.lintUpdated(result) }
    }
  }

  fun addListener(listener: Listener) {
    listeners.add(listener)
  }

  fun removeListener(listener: Listener) {
    listeners.remove(listener)
  }

  override fun dispose() {
    metricTracker.clear()
    layoutParser.clear()
    listeners.clear()
    lintIntegrator.clear()
  }

  /** Returns true if [NlScannerLayoutParser] has been cleaned. False otherwise. */
  @VisibleForTesting
  fun isParserCleaned(): Boolean {
    return layoutParser.isEmpty()
  }
}

// For debugging
fun ValidatorResult.toDetailedString(): String? {
  val builder: StringBuilder = StringBuilder().append("Result containing ").append(issues.size).append(
    " issues:\n")
  val var2: Iterator<*> = this.issues.iterator()
  while (var2.hasNext()) {
    val issue = var2.next() as ValidatorData.Issue
    if (issue.mLevel == ValidatorData.Level.ERROR) {
      builder.append(" - [E::").append(issue.mLevel.name).append("] ").append(issue.mMsg).append("\n")
    }
    else {
      builder.append(" - [W::").append(issue.mLevel.name).append("] ").append(issue.mMsg).append("\n")
    }
  }
  return builder.toString()
}
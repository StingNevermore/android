/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.tools.idea.compose.gradle.uicheck

import com.android.tools.idea.common.SyncNlModel
import com.android.tools.idea.compose.gradle.ComposeGradleProjectRule
import com.android.tools.idea.compose.preview.PreviewConfiguration
import com.android.tools.idea.compose.preview.SIMPLE_COMPOSE_PROJECT_PATH
import com.android.tools.idea.compose.preview.SingleComposePreviewElementInstance
import com.android.tools.idea.compose.preview.renderer.renderPreviewElementForResult
import com.android.tools.idea.uibuilder.model.NlComponentRegistrar
import com.android.tools.idea.uibuilder.scene.accessibilityBasedHierarchyParser
import com.android.tools.idea.uibuilder.visual.visuallint.analyzers.LongTextAnalyzer
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class LongTextAnalyzerComposeTest {
  @get:Rule val projectRule = ComposeGradleProjectRule(SIMPLE_COMPOSE_PROJECT_PATH)

  @Test
  fun testLongText() {
    val facet = projectRule.androidFacet(":app")
    val renderResult =
      renderPreviewElementForResult(
          facet,
          SingleComposePreviewElementInstance.forTesting(
            "google.simpleapplication.VisualLintPreviewKt.VisualLintErrorPreview",
            configuration =
              PreviewConfiguration.cleanAndGet(
                device = "spec:shape=Normal,width=1280,height=800,unit=dp,dpi=420" // Tablet
              )
          ),
          customViewInfoParser = accessibilityBasedHierarchyParser
        )
        .get()!!
    val file = renderResult.sourceFile.virtualFile
    val nlModel =
      SyncNlModel.create(
        projectRule.fixture.testRootDisposable,
        NlComponentRegistrar,
        null,
        facet,
        file
      )
    val issues = LongTextAnalyzer.findIssues(renderResult, nlModel)
    Assert.assertEquals(1, issues.size)
    Assert.assertEquals(
      "<android.widget.TextView> has lines containing more than 120 characters",
      issues[0].message
    )
  }

  @Test
  fun testShortText() {
    val facet = projectRule.androidFacet(":app")
    val renderResult =
      renderPreviewElementForResult(
          facet,
          SingleComposePreviewElementInstance.forTesting(
            "google.simpleapplication.VisualLintPreviewKt.NoVisualLintErrorPreview",
            configuration =
              PreviewConfiguration.cleanAndGet(
                device = "spec:shape=Normal,width=1280,height=800,unit=dp,dpi=420" // Tablet
              )
          ),
          customViewInfoParser = accessibilityBasedHierarchyParser
        )
        .get()!!
    val file = renderResult.sourceFile.virtualFile
    val nlModel =
      SyncNlModel.create(
        projectRule.fixture.testRootDisposable,
        NlComponentRegistrar,
        null,
        facet,
        file
      )
    val issues = LongTextAnalyzer.findIssues(renderResult, nlModel)
    Assert.assertEquals(0, issues.size)
  }
}

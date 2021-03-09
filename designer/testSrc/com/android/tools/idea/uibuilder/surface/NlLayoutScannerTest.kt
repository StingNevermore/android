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

import com.android.tools.idea.common.error.IssueModel
import com.android.tools.idea.rendering.RenderResult
import com.android.tools.idea.testing.AndroidProjectRule
import com.android.tools.idea.validator.ValidatorData
import com.android.tools.idea.validator.ValidatorResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@RunWith(JUnit4::class)
class NlLayoutScannerTest {
  @get:Rule
  val projectRule = AndroidProjectRule.withSdk()

  @Mock
  lateinit var mockSurface: NlDesignSurface

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  private fun createScanner(): NlLayoutScanner {
    val issueModel: IssueModel = Mockito.mock(IssueModel::class.java)
    val metricTracker = NlLayoutScannerMetricTrackerTest.createMetricTracker()
    return NlLayoutScanner(issueModel, projectRule.fixture.testRootDisposable!!, metricTracker)
  }

  @Test
  fun addListener() {
    val scanner = createScanner()
    val listener = object : NlLayoutScanner.Listener {
      override fun lintUpdated(result: ValidatorResult?) { }
    }

    scanner.addListener(listener)

    assertTrue(scanner.listeners.contains(listener))
  }

  @Test
  fun removeListener() {
    val scanner = createScanner()
    val listener = object : NlLayoutScanner.Listener {
      override fun lintUpdated(result: ValidatorResult?) { }
    }
    scanner.addListener(listener)
    assertTrue(scanner.listeners.contains(listener))

    scanner.removeListener(listener)
    assertFalse(scanner.listeners.contains(listener))
  }

  @Test
  fun removeListenerInCallback() {
    val scanner = createScanner()
    val model = ScannerTestHelper().buildModel(0)
    val renderResult = Mockito.mock(RenderResult::class.java)
    val listener = object : NlLayoutScanner.Listener {
      override fun lintUpdated(result: ValidatorResult?) {
        scanner.removeListener(this)
      }
    }
    scanner.addListener(listener)

    scanner.validateAndUpdateLint(renderResult, model, mockSurface)

    assertFalse(scanner.listeners.contains(listener))
  }

  @Test
  fun validateNoResult() {
    val scanner = createScanner()
    val helper = ScannerTestHelper()
    val componentSize = 0
    val model = helper.buildModel(componentSize)
    val renderResult = Mockito.mock(RenderResult::class.java)

    var listenerTriggered = false
    val listener = object : NlLayoutScanner.Listener {
      override fun lintUpdated(result: ValidatorResult?) {
        listenerTriggered = true
      }
    }
    scanner.addListener(listener)

    scanner.validateAndUpdateLint(renderResult, model, mockSurface)

    assertTrue(listenerTriggered)
    assertEquals(0, scanner.issues.size)
    assertTrue(scanner.isParserCleaned())
  }

  @Test
  fun validateEmpty() {
    val scanner = createScanner()
    val helper = ScannerTestHelper()
    val componentSize = 0
    val model = helper.buildModel(componentSize)
    val renderResult = helper.mockRenderResult(model)

    var listenerTriggered = false
    val listener = object : NlLayoutScanner.Listener {
      override fun lintUpdated(result: ValidatorResult?) {
        listenerTriggered = true
      }
    }
    scanner.addListener(listener)

    scanner.validateAndUpdateLint(renderResult, model, mockSurface)

    assertTrue(listenerTriggered)
    assertEquals(0, scanner.issues.size)
    assertTrue(scanner.isParserCleaned())
  }

  @Test
  fun validateMultipleIssues() {
    val scanner = createScanner()
    val helper = ScannerTestHelper()
    val componentSize = 5
    val model = helper.buildModel(componentSize)
    val renderResult = helper.mockRenderResult(model)

    var validatorResult: ValidatorResult? = null
    val listener = object : NlLayoutScanner.Listener {
      override fun lintUpdated(result: ValidatorResult?) {
        validatorResult = result
      }
    }
    scanner.addListener(listener)

    scanner.validateAndUpdateLint(renderResult, model, mockSurface)

    assertNotNull(validatorResult)
    assertEquals(componentSize, validatorResult!!.issues.size)
    assertEquals(componentSize, scanner.issues.size)
    assertTrue(scanner.isParserCleaned())
  }

  @Test
  fun validateFiltersInternalIssues() {
    val scanner = createScanner()
    val helper = ScannerTestHelper()
    val model = helper.buildModel(1)
    val resultToInject = ValidatorResult.Builder()

    // Add 3 types of issues that will be filtered: Internal, verbose or info.
    resultToInject.mIssues.add(
      ScannerTestHelper.createTestIssueBuilder()
        .setLevel(ValidatorData.Level.ERROR)
        .setType(ValidatorData.Type.INTERNAL_ERROR)
        .build())
    resultToInject.mIssues.add(
      ScannerTestHelper.createTestIssueBuilder()
        .setLevel(ValidatorData.Level.VERBOSE)
        .setType(ValidatorData.Type.ACCESSIBILITY)
        .build())
    resultToInject.mIssues.add(
      ScannerTestHelper.createTestIssueBuilder()
        .setLevel(ValidatorData.Level.INFO)
        .setType(ValidatorData.Type.ACCESSIBILITY)
        .build())

    // Run the scanner core code.
    val renderResult = helper.mockRenderResult(model, resultToInject.build())
    var validatorResult: ValidatorResult? = null
    val listener = object : NlLayoutScanner.Listener {
      override fun lintUpdated(result: ValidatorResult?) {
        validatorResult = result
      }
    }
    scanner.addListener(listener)
    scanner.validateAndUpdateLint(renderResult, model, mockSurface)

    // Expect the results to be filtered.
    assertNotNull(validatorResult)
    assertEquals( 3, validatorResult!!.issues.size)
    assertTrue("Issue from Validator Result must be filtered.", scanner.issues.isEmpty())
    assertTrue("Maps must be cleaned after the scan.", scanner.isParserCleaned())
  }
}

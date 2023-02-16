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
package com.android.tools.idea.layoutinspector.runningdevices

import com.android.testutils.MockitoKt.any
import com.android.testutils.MockitoKt.mock
import com.android.testutils.MockitoKt.whenever
import com.android.tools.idea.streaming.RUNNING_DEVICES_TOOL_WINDOW_ID
import com.android.tools.idea.streaming.SERIAL_NUMBER_KEY
import com.google.common.truth.Truth.assertThat
import com.intellij.ide.ui.customization.CustomActionsSchema
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.DisposableRule
import com.intellij.testFramework.ProjectRule
import com.intellij.testFramework.RuleChain
import com.intellij.testFramework.replaceService
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ToggleLayoutInspectorActionTest {

  private val projectRule = ProjectRule()

  private val disposableRule = DisposableRule()

  @get:Rule
  val ruleChain = RuleChain(ApplicationRule(), disposableRule, projectRule)

  private lateinit var fakeLayoutInspectorManager: FakeLayoutInspectorManager

  @Before
  fun setUp() {
    // replace ToolWindowManager with fake one
    projectRule.project.replaceService(
      ToolWindowManager::class.java,
      FakeToolWindowManager(projectRule.project),
      disposableRule.disposable
    )

    // replace CustomActionsSchema with mocked one
    val mockCustomActionSchema = mock<CustomActionsSchema>()
    whenever(mockCustomActionSchema.getCorrectedAction(any())).thenAnswer { getFakeAction() }
    ApplicationManager.getApplication().replaceService(CustomActionsSchema::class.java, mockCustomActionSchema, disposableRule.disposable)

    // replace LayoutInspectorManager with fake one
    fakeLayoutInspectorManager = FakeLayoutInspectorManager()
    projectRule.project.replaceService(LayoutInspectorManager::class.java, fakeLayoutInspectorManager, disposableRule.disposable)
  }

  @Test
  fun testActionPerformedTogglesLayoutInspector() {
    val toggleLayoutInspectorAction = ToggleLayoutInspectorAction()
    val fakeActionEvent = toggleLayoutInspectorAction.getFakeActionEvent()

    toggleLayoutInspectorAction.actionPerformed(fakeActionEvent)

    assertThat(fakeLayoutInspectorManager.toggleLayoutInspectorInvocations).isEqualTo(1)
    assertThat(fakeLayoutInspectorManager.isEnabled).isTrue()

    toggleLayoutInspectorAction.actionPerformed(fakeActionEvent)

    assertThat(fakeLayoutInspectorManager.toggleLayoutInspectorInvocations).isEqualTo(2)
    assertThat(fakeLayoutInspectorManager.isEnabled).isFalse()
  }

  private fun AnAction.getFakeActionEvent(): AnActionEvent {
    val dataContext = DataContext {
      when (it) {
        CommonDataKeys.PROJECT.name -> projectRule.project
        SERIAL_NUMBER_KEY.name -> "serial_number"
        else -> null
      }
    }

    return AnActionEvent.createFromAnAction(this, null, "", dataContext)
  }

  private fun getFakeAction(): AnAction {
    return object : AnAction() {
      override fun actionPerformed(e: AnActionEvent) { }
    }
  }

  private class FakeLayoutInspectorManager : LayoutInspectorManager {
    var toggleLayoutInspectorInvocations = 0

    override var isEnabled = false

    override fun toggleLayoutInspector(enable: Boolean) {
      toggleLayoutInspectorInvocations += 1
      isEnabled = enable
    }
  }

  private class FakeToolWindowManager(project: Project) : ToolWindowHeadlessManagerImpl(project) {
    private var toolWindow = FakeToolWindow(project)

    override fun getToolWindow(id: String?): ToolWindow? {
      return if (id == RUNNING_DEVICES_TOOL_WINDOW_ID) toolWindow else super.getToolWindow(id)
    }
  }

  private class FakeToolWindow(project: Project) : ToolWindowHeadlessManagerImpl.MockToolWindow(project) {
    private val mockContentManager = mock<ContentManager>()

    init {
      val mockContent = mock<Content>()
      whenever(mockContentManager.selectedContent).thenAnswer { mockContent }
    }

    override fun getContentManager(): ContentManager {
      return mockContentManager
    }
  }
}
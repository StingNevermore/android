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
package com.android.tools.property.panel.impl.ui

import com.android.SdkConstants.ANDROID_URI
import com.android.SdkConstants.ATTR_INPUT_TYPE
import com.android.testutils.MockitoKt.mock
import com.android.testutils.MockitoKt.whenever
import com.android.tools.property.panel.api.ControlType
import com.android.tools.property.panel.api.EditorProvider
import com.android.tools.property.panel.api.EnumSupport
import com.android.tools.property.panel.api.EnumSupportProvider
import com.android.tools.property.panel.api.FlagsPropertyItem
import com.android.tools.property.panel.api.NewPropertyItem
import com.android.tools.property.panel.api.PropertyItem
import com.android.tools.property.panel.api.TableUIProvider
import com.android.tools.property.panel.impl.model.TableLineModelImpl
import com.android.tools.property.panel.impl.model.util.FakeFlagsPropertyItem
import com.android.tools.property.panel.impl.support.SimpleControlTypeProvider
import com.android.tools.property.panel.impl.table.EditorPanel
import com.android.tools.property.panel.impl.table.PTableCellEditorProviderImpl
import com.android.tools.property.ptable.PTableColumn
import com.android.tools.property.ptable.PTableItem
import com.android.tools.property.ptable.PTableModel
import com.android.tools.property.ptable.impl.PTableImpl
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.DisposableRule
import com.intellij.testFramework.replaceService
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.JTextField

class FlagPropertyEditorTest {

  companion object {
    @JvmField @ClassRule val rule = ApplicationRule()
  }

  @get:Rule val disposableRule = DisposableRule()

  @Before
  fun setUp() {
    ApplicationManager.getApplication()
      .replaceService(ActionManager::class.java, mock(), disposableRule.disposable)
    whenever(ActionManager.getInstance().getAction(IdeActions.ACTION_CLEAR_TEXT))
      .thenReturn(SomeAction("ClearText"))
  }

  @Test
  fun testRestoreComponentIsTableWhenEditingFlagPropertyInTable() {
    val table = createTableWithFlagEditors()
    val flagEditor = getEditorFromTable(table, 1)
    assertThat(flagEditor.tableParent).isEqualTo(table.component)
  }

  @Test
  fun testShortCheckBoxListIsNotScrollable() {
    val table = createTableWithFlagEditors()
    val flagEditor = getEditorFromTable(table, 1)
    val panel = FlagPropertyPanel(flagEditor.editorModel, flagEditor.tableParent!!, 400)
    val scrollPane = panel.getComponent(2) as JScrollPane
    assertThat(scrollPane.isPreferredSizeSet).isFalse()
  }

  @Test
  fun testLongCheckBoxListIsScrollable() {
    val table = createTableWithFlagEditors()
    val flagEditor = getEditorFromTable(table, 2)
    val panel = FlagPropertyPanel(flagEditor.editorModel, flagEditor.tableParent!!, 400)
    val scrollPane = panel.getComponent(2) as JScrollPane
    assertThat(scrollPane.isPreferredSizeSet).isTrue()
    assertThat(scrollPane.preferredSize.height).isLessThan(400)
  }

  @Test
  fun testPanelWillRestoreEditingInTable() {
    val table = createTableWithFlagEditors()
    val flagEditor = getEditorFromTable(table, 1)

    // The flag editor should have a JTextField and not a PropertyLabel:
    assertThat(flagEditor.components.single { it is JTextField }).isNotNull()
    assertThat(flagEditor.components.singleOrNull { it is PropertyLabel }).isNull()

    val panel = FlagPropertyPanel(flagEditor.editorModel, flagEditor.tableParent!!, 400)
    val swingTable = table.component as PTableImpl
    swingTable.removeEditor()
    assertThat(swingTable.editingRow).isEqualTo(-1)
    panel.hideBalloonAndRestoreFocusOnEditor()
    assertThat(swingTable.editingRow).isEqualTo(1)
  }

  @Test
  fun testRendererUsesLabel() {
    val tableEditor = createTableWithFlagEditors()
    val table = tableEditor.component
    val renderer = table.getCellRenderer(0, 1)
    val component =
      renderer.getTableCellRendererComponent(table, table.getValueAt(0, 1), false, false, 0, 1)
        as? JComponent
    val flagEditor = component?.components?.single() as? FlagPropertyEditor ?: error("unexpected")
    // The flag renderer should have a PropertyLabel and not a JTextField:
    assertThat(flagEditor.components.single { it is PropertyLabel }).isNotNull()
    assertThat(flagEditor.components.singleOrNull { it is JTextField }).isNull()
  }

  private fun createTableWithFlagEditors(): TableEditor {
    val flag1 =
      FakeFlagsPropertyItem(
        ANDROID_URI,
        ATTR_INPUT_TYPE,
        listOf("text", "date", "datetime"),
        listOf(1, 6, 2)
      )
    val flag2 =
      FakeFlagsPropertyItem(
        ANDROID_URI,
        "autoLink",
        listOf("none", "web", "email", "phone", "all"),
        listOf(0, 1, 2, 4, 7)
      )
    val flag3 =
      FakeFlagsPropertyItem(
        ANDROID_URI,
        "long",
        listOf(
          "one",
          "two",
          "three",
          "four",
          "five",
          "six",
          "seven",
          "eight",
          "nine",
          "ten",
          "eleven",
          "twelve",
          "thirteen",
          "fourteen"
        ),
        listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)
      )
    val tableModel = PTableTestModel(flag1, flag2, flag3)
    val lineModel = TableLineModelImpl(tableModel, true)
    val enumSupportProvider =
      object : EnumSupportProvider<PropertyItem> {
        override fun invoke(property: PropertyItem): EnumSupport? {
          return null
        }
      }
    val controlTypeProvider = SimpleControlTypeProvider<PropertyItem>(ControlType.FLAG_EDITOR)
    val nameControlTypeProvider =
      SimpleControlTypeProvider<NewPropertyItem>(ControlType.TEXT_EDITOR)
    val editorProvider = EditorProvider.create(enumSupportProvider, controlTypeProvider)
    val uiProvider = TableUIProvider(controlTypeProvider, editorProvider)

    val cellEditorProvider =
      PTableCellEditorProviderImpl(
        NewPropertyItem::class.java,
        nameControlTypeProvider,
        EditorProvider.createForNames(),
        FlagsPropertyItem::class.java,
        controlTypeProvider,
        editorProvider
      )
    return TableEditor(
      lineModel,
      uiProvider.tableCellRendererProvider,
      uiProvider.tableCellEditorProvider
    )
  }

  private fun getEditorFromTable(table: TableEditor, row: Int): FlagPropertyEditor {
    val swingTable = table.component as PTableImpl
    while (swingTable.editingRow < row) {
      assertThat(swingTable.startNextEditor()).isTrue()
    }
    val editorPanel = swingTable.editorComponent as EditorPanel
    return editorPanel.editor as FlagPropertyEditor
  }
}

private class PTableTestModel(vararg items: PTableItem) : PTableModel {
  override var editedItem: PTableItem? = null
  override val items = mutableListOf(*items)

  override fun isCellEditable(item: PTableItem, column: PTableColumn): Boolean {
    return column == PTableColumn.VALUE
  }

  override fun addItem(item: PTableItem): PTableItem {
    items.add(item)
    return item
  }

  override fun removeItem(item: PTableItem) {
    items.remove(item)
  }
}

private class SomeAction internal constructor(title: String) : AnAction(title) {
  override fun actionPerformed(e: AnActionEvent) {}
}

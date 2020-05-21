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
package com.android.tools.idea.uibuilder.property2.ui
import com.android.SdkConstants
import com.android.tools.adtui.common.secondaryPanelBackground
import com.android.tools.idea.common.command.NlWriteCommandActionUtil
import com.android.tools.idea.uibuilder.property2.NelePropertyItem
import com.android.tools.property.panel.api.PropertiesModel
import com.android.tools.property.panel.api.PropertiesModelListener
import com.android.tools.property.panel.api.PropertiesTable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.idea.debugger.readAction
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Custom panel to support direct editing of transition easing curves
 */
class ViewTransitionPanel(properties: PropertiesTable<NelePropertyItem>) : JPanel(BorderLayout()) {

  private val PANEL_WIDTH = 200
  private val PANEL_HEIGHT = PANEL_WIDTH

  private val transitionEasing = properties.getOrNull(SdkConstants.AUTO_URI, "transitionEasing")

  private val easingCurvePanel = EasingCurve()
  private var processingChange: Boolean = false
  private var processingModelUpdate: Boolean = false

  init {
    val panelSize = JBUI.size(PANEL_WIDTH, PANEL_HEIGHT)
    preferredSize = panelSize

    easingCurvePanel.background = secondaryPanelBackground
    add(easingCurvePanel)

    var listener = object: PropertiesModelListener<NelePropertyItem> {
      override fun propertiesGenerated(model: PropertiesModel<NelePropertyItem>) {
        updateFromValues()
      }
      override fun propertyValuesChanged(model: PropertiesModel<NelePropertyItem>) {
        updateFromValues()
      }
    }

    transitionEasing?.model?.addListener(listener)

    easingCurvePanel.addActionListener {
      if (!processingModelUpdate) {
        processingChange = true
        if (transitionEasing != null) {
          var component = transitionEasing.componentName
          TransactionGuard.submitTransaction(transitionEasing.model, Runnable {
            NlWriteCommandActionUtil.run(transitionEasing.components,
                                         "Set $component.${transitionEasing.name} to ${it.actionCommand}") {
              var cubic = it.actionCommand
              when (cubic) {
                "cubic(0.2,0.2,0.8,0.8)" -> cubic = "linear"
                "cubic(0.4,0,0.2,1)" -> cubic = "standard"
                "cubic(0.4,0,1,1)" -> cubic = "accelerate"
                "cubic(0,0,0.2,1)" -> cubic = "decelerate"
              }
              transitionEasing.value = cubic
            }
          })
        }
        processingChange = false
      }
    }

    updateFromValues()
  }

  private fun updateFromValues() {
    if (processingChange) {
      return
    }
    val application = ApplicationManager.getApplication()
    if (application.isReadAccessAllowed) {
      updateFromProperty()
    } else {
      application.readAction {
        updateFromProperty()
      }
    }
  }

  private fun updateFromProperty() {
    transitionEasing?.value?.let {
      var cubic : String? = it
      when (cubic) {
        "standard" -> cubic = "cubic(0.4,0,0.2,1)"
        "accelerate" -> cubic = "cubic(0.4,0,1,1)"
        "decelerate" -> cubic = "cubic(0,0,0.2,1)"
        "linear" -> cubic = "cubic(0.2,0.2,0.8,0.8)"
      }
      processingModelUpdate = true
      easingCurvePanel.controlPoints = cubic
      processingModelUpdate = false
    }
  }

}
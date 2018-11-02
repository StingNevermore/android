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
package com.android.tools.idea.uibuilder.handlers.motion.property2;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.AUTO_URI;
import static com.android.tools.idea.uibuilder.handlers.motion.MotionSceneString.ConstraintSetConstraint;
import static com.android.tools.idea.uibuilder.handlers.motion.MotionSceneString.Key_framePosition;
import static com.android.tools.idea.uibuilder.handlers.motion.MotionSceneString.Key_frameTarget;
import static com.android.tools.idea.uibuilder.handlers.motion.MotionSceneString.MotionSceneConstraintSet;
import static com.android.tools.idea.uibuilder.handlers.motion.MotionSceneString.MotionSceneTransition;

import com.android.tools.adtui.ptable2.PTableColumn;
import com.android.tools.adtui.ptable2.PTableItem;
import com.android.tools.adtui.ptable2.PTableModel;
import com.android.tools.adtui.ptable2.PTableModelUpdateListener;
import com.android.tools.idea.common.model.NlComponent;
import com.android.tools.idea.common.property2.api.EditorProvider;
import com.android.tools.idea.common.property2.api.InspectorBuilder;
import com.android.tools.idea.common.property2.api.InspectorLineModel;
import com.android.tools.idea.common.property2.api.InspectorPanel;
import com.android.tools.idea.common.property2.api.PropertiesTable;
import com.android.tools.idea.common.property2.api.PropertiesView;
import com.android.tools.idea.common.property2.api.PropertiesViewTab;
import com.android.tools.idea.common.property2.api.TableUIProvider;
import com.android.tools.idea.uibuilder.handlers.motion.property2.model.TargetModel;
import com.android.tools.idea.uibuilder.handlers.motion.property2.ui.TargetComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.android.formatter.AttributeComparator;
import org.jetbrains.annotations.NotNull;

/**
 * {@link PropertiesView} for motion layout property editor.
 */
public class MotionLayoutAttributesView extends PropertiesView<MotionPropertyItem> {
  private static final String MOTION_VIEW_NAME = "Motion";

  public MotionLayoutAttributesView(@NotNull MotionLayoutAttributesModel model) {
    super(MOTION_VIEW_NAME, model);
    PropertiesViewTab<MotionPropertyItem> tab = addTab("");
    MotionControlTypeProvider controlTypeProvider = new MotionControlTypeProvider();
    EditorProvider<MotionPropertyItem> editorProvider =
      EditorProvider.Companion.create(new MotionEnumSupportProvider(), controlTypeProvider);
    TableUIProvider tableUIProvider = TableUIProvider.Companion.create(MotionPropertyItem.class, controlTypeProvider, editorProvider);
    tab.getBuilders().add(new MotionInspectorBuilder(editorProvider, tableUIProvider));
  }

  private static class MotionInspectorBuilder implements InspectorBuilder<MotionPropertyItem> {
    private final EditorProvider<MotionPropertyItem> myEditorProvider;
    private final AttributeComparator<MotionPropertyItem> myAttributeComparator;
    private final TableUIProvider myTableUIProvider;

    private MotionInspectorBuilder(@NotNull EditorProvider<MotionPropertyItem> editorProvider, @NotNull TableUIProvider tableUIProvider) {
      myEditorProvider = editorProvider;
      myAttributeComparator = new AttributeComparator<>(MotionPropertyItem::getName);
      myTableUIProvider = tableUIProvider;
    }

    @Override
    public void attachToInspector(@NotNull InspectorPanel inspector,
                                  @NotNull PropertiesTable<? extends MotionPropertyItem> properties) {
      MotionPropertyItem any = properties.getFirst();
      if (any == null) {
        return;
      }
      XmlTag tag = any.getTag().getElement();
      if (tag == null) {
        return;
      }
      NlComponent component = any.getComponent();
      String label = tag.getLocalName();
      switch (label) {
        case ConstraintSetConstraint:
          MotionPropertyItem targetId = properties.getOrNull(ANDROID_URI, ATTR_ID);
          label = MotionSceneConstraintSet;
          addTargetComponent(inspector, component, label);
          addPropertyTable(inspector, label, properties, targetId);
          break;

        case MotionSceneTransition:
          addTargetComponent(inspector, component, label);
          addPropertyTable(inspector, label, properties);
          break;

        default:
          // This should be some kind of KeyFrame
          MotionPropertyItem target = properties.getOrNull(AUTO_URI, Key_frameTarget);
          MotionPropertyItem position = properties.getOrNull(AUTO_URI, Key_framePosition);
          if (target == null || position == null) {
            // All KeyFrames should have target and position.
            Logger.getInstance(MotionPropertyItem.class).warn("KeyFrame without target and position");
            return;
          }
          addTargetComponent(inspector, component, label);
          inspector.addEditor(myEditorProvider.createEditor(position, false), null);
          addPropertyTable(inspector, label, properties, target, position);
          break;
      }
    }

    private static void addTargetComponent(@NotNull InspectorPanel inspector, @NotNull NlComponent component, @NotNull String label) {
      TargetComponent targetComponent = new TargetComponent(new TargetModel(component, label));
      inspector.addComponent(targetComponent, null);
    }

    private void addPropertyTable(@NotNull InspectorPanel inspector,
                                  @NotNull String titleName,
                                  @NotNull PropertiesTable<? extends MotionPropertyItem> properties,
                                  @NotNull MotionPropertyItem... excluded) {
      List<MotionPropertyItem> attributes = properties.getValues().stream()
        .filter(item -> ArrayUtil.find(excluded, item) < 0)
        .sorted(myAttributeComparator)
        .collect(Collectors.toList());

      InspectorLineModel title = inspector.addExpandableTitle(titleName, true);
      inspector.addTable(new MotionTableModel(attributes), true, myTableUIProvider, title);
    }

    @Override
    public void resetCache() {
    }
  }

  /**
   * Model for a general properties table.
   *
   * Used to display a list of properties.
   * Certain key properties may be filtered out and shown separately.
   */
  private static class MotionTableModel implements PTableModel {
    private final List<PTableItem> myItems;

    private MotionTableModel(@NotNull List<MotionPropertyItem> items) {
      items.sort(Comparator.comparing(MotionPropertyItem::getName).thenComparing(MotionPropertyItem::getNamespace));
      myItems = new ArrayList<>(items);
    }

    @NotNull
    @Override
    public List<PTableItem> getItems() {
      return myItems;
    }

    @Override
    public boolean isCellEditable(@NotNull PTableItem item, @NotNull PTableColumn column) {
      return true;
    }

    @Override
    public boolean acceptMoveToNextEditor(@NotNull PTableItem item, @NotNull PTableColumn column) {
      return true;
    }

    @Override
    public void addListener(@NotNull PTableModelUpdateListener listener) {
      // items are not updated in this model
    }
  }
}

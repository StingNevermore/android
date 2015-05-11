/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.tools.idea.tests.gui.framework.fixture;

import com.google.common.collect.Lists;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableGroup;
import com.intellij.openapi.options.ex.ProjectConfigurablesGroup;
import com.intellij.openapi.options.newEditor.OptionsEditor;
import com.intellij.openapi.options.newEditor.OptionsEditorDialog;
import com.intellij.openapi.options.newEditor.OptionsTree;
import com.intellij.openapi.util.SystemInfo;
import org.fest.reflect.reference.TypeRef;
import org.fest.swing.core.GenericTypeMatcher;
import org.fest.swing.core.Robot;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

import static org.fest.reflect.core.Reflection.field;
import static org.junit.Assert.assertNotNull;

public class IdeSettingsDialogFixture extends IdeaDialogFixture<OptionsEditorDialog> {
  @NotNull
  public static IdeSettingsDialogFixture find(@NotNull Robot robot) {
    return new IdeSettingsDialogFixture(robot, find(robot, OptionsEditorDialog.class, new GenericTypeMatcher<JDialog>(JDialog.class) {
      @Override
      protected boolean isMatching(@NotNull JDialog dialog) {
        String expectedTitle = SystemInfo.isMac ? "Preferences" : "Settings";
        return expectedTitle.equals(dialog.getTitle()) && dialog.isShowing();
      }
    }));
  }

  private IdeSettingsDialogFixture(@NotNull Robot robot, @NotNull DialogAndWrapper<OptionsEditorDialog> dialogAndWrapper) {
    super(robot, dialogAndWrapper);
  }

  @NotNull
  public List<String> getProjectSettingsNames() {
    return getSettingsNames(ProjectConfigurablesGroup.class);
  }

  private List<String> getSettingsNames(@NotNull Class<? extends ConfigurableGroup> groupType) {
    List<String> names = Lists.newArrayList();
    OptionsEditor optionsEditor = field("myEditor").ofType(OptionsEditor.class)
                                                   .in(getDialogWrapper())
                                                   .get();
    assertNotNull(optionsEditor);
    OptionsTree optionsTree = field("myTree").ofType(OptionsTree.class)
                                             .in(optionsEditor)
                                             .get();
    assertNotNull(optionsTree);
    List<ConfigurableGroup> groups = field("myGroups").ofType(new TypeRef<List<ConfigurableGroup>>() {})
                                                      .in(optionsTree)
                                                      .get();
    assertNotNull(groups);
    ConfigurableGroup group = null;
    for (ConfigurableGroup current : groups) {
      if (groupType.isInstance(current)) {
        group = current;
        break;
      }
    }
    assertNotNull(group);
    for (Configurable configurable : group.getConfigurables()) {
      names.add(configurable.getDisplayName());
    }
    return names;
  }
}

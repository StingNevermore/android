/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.editors.theme;

import com.android.SdkConstants;
import com.android.ide.common.rendering.api.ItemResourceValue;
import com.android.ide.common.resources.ResourceResolver;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.tools.idea.configurations.*;
import com.android.tools.idea.editors.theme.attributes.AttributesGrouper;
import com.android.tools.idea.editors.theme.attributes.AttributesModelColorPaletteModel;
import com.android.tools.idea.editors.theme.attributes.AttributesTableModel;
import com.android.tools.idea.editors.theme.attributes.TableLabel;
import com.android.tools.idea.editors.theme.attributes.editors.StyleListPaletteCellRenderer;
import com.android.tools.idea.editors.theme.datamodels.EditedStyleItem;
import com.android.tools.idea.editors.theme.datamodels.ThemeEditorStyle;
import com.android.tools.idea.editors.theme.preview.AndroidThemePreviewPanel;
import com.android.tools.idea.editors.theme.ui.ResourceComponent;
import com.android.tools.idea.rendering.ResourceNotificationManager;
import com.android.tools.idea.rendering.ResourceNotificationManager.ResourceChangeListener;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenameDialog;
import com.intellij.ui.*;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.android.dom.drawable.DrawableDomElement;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.PanelUI;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class ThemeEditorComponent extends Splitter {
  private static final Logger LOG = Logger.getInstance(ThemeEditorComponent.class);

  private static final JBColor PREVIEW_BACKGROUND = new JBColor(new Color(0xFAFAFA), new Color(0x343739));

  private static final ImmutableMap<String, Integer> SORTING_MAP =
    ImmutableMap.<String, Integer>builder()
      .put(MaterialColors.PRIMARY_MATERIAL_ATTR, 1)
      .put(MaterialColors.PRIMARY_DARK_MATERIAL_ATTR, 2)
      .put(MaterialColors.ACCENT_MATERIAL_ATTR, 3)
      .build();

  /**
   * Comparator used for simple mode attribute sorting
   */
  public static final Comparator SIMPLE_MODE_COMPARATOR = new Comparator() {
    @Override
    public int compare(Object o1, Object o2) {
      // The parent attribute goes always first
      if (o1 instanceof ThemeEditorStyle) {
        return -1;
      } else if (o2 instanceof ThemeEditorStyle) {
        return 1;
      }

      if (o1 instanceof EditedStyleItem && o2 instanceof EditedStyleItem) {
        Integer pos1 = SORTING_MAP.get(((EditedStyleItem)o1).getName());
        Integer pos2 = SORTING_MAP.get(((EditedStyleItem)o2).getName());
        if (pos1 != null && pos2 != null) {
          return pos1 - pos2;
        }
        if (pos1 != null) {
          return -1;
        }
        if (pos2 != null) {
          return 1;
        }
        return ((EditedStyleItem)o1).compareTo((EditedStyleItem)o2);
      }

      // Fall-back for other comparisons
      return Ordering.usingToString().compare(o1, o2);
    }
  };

  public static final int REGULAR_CELL_PADDING = 4;
  public static final int LARGE_CELL_PADDING = 10;
  private final Project myProject;

  private EditedStyleItem mySubStyleSourceAttribute;

  // Name of current selected Theme
  private String myThemeName;
  // Name of current selected subStyle within the theme
  private String mySubStyleName;

  // Subcomponents
  private final ThemeEditorContext myThemeEditorContext;
  private final AndroidThemePreviewPanel myPreviewPanel;

  private final StyleAttributesFilter myAttributesFilter;
  private TableRowSorter<AttributesTableModel> myAttributesSorter;
  private final SimpleModeFilter mySimpleModeFilter;

  private final AttributesPanel myPanel;
  private final ThemeEditorTable myAttributesTable;

  private final ResourceChangeListener myResourceChangeListener;
  private boolean myIsSubscribedResourceNotification;
  private MutableCollectionComboBoxModel<Module> myModuleComboModel;

  /** Next pending search. The {@link ScheduledFuture} allows us to cancel the next search before it runs. */
  private ScheduledFuture<?> myScheduledSearch;

  private String myHoverPreviewTheme;

  public interface GoToListener {
    void goTo(@NotNull EditedStyleItem value);
    void goToParent();
  }

  private AttributesTableModel myModel;

  public ThemeEditorComponent(@NotNull final Project project) {
    myProject = project;
    myPanel = new AttributesPanel();

    initializeModulesCombo(null);

    final JComboBox moduleCombo = myPanel.getModuleCombo();
    moduleCombo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        reload(myThemeName, mySubStyleName, getSelectedModule().getName());
      }
    });
    moduleCombo.setRenderer(new ListCellRendererWrapper<Module>() {
      @Override
      public void customize(JList list, Module value, int index, boolean selected, boolean hasFocus) {
        setText(value.getName());
      }
    });

    final Module selectedModule = myModuleComboModel.getSelected();
    assert selectedModule != null;

    final Configuration configuration = ThemeEditorUtils.getConfigurationForModule(selectedModule);

    myThemeEditorContext = new ThemeEditorContext(configuration);
    myThemeEditorContext.addConfigurationListener(new ConfigurationListener() {
      @Override
      public boolean changed(int flags) {
        // reloads the theme editor preview when the configuration folder is updated
        if ((flags & MASK_FOLDERCONFIG) != 0) {
          loadStyleAttributes();
          myThemeEditorContext.getConfiguration().save();
        }

        return true;
      }
    });

    myPreviewPanel = new AndroidThemePreviewPanel(myThemeEditorContext, PREVIEW_BACKGROUND);
    myPreviewPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

    GoToListener goToListener = new GoToListener() {
      @Override
      public void goTo(@NotNull EditedStyleItem value) {
        ResourceResolver resolver = myThemeEditorContext.getResourceResolver();
        if (value.isAttr() && getUsedStyle() != null && resolver != null) {
          // We need to resolve the theme attribute.
          // TODO: Do we need a full resolution or can we just try to get it from the StyleWrapper?
          ItemResourceValue resourceValue = (ItemResourceValue)resolver.findResValue(value.getValue(), false);
          if (resourceValue == null) {
            LOG.error("Unable to resolve " + value.getValue());
            return;
          }

          mySubStyleName = ResolutionUtils.getQualifiedValue(resourceValue);
        }
        else {
          mySubStyleName = value.getValue();
        }
        mySubStyleSourceAttribute = value;
        loadStyleAttributes();
      }

      @Override
      public void goToParent() {
        ThemeEditorComponent.this.goToParent();
      }
    };

    myAttributesTable = myPanel.getAttributesTable();
    myAttributesTable.customizeTable(myThemeEditorContext, myPreviewPanel);
    myAttributesTable.setGoToListener(goToListener);

    updateUiParameters();

    myAttributesFilter = new StyleAttributesFilter();
    mySimpleModeFilter = new SimpleModeFilter();

    myPanel.getBackButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        mySubStyleName = null;
        loadStyleAttributes();
      }
    });

    myPanel.getAdvancedFilterCheckBox().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myAttributesTable.isEditing()) {
          myAttributesTable.getCellEditor().cancelCellEditing();
        }

        myAttributesTable.clearSelection();
        myPanel.getPalette().clearSelection();

        configureFilter();

        ((TableRowSorter)myAttributesTable.getRowSorter()).sort();
        myAttributesTable.updateRowHeights();
      }
    });

    myPanel.getThemeCombo()
      .setRenderer(new StyleListPaletteCellRenderer(myThemeEditorContext, new StyleListPaletteCellRenderer.ItemHoverListener() {
        @Override
        public void itemHovered(@NotNull String name) {
          if (!name.equals(myHoverPreviewTheme)) {
            mySubStyleName = null;
            mySubStyleSourceAttribute = null;
            refreshPreviewPanel(name);
          }
        }
      }, myPanel.getThemeCombo()));

    myPanel.getThemeCombo().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object selectedItem = myPanel.getThemeCombo().getSelectedItem();
        if (selectedItem instanceof ThemeEditorStyle) {
          final ThemeEditorStyle theme = (ThemeEditorStyle)selectedItem;

          myThemeName = theme.getQualifiedName();
          mySubStyleName = null;
          mySubStyleSourceAttribute = null;

          loadStyleAttributes();
        }
        else {
          // Keep current theme name in combo box
          myPanel.setSelectedTheme(getSelectedTheme());

          if (ThemesListModel.CREATE_NEW_THEME.equals(selectedItem)) {
            createNewTheme();
          }
          else if (ThemesListModel.SHOW_ALL_THEMES.equals(selectedItem)) {
            selectNewTheme();
          }
          else {
            renameTheme();
          }
        }
      }
    });

    myPanel.getThemeCombo().addPopupMenuListener(new PopupMenuListener() {

      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
        myHoverPreviewTheme = null;
        mySubStyleName = null;
        mySubStyleSourceAttribute = null;
        loadStyleAttributes();
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
      }
    });

    myPanel.getAttrGroupCombo().setModel(new DefaultComboBoxModel(AttributesGrouper.GroupBy.values()));
    myPanel.getAttrGroupCombo().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        loadStyleAttributes();
      }
    });

    // Adds the Device selection button
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new OrientationMenuAction(myPreviewPanel, false));
    group.add(new DeviceMenuAction(myPreviewPanel, false));
    group.add(new TargetMenuAction(myPreviewPanel, true, false));
    group.add(new LocaleMenuAction(myPreviewPanel, false));

    ActionManager actionManager = ActionManager.getInstance();
    ActionToolbar actionToolbar = actionManager.createActionToolbar("ThemeToolbar", group, true);
    actionToolbar.setLayoutPolicy(ActionToolbar.WRAP_LAYOUT_POLICY);

    final JPanel toolbar = new JPanel(null);
    toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
    toolbar.setBorder(BorderFactory.createMatteBorder(7, 14, 7, 14, PREVIEW_BACKGROUND));
    toolbar.setBackground(PREVIEW_BACKGROUND);

    final JComponent actionToolbarComponent = actionToolbar.getComponent();
    actionToolbarComponent.setBackground(PREVIEW_BACKGROUND);
    // The action toolbar is not always populated immediately.
    // Wait to make sure the components exist before setting their background.
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        for (Component component : actionToolbarComponent.getComponents()) {
          component.setBackground(PREVIEW_BACKGROUND);
        }
      }
    });
    toolbar.add(actionToolbarComponent);

    final SearchTextField textField = new SearchTextField(true);
    // Avoid search box stretching more than 1 line.
    textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, textField.getPreferredSize().height));
    textField.setBackground(PREVIEW_BACKGROUND);
    // If the text field has icons outside of the search field, their background needs to be set correctly
    for (Component component : textField.getComponents()) {
      if (component instanceof JLabel) {
        component.setBackground(PREVIEW_BACKGROUND);
      }
    }

    final ScheduledExecutorService searchUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
    textField.addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        if (myScheduledSearch != null) {
          myScheduledSearch.cancel(false);
        }

        myScheduledSearch = searchUpdateScheduler.schedule(new Runnable() {
          @Override
          public void run() {
            myPreviewPanel.setSearchTerm(textField.getText());
          }
        }, 300, TimeUnit.MILLISECONDS);
      }
    });
    toolbar.add(textField);

    final JPanel previewPanel = new JPanel(new BorderLayout());
    previewPanel.add(myPreviewPanel, BorderLayout.CENTER);
    previewPanel.add(toolbar, BorderLayout.NORTH);

    setFirstComponent(previewPanel);
    setSecondComponent(myPanel.getRightPanel());
    setShowDividerControls(false);

    myResourceChangeListener = new ResourceChangeListener() {
      @Override
      public void resourcesChanged(@NotNull Set<ResourceNotificationManager.Reason> reason) {
        myThemeEditorContext.updateThemeResolver();
        reload(myThemeName, mySubStyleName);
      }
    };

    // Set an initial state in case that the editor didn't have a previously saved state
    // TODO: Try to be smarter about this and get the ThemeEditor to set a default state where there is no previous state
    reload(null);
  }

  @NotNull
  public Module getSelectedModule() {
    final Module module = myModuleComboModel.getSelected();
    assert module != null;

    return module;
  }

  private void initializeModulesCombo(@Nullable String defaultModuleName) {
    final ImmutableList<Module> modules = ThemeEditorUtils.findAndroidModules(myProject);
    assert modules.size() > 0 : "Theme Editor shouldn't be launched in a project with no Android modules";

    Module defaultModule = null;
    for (Module module : modules) {
      if (module.getName().equals(defaultModuleName)) {
        defaultModule = module;
        break;
      }
    }

    if (defaultModule == null) {
      myModuleComboModel = new MutableCollectionComboBoxModel<Module>(modules);
    }
    else {
      myModuleComboModel = new MutableCollectionComboBoxModel<Module>(modules, defaultModule);
    }

    final JComboBox moduleCombo = myPanel.getModuleCombo();
    moduleCombo.setModel(myModuleComboModel);
  }

  /**
   * Subscribes myResourceChangeListener to ResourceNotificationManager with current AndroidFacet.
   * By subscribing, myResourceChangeListener can track all internal and external changes in resources.
   */
  private void subscribeResourceNotification() {
    // Already subscribed, we check this, because sometimes selectNotify can be called twice
    if (myIsSubscribedResourceNotification) {
      return;
    }
    ResourceNotificationManager manager = ResourceNotificationManager.getInstance(myThemeEditorContext.getProject());
    AndroidFacet facet = AndroidFacet.getInstance(myThemeEditorContext.getCurrentContextModule());
    assert facet != null : myThemeEditorContext.getCurrentContextModule().getName() + " module doesn't have an AndroidFacet";
    manager.addListener(myResourceChangeListener, facet, null, null);
    myIsSubscribedResourceNotification = true;
  }

  /**
   * Unsubscribes myResourceChangeListener from ResourceNotificationManager with current AndroidFacet.
   */
  private void unsubscribeResourceNotification() {
    if (myIsSubscribedResourceNotification) {
      ResourceNotificationManager manager = ResourceNotificationManager.getInstance(myThemeEditorContext.getProject());
      AndroidFacet facet = AndroidFacet.getInstance(myThemeEditorContext.getCurrentContextModule());
      assert facet != null : myThemeEditorContext.getCurrentContextModule().getName() + " module doesn't have an AndroidFacet";
      manager.removeListener(myResourceChangeListener, facet, null, null);
      myIsSubscribedResourceNotification = false;
    }
  }

  /**
   * @see FileEditor#selectNotify().
   */
  public void selectNotify() {
    reload(myThemeName, mySubStyleName);
    // TODO calling reload will call subscribeResourceNotification, so why call it here?
    subscribeResourceNotification();
  }

  /**
   * @see FileEditor#deselectNotify().
   */
  public void deselectNotify() {
    unsubscribeResourceNotification();
  }

  private void configureFilter() {
    if (myPanel.isAdvancedMode()) {
      myAttributesFilter.setFilterEnabled(false);
      myAttributesSorter.setRowFilter(myAttributesFilter);
      myAttributesSorter.setSortKeys(null);
    } else {
      mySimpleModeFilter
        .configure(myModel.getDefinedAttributes(), ThemeEditorUtils.isSelectedAppCompatTheme(myThemeEditorContext));
      myAttributesSorter.setRowFilter(mySimpleModeFilter);
      myAttributesSorter.setSortKeys(ImmutableList.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
    }
  }

  /**
   * Launches dialog to create a new theme based on selected one.
   * @return whether creation of new theme succeeded.
   */
  private boolean createNewTheme() {
    String newThemeName = ThemeEditorUtils.showCreateNewStyleDialog(getSelectedTheme(), myThemeEditorContext, !isSubStyleSelected(), null);
    if (newThemeName != null) {
      // We don't need to call reload here, because myResourceChangeListener will take care of it
      myThemeName = newThemeName;
      mySubStyleName = null;
      return true;
    }
    return false;
  }

  /**
   * Launches dialog to choose a theme among all existing ones
   * @return whether the choice is valid
   */
  private boolean selectNewTheme() {
    ThemeSelectionDialog dialog = new ThemeSelectionDialog(myThemeEditorContext.getConfiguration());
    if (dialog.showAndGet()) {
      String newThemeName = dialog.getTheme();
      if (newThemeName != null) {
        // TODO: call loadStyleProperties instead
        reload(newThemeName);
        return true;
      }
    }
    return false;
  }

  /**
   * Uses Android Studio refactoring to rename the current theme
   * @return Whether the renaming is successful
   */
  private boolean renameTheme() {
    ThemeEditorStyle selectedTheme = getSelectedTheme();
    assert selectedTheme != null;
    assert selectedTheme.isProjectStyle();
    PsiElement namePsiElement = selectedTheme.getNamePsiElement();
    if (namePsiElement == null) {
      return false;
    }
    RenameDialog renameDialog = new RenameDialog(myThemeEditorContext.getProject(), namePsiElement, null, null);
    renameDialog.show();
    if (renameDialog.isOK()) {
      String newName = renameDialog.getNewName();
      // We don't need to call reload here, because myResourceChangeListener will take care of it
      myThemeName = selectedTheme.getQualifiedName().replace(selectedTheme.getName(), newName);
      mySubStyleName = null;
      return true;
    }
    return false;
  }

  public void goToParent() {
    ThemeEditorStyle selectedStyle = getUsedStyle();
    if (selectedStyle == null) {
      LOG.error("No style selected.");
      return;
    }

    ThemeEditorStyle parent = getUsedStyle().getParent(myThemeEditorContext.getThemeResolver());
    assert parent != null;

    // TODO: This seems like it could be confusing for users, we might want to differentiate parent navigation depending if it's
    // substyle or theme navigation.
    if (isSubStyleSelected()) {
      mySubStyleName = parent.getQualifiedName();
      loadStyleAttributes();
    }
    else {
      myPanel.setSelectedTheme(parent);
    }
  }

  @Nullable
  ThemeEditorStyle getHoveredTheme() {
    if (myHoverPreviewTheme != null) {
      return myThemeEditorContext.getThemeResolver().getTheme(myHoverPreviewTheme);
    }
    return null;
  }

  @Nullable
  ThemeEditorStyle getSelectedTheme() {
    if (myThemeName != null) {
      return myThemeEditorContext.getThemeResolver().getTheme(myThemeName);
    }
    return null;
  }

  @Nullable
  private ThemeEditorStyle getUsedStyle() {
    if (mySubStyleName != null) {
      return getCurrentSubStyle();
    }

    return getSelectedTheme();
  }

  @Nullable
  ThemeEditorStyle getCurrentSubStyle() {
    if (mySubStyleName == null) {
      return null;
    }
    return myThemeEditorContext.getThemeResolver().getTheme(mySubStyleName);
  }

  private boolean isSubStyleSelected() {
    return mySubStyleName != null;
  }

  // Never null, because the list of elements of attGroup is constant and never changed
  @NotNull
  private AttributesGrouper.GroupBy getSelectedAttrGroup() {
    return (AttributesGrouper.GroupBy)myPanel.getAttrGroupCombo().getSelectedItem();
  }

  /**
   * Sets a new value to the passed attribute. It will also trigger the reload if a change happened.
   * @param rv The attribute to set, including the current value.
   * @param strValue The new value.
   */
  private void createNewThemeWithAttributeValue(@NotNull final EditedStyleItem rv, @NotNull final String strValue) {
    if (strValue.equals(rv.getValue())) {
      // No modification required.
      return;
    }

    ThemeEditorStyle selectedStyle = getUsedStyle();
    if (selectedStyle == null) {
      LOG.error("No style/theme selected.");
      return;
    }

    // The current style is R/O so we need to propagate this change a new style.
    String message = String
      .format("<html>The %1$s '<code>%2$s</code>' is Read-Only.<br/>A new %1$s will be created to modify '<code>%3$s</code>'.<br/></html>",
              isSubStyleSelected() ? "style" : "theme", selectedStyle.getQualifiedName(), rv.getName());

    final String newStyleName = ThemeEditorUtils.showCreateNewStyleDialog(selectedStyle, myThemeEditorContext, !isSubStyleSelected(), message);

    if (newStyleName == null) {
      return;
    }

    // Need invokeLater to wait for the theme resolver to be aware of the newly created style through the resource change listener
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        myThemeEditorContext.updateThemeResolver();
        ThemeEditorStyle newStyle = myThemeEditorContext.getThemeResolver().getTheme(newStyleName);
        assert newStyle != null;
        newStyle.setValue(rv.getQualifiedName(), strValue);
      }
    });

    if (!isSubStyleSelected()) {
      // We changed a theme, so we are done.
      // We don't need to call reload, because myResourceChangeListener will take care of it
      myThemeName = newStyleName;
      mySubStyleName = null;
      return;
    }

    ThemeEditorStyle selectedTheme = getSelectedTheme();
    if (selectedTheme == null) {
      LOG.error("No theme selected.");
      return;
    }

    // Decide what property we need to modify.
    // If the modified style was pointed by a theme attribute, we need to use that theme attribute value
    // as property. Otherwise, just update the original property name with the new style.
    final String sourcePropertyName = mySubStyleSourceAttribute.isAttr() ?
                                      mySubStyleSourceAttribute.getAttrPropertyName():
                                      mySubStyleSourceAttribute.getQualifiedName();

    // We've modified a sub-style so we need to modify the attribute that was originally pointing to this.
    if (selectedTheme.isReadOnly()) {

      // The theme pointing to the new style is r/o so create a new theme and then write the value.
      message = String.format("<html>The style '%1$s' which references to '%2$s' is also Read-Only.<br/>" +
                              "A new theme will be created to point to the modified style '%3$s'.<br/></html>",
                              selectedTheme.getQualifiedName(), rv.getName(), newStyleName);

      final String newThemeName = ThemeEditorUtils.showCreateNewStyleDialog(selectedTheme, myThemeEditorContext, true, message);
      if (newThemeName != null) {
        // We don't need to call reload, because myResourceChangeListener will take care of it
        myThemeName = newThemeName;
        mySubStyleName = newStyleName;

        // Need invokeLater to wait for the theme resolver to be aware of the newly created theme through the resource change listener
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            myThemeEditorContext.updateThemeResolver();
            ThemeEditorStyle newTheme = myThemeEditorContext.getThemeResolver().getTheme(newThemeName);
            assert newTheme != null;
            newTheme.setValue(sourcePropertyName, newStyleName);
          }
        });
      }
    }
    else {
      // The theme pointing to the new style is writable, so go ahead.
      FolderConfiguration configurationToModify = selectedTheme.findBestConfiguration(myThemeEditorContext.getConfiguration().getFullConfig());
      selectedTheme.setValue(configurationToModify, sourcePropertyName, newStyleName);
      // We don't need to call reload, because myResourceChangeListener will take care of it
      mySubStyleName = newStyleName;
    }
  }

  /**
   * Reloads the attributes editor.
   * @param defaultThemeName The name to select from the themes list.
   */
  public void reload(@Nullable final String defaultThemeName) {
    reload(defaultThemeName, null);
  }

  public void reload(@Nullable final String defaultThemeName, @Nullable final String defaultSubStyleName) {
    reload(defaultThemeName, defaultSubStyleName, getSelectedModule().getName());
  }

  public void reload(@Nullable final String defaultThemeName, @Nullable final String defaultSubStyleName, @Nullable final String defaultModuleName) {
    // Need to clean myHoverPreviewTheme, because we are no longer "hovering".
    myHoverPreviewTheme = null;

    // Unsubscribing from ResourceNotificationManager, because Module might be changed
    unsubscribeResourceNotification();

    initializeModulesCombo(defaultModuleName);
    myThemeEditorContext.setCurrentContextModule(getSelectedModule());

    // Subscribes to ResourceNotificationManager with new facet
    subscribeResourceNotification();

    mySubStyleSourceAttribute = null;

    final ThemeResolver themeResolver = myThemeEditorContext.getThemeResolver();
    final ThemeEditorStyle defaultTheme = defaultThemeName == null ? null : themeResolver.getTheme(defaultThemeName);
    myPanel.getThemeCombo().setModel(
      new ThemesListModel(myThemeEditorContext, ThemeEditorUtils.getDefaultThemes(themeResolver), defaultTheme));
    myThemeName = (myPanel.getSelectedTheme() == null) ? null : myPanel.getSelectedTheme().getQualifiedName();
    mySubStyleName = (StringUtil.equals(myThemeName,defaultThemeName)) ? defaultSubStyleName : null;
    loadStyleAttributes();
  }

  /**
   * Loads the theme attributes table for the current selected theme or substyle.
   */
  private void loadStyleAttributes() {
    ThemeEditorStyle selectedTheme = getHoveredTheme();
    ThemeEditorStyle selectedStyle = null;

    if (selectedTheme == null) {
      selectedTheme = getSelectedTheme();
      selectedStyle = getCurrentSubStyle();
    }

    if (selectedTheme == null) {
      myPreviewPanel.showError(myThemeName);
      return;
    }

    myPreviewPanel.showError(null);
    myThemeEditorContext.setCurrentTheme(selectedTheme);
    myPanel.setSubstyleName(mySubStyleName);
    myPanel.getBackButton().setVisible(mySubStyleName != null);
    final Configuration configuration = myThemeEditorContext.getConfiguration();
    configuration.setTheme(selectedTheme.getQualifiedName());

    assert configuration.getResourceResolver() != null; // ResourceResolver is only null if no theme was set.
    myModel = new AttributesTableModel(configuration, selectedStyle != null ? selectedStyle : selectedTheme,
                                       getSelectedAttrGroup(), myThemeEditorContext);

    myModel.addThemePropertyChangedListener(new AttributesTableModel.ThemePropertyChangedListener() {
      @Override
      public void attributeChangedOnReadOnlyTheme(final EditedStyleItem attribute, final String newValue) {
        createNewThemeWithAttributeValue(attribute, newValue);
      }
    });

    myAttributesTable.setRowSorter(null); // Clean any previous row sorters.
    myAttributesSorter = new TableRowSorter<AttributesTableModel>(myModel);
    // This is only used when the sort keys are set (only set in simple mode).
    myAttributesSorter.setComparator(0, SIMPLE_MODE_COMPARATOR);

    configureFilter();

    myAttributesTable.setModel(myModel);
    myAttributesTable.setRowSorter(myAttributesSorter);
    myAttributesTable.updateRowHeights();

    myPanel.getPalette().setModel(new AttributesModelColorPaletteModel(configuration, myModel));
    myPanel.getPalette().addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          AttributesModelColorPaletteModel model = (AttributesModelColorPaletteModel)myPanel.getPalette().getModel();
          List<EditedStyleItem> references = model.getReferences((Color)e.getItem());
          if (references.isEmpty()) {
            return;
          }

          HashSet<String> attributeNames = new HashSet<String>(references.size());
          for (EditedStyleItem item : references) {
            attributeNames.add(item.getQualifiedName());
          }
          myAttributesFilter.setAttributesFilter(attributeNames);
          myAttributesFilter.setFilterEnabled(true);
        }
        else {
          myAttributesFilter.setFilterEnabled(false);
        }

        if (myAttributesTable.isEditing()) {
          myAttributesTable.getCellEditor().cancelCellEditing();
        }
        ((TableRowSorter)myAttributesTable.getRowSorter()).sort();
        myPanel.getAdvancedFilterCheckBox().getModel().setSelected(!myAttributesFilter.myIsFilterEnabled);
      }
    });

    myAttributesTable.updateRowHeights();
    myPreviewPanel.invalidateGraphicsRenderer();
    myPreviewPanel.revalidate();
    myAttributesTable.repaint();
    myPanel.getThemeCombo().repaint();
  }

  /**
   * Refreshes the preview panel for theme previews
   */
  private void refreshPreviewPanel(@NotNull String hoveredPreviewTheme) {
    myHoverPreviewTheme = hoveredPreviewTheme;

    ThemeEditorStyle hoveredTheme = myThemeEditorContext.getThemeResolver().getTheme(myHoverPreviewTheme);

    assert hoveredTheme != null;

    myThemeEditorContext.setCurrentTheme(hoveredTheme);
    final Configuration configuration = myThemeEditorContext.getConfiguration();
    configuration.setTheme(hoveredTheme.getQualifiedName());

    myPreviewPanel.invalidateGraphicsRenderer();
    myPreviewPanel.revalidate();
  }

  @Override
  public void dispose() {
    // First remove the table editor so that it won't be called after
    // objects it relies on, like the module, have themselves been disposed
    myAttributesTable.removeEditor();
    myThemeEditorContext.dispose();
    super.dispose();
  }

  class SimpleModeFilter extends AttributesFilter {
    public final Set<String> ATTRIBUTES_DEFAULT_FILTER = ImmutableSet
      .of("colorPrimary",
          "colorPrimaryDark",
          "colorAccent",
          "colorForeground",
          "textColorPrimary",
          "textColorSecondary",
          "textColorPrimaryInverse",
          "textColorSecondaryInverse",
          "colorBackground",
          "windowBackground",
          "navigationBarColor",
          "statusBarColor");

    public SimpleModeFilter() {
      myIsFilterEnabled = true;
      filterAttributes = new HashSet<String>();
    }

    public void configure(final Set<String> availableAttributes, boolean appCompat) {
      filterAttributes.clear();

      for (final String candidate : ATTRIBUTES_DEFAULT_FILTER) {
        if (appCompat && availableAttributes.contains(candidate)) {
          filterAttributes.add(candidate);
        } else {
          filterAttributes.add(SdkConstants.ANDROID_NS_NAME_PREFIX + candidate);
        }
      }
    }
  }

  abstract class AttributesFilter extends RowFilter<AttributesTableModel, Integer> {
    boolean myIsFilterEnabled;
    Set<String> filterAttributes;

    @Override
    public boolean include(Entry<? extends AttributesTableModel, ? extends Integer> entry) {
      if (!myIsFilterEnabled) {
        return true;
      }
      int row = entry.getIdentifier().intValue();
      if (entry.getModel().isThemeParentRow(row)) {
        return true;
      }

      // We use the column 1 because it's the one that contains the ItemResourceValueWrapper.
      Object value = entry.getModel().getValueAt(row, 1);
      if (value instanceof TableLabel) {
        return false;
      }

      String attributeName;
      if (value instanceof EditedStyleItem) {
        attributeName = ((EditedStyleItem)value).getQualifiedName();
      }
      else {
        attributeName = value.toString();
      }

      ThemeEditorStyle selectedTheme = getUsedStyle();
      if (selectedTheme == null) {
        LOG.error("No theme selected.");
        return false;
      }

      return filterAttributes.contains(attributeName);
    }
  }

  class StyleAttributesFilter extends AttributesFilter {
    public StyleAttributesFilter() {
      myIsFilterEnabled = true;
      filterAttributes = Collections.emptySet();
    }

    public void setFilterEnabled(boolean enabled) {
      this.myIsFilterEnabled = enabled;
    }

    /**
     * Set the attribute names we want to display.
     */
    public void setAttributesFilter(@NotNull Set<String> attributeNames) {
      filterAttributes = ImmutableSet.copyOf(attributeNames);
    }
  }

  @Override
  public void setUI(PanelUI ui) {
    super.setUI(ui);
    updateUiParameters();
  }

  private void updateUiParameters() {
    Font regularFont = UIUtil.getLabelFont();

    int regularFontSize = getFontMetrics(regularFont).getHeight();
    Font headerFont = regularFont.deriveFont(regularFontSize * ThemeEditorConstants.ATTRIBUTES_HEADER_FONT_SCALE);

    // The condition below isn't constant, because updateUiParameters() is triggered during
    // construction: constructor of ThemeEditorComponent calls constructor of Splitter, which
    // calls setUI at some point. If this condition is removed, theme editor would fail with
    // NPE during its startup.
    //noinspection ConstantConditions
    if (myAttributesTable == null) {
      return;
    }

    int headerFontSize = getFontMetrics(headerFont).getHeight();

    // We calculate the size of the resource cell (drawable and color cells) by creating a ResourceComponent that
    // we use to measure the preferred size.
    ResourceComponent sampleComponent = new ResourceComponent();
    int bigCellSize = sampleComponent.getPreferredSize().height;
    myAttributesTable.setClassHeights(ImmutableMap.of(
      Object.class, regularFontSize + REGULAR_CELL_PADDING,
      Color.class, bigCellSize,
      DrawableDomElement.class, bigCellSize,
      TableLabel.class, headerFontSize + LARGE_CELL_PADDING,
      AttributesTableModel.ParentAttribute.class, bigCellSize
    ));
  }
}

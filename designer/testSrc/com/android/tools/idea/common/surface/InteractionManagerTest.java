/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.common.surface;

import com.android.tools.adtui.ui.AdtUiCursors;
import com.android.tools.idea.common.SyncNlModel;
import com.android.tools.idea.common.api.InsertType;
import com.android.tools.idea.common.model.Coordinates;
import com.android.tools.idea.common.model.NlComponent;
import com.android.tools.idea.common.model.SelectionModel;
import com.android.tools.idea.common.scene.SceneComponent;
import com.android.tools.idea.common.scene.SceneContext;
import com.android.tools.idea.common.scene.TemporarySceneComponent;
import com.android.tools.idea.common.scene.draw.DisplayList;
import com.android.tools.idea.common.util.NlTreeDumper;
import com.android.tools.idea.uibuilder.LayoutTestCase;
import com.android.tools.idea.uibuilder.api.ViewEditor;
import com.android.tools.idea.uibuilder.fixtures.DropTargetDragEventBuilder;
import com.android.tools.idea.uibuilder.handlers.ImageViewHandler;
import com.android.tools.idea.uibuilder.handlers.ViewHandlerManager;
import com.android.tools.idea.uibuilder.surface.NlDesignSurface;
import com.android.tools.idea.uibuilder.surface.ScreenView;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetListener;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import static com.android.SdkConstants.*;
import static com.android.tools.idea.uibuilder.LayoutTestUtilities.*;

/**
 * TODO: remove layout-specific stuff, add generic tests.
 */
public class InteractionManagerTest extends LayoutTestCase {
  private final NlTreeDumper myTreeDumper = new NlTreeDumper(true, false);

  public void testDragAndDrop() throws Exception {
    // Drops a fragment (xmlFragment below) into the design surface (via drag & drop events) and verifies that
    // the resulting document ends up modified as expected.
    SyncNlModel model = model("test.xml", component(LINEAR_LAYOUT)
      .withAttribute(ATTR_ORIENTATION, VALUE_VERTICAL)
      .withBounds(0, 0, 200, 200)).build();

    ScreenView screenView = createScreen(model);

    DesignSurface designSurface = screenView.getSurface();
    InteractionManager manager = createManager(designSurface);

    @Language("XML")
    String xmlFragment = "" +
                         "<TextView xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                         "     android:id=\"@+id/textView\"\n" +
                         "     android:layout_width=\"wrap_content\"\n" +
                         "     android:layout_height=\"wrap_content\"\n" +
                         "     android:text=\"Hello World\"\n" +
                         "/>";
    Transferable transferable = createTransferable(DataFlavor.stringFlavor, xmlFragment);
    dragDrop(manager, 0, 0, 100, 100, transferable);
    Disposer.dispose(model);

    String expected = "NlComponent{tag=<LinearLayout>, instance=0}\n" +
                      "    NlComponent{tag=<TextView>, instance=1}";
    assertEquals(expected, myTreeDumper.toTree(model.getComponents()));
    assertEquals("Hello World", model.find("textView").getAttribute(ANDROID_URI, ATTR_TEXT));
  }

  public void testDragAndDropWithOnCreate() throws Exception {
    // Drops an ImageView and verifies that onCreate was called.
    ViewHandlerManager viewManager = ViewHandlerManager.get(myFacet);
    viewManager.registerHandler(IMAGE_VIEW, new FakeImageViewHandler());

    SyncNlModel model = model("test.xml", component(LINEAR_LAYOUT)
      .withAttribute(ATTR_ORIENTATION, VALUE_VERTICAL)
      .withBounds(0, 0, 200, 200)).build();

    ScreenView screenView = createScreen(model);

    DesignSurface designSurface = screenView.getSurface();
    InteractionManager manager = createManager(designSurface);

    @Language("XML")
    String xmlFragment = "" +
                         "<ImageView xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                         "     android:layout_width=\"wrap_content\"\n" +
                         "     android:layout_height=\"wrap_content\"\n" +
                         "/>";
    Transferable transferable = createTransferable(DataFlavor.stringFlavor, xmlFragment);
    dragDrop(manager, 0, 0, 100, 100, transferable);
    Disposer.dispose(model);

    String expected = "NlComponent{tag=<LinearLayout>, instance=0}\n" +
                      "    NlComponent{tag=<ImageView>, instance=1}";
    assertEquals(expected, myTreeDumper.toTree(model.getComponents()));
    SceneComponent sceneComponent = screenView.getScene().getRoot().getChild(0);
    assertEquals("@android:drawable/selected_image", sceneComponent.getNlComponent().getAttribute(ANDROID_URI, ATTR_SRC));
  }

  public void testSelectSingleComponent() {
    InteractionManager manager = setupLinearLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    clickMouse(manager, MouseEvent.BUTTON1, 1,
                                   Coordinates.getSwingXDip(screenView, textView.getCenterX()),
                                   Coordinates.getSwingYDip(screenView, textView.getCenterY()), 0);
    ImmutableList<NlComponent> selections = surface.getSelectionModel().getSelection();
    assertEquals(1, selections.size());
    assertEquals(textView.getNlComponent(), selections.get(0));
  }

  public void testSelectDraggedComponent() {
    InteractionManager manager = setupConstraintLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    SelectionModel selectionModel = surface.getSelectionModel();
    ImmutableList<NlComponent> selections = selectionModel.getSelection();
    assertEquals(0, selections.size());
    int startX = Coordinates.getSwingXDip(screenView, textView.getCenterX());
    int startY = Coordinates.getSwingYDip(screenView, textView.getCenterY());

    pressMouse(manager, MouseEvent.BUTTON1, startX, startY, 0);
    selections = selectionModel.getSelection();
    assertEquals(0, selections.size());

    dragMouse(manager, startX, startY, startX + 50, startY + 20, 0);
    selections = selectionModel.getSelection();
    assertEquals(1, selections.size());
    assertEquals(textView.getNlComponent(), selections.get(0));

    releaseMouse(manager, MouseEvent.BUTTON1, startX + 50, startY + 20, 0);
    selections = selectionModel.getSelection();
    assertEquals(1, selections.size());
    assertEquals(textView.getNlComponent(), selections.get(0));
    manager.stopListening();
  }

  public void testMultiSelectComponent() {
    InteractionManager manager = setupLinearLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);

    surface.getSelectionModel().clear();
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    clickMouse(manager, MouseEvent.BUTTON1, 1,
                                   Coordinates.getSwingXDip(screenView, textView.getCenterX()),
                                   Coordinates.getSwingYDip(screenView, textView.getCenterY()), 0);

    SceneComponent button = screenView.getScene().getSceneComponent("button");
    clickMouse(manager, MouseEvent.BUTTON1, 1,
                                   Coordinates.getSwingXDip(screenView, button.getCenterX()),
                                   Coordinates.getSwingYDip(screenView, button.getCenterY()), InputEvent.SHIFT_DOWN_MASK);

    ImmutableList<NlComponent> selections = surface.getSelectionModel().getSelection();
    assertEquals(2, selections.size());
    assertEquals(textView.getNlComponent(), selections.get(0));
    assertEquals(button.getNlComponent(), selections.get(1));

    // Now deselect one
    clickMouse(manager, MouseEvent.BUTTON1, 1,
               Coordinates.getSwingXDip(screenView, button.getCenterX()),
               Coordinates.getSwingYDip(screenView, button.getCenterY()), InputEvent.SHIFT_DOWN_MASK);

    selections = surface.getSelectionModel().getSelection();
    assertEquals(1, selections.size());
    assertEquals(textView.getNlComponent(), selections.get(0));

    // Now select with ctrl
    clickMouse(manager, MouseEvent.BUTTON1, 1,
               Coordinates.getSwingXDip(screenView, button.getCenterX()),
               Coordinates.getSwingYDip(screenView, button.getCenterY()), InputEvent.CTRL_MASK);

    selections = surface.getSelectionModel().getSelection();
    assertEquals(2, selections.size());
    assertEquals(textView.getNlComponent(), selections.get(0));
    assertEquals(button.getNlComponent(), selections.get(1));
  }

  public void testMarqueeSelect() {
    InteractionManager manager = setupLinearLayoutCursorTest();
    DesignSurface surface = manager.getSurface();

    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);

    SceneComponent button = screenView.getScene().getSceneComponent("button");
    int startX = -20;
    int startY = -20;

    int endX = Coordinates.getSwingXDip(screenView, button.getDrawX()) + 3;
    int endY = Coordinates.getSwingXDip(screenView, button.getDrawY()) + 3;
    pressMouse(manager, MouseEvent.BUTTON1, startX, startY, 0);
    dragMouse(manager, startX, startY, endX, endY, 0);
    releaseMouse(manager, MouseEvent.BUTTON1, endX, endY, 0);

    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    ImmutableList<NlComponent> selections = surface.getSelectionModel().getSelection();
    assertEquals(ImmutableList.of(textView.getNlComponent(), button.getNlComponent()), selections);

    surface.getSelectionModel().clear();

    startX = Coordinates.getSwingXDip(screenView, button.getDrawX() + button.getDrawWidth() + 20);
    startY = Coordinates.getSwingYDip(screenView, button.getDrawY() + button.getDrawHeight() + 20);
    endX = Coordinates.getSwingXDip(screenView, button.getDrawX() + button.getDrawWidth()) - 3;
    endY = Coordinates.getSwingXDip(screenView, button.getDrawY() + button.getDrawHeight()) - 3;
    pressMouse(manager, MouseEvent.BUTTON1, startX, startY, 0);
    dragMouse(manager, startX, startY, endX, endY, 0);
    releaseMouse(manager, MouseEvent.BUTTON1, endX, endY, 0);

    selections = surface.getSelectionModel().getSelection();
    assertEquals(ImmutableList.of(button.getNlComponent()), selections);

    manager.stopListening();
    Disposer.dispose(surface);
  }

  public void testLinearLayoutCursorHoverComponent() {
    InteractionManager manager = setupLinearLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    manager.updateCursor(Coordinates.getSwingXDip(screenView, textView.getCenterX()),
                         Coordinates.getSwingYDip(screenView, textView.getCenterY()));
    Mockito.verify(surface).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
  }

  public void testLinearLayoutCursorHoverComponentHandle() {
    InteractionManager manager = setupLinearLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    SelectionModel selectionModel = screenView.getSelectionModel();
    selectionModel.setSelection(ImmutableList.of(textView.getNlComponent()));
    textView.layout(SceneContext.get(screenView), 0);
    manager.updateCursor(Coordinates.getSwingXDip(screenView, textView.getDrawX() + textView.getDrawWidth()),
                         Coordinates.getSwingYDip(screenView, textView.getDrawY() + textView.getDrawHeight()));
    Mockito.verify(surface).setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
  }

  public void testLinearLayoutCursorHoverRoot() {
    InteractionManager manager = setupLinearLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    manager.updateCursor(Coordinates.getSwingXDip(screenView, textView.getDrawHeight() + textView.getDrawY() + 20),
                         Coordinates.getSwingYDip(screenView, textView.getCenterY()));
    Mockito.verify(surface).setCursor(null);
  }

  public void testLinearLayoutCursorHoverSceneHandle() {
    InteractionManager manager = setupLinearLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    Mockito.when(((NlDesignSurface)surface).hasCustomDevice()).thenReturn(true);
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    manager.updateCursor(screenView.getX() + screenView.getSize().width,
                         screenView.getY() + screenView.getSize().height);
    Mockito.verify(surface).setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
  }

  private InteractionManager setupLinearLayoutCursorTest() {
    SyncNlModel model = model("test.xml", component(LINEAR_LAYOUT)
      .withAttribute(ATTR_ORIENTATION, VALUE_VERTICAL)
      .withBounds(0, 0, 100, 100)
      .children(
        component(TEXT_VIEW)
          .withBounds(0, 0, 50, 50)
          .id("@+id/textView")
          .text("Hello World")
          .wrapContentWidth()
          .wrapContentHeight(),
        component(BUTTON)
          .id("@+id/button")
          .withBounds(50, 50, 50, 50)
          .text("Button")
          .wrapContentWidth()
          .wrapContentHeight()
        )).build();

    NlDesignSurface surface = (NlDesignSurface)model.getSurface();
    surface.getScene().buildDisplayList(new DisplayList(), 0);
    return createManager(surface);
  }

  public void testConstraintLayoutCursorHoverComponent() {
    InteractionManager manager = setupConstraintLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    manager.updateCursor(Coordinates.getSwingXDip(screenView, textView.getCenterX()),
                         Coordinates.getSwingYDip(screenView, textView.getCenterY()));
    Mockito.verify(surface).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
  }

  public void testConstraintLayoutCursorHoverComponentHandle() {
    InteractionManager manager = setupConstraintLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    SelectionModel selectionModel = screenView.getSelectionModel();
    selectionModel.setSelection(ImmutableList.of(textView.getNlComponent()));
    textView.layout(SceneContext.get(screenView), 0);
    manager.updateCursor(Coordinates.getSwingXDip(screenView, textView.getDrawX() + textView.getDrawWidth()),
                         Coordinates.getSwingYDip(screenView, textView.getDrawY() + textView.getDrawHeight()));
    Mockito.verify(surface).setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
  }

  public void testConstraintLayoutCursorHoverRoot() {
    InteractionManager manager = setupConstraintLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    SceneComponent textView = screenView.getScene().getSceneComponent("textView");
    manager.updateCursor(Coordinates.getSwingXDip(screenView, textView.getDrawHeight() + textView.getDrawY() + 20),
                         Coordinates.getSwingYDip(screenView, textView.getCenterY()));
    Mockito.verify(surface).setCursor(null);
  }

  public void testConstraintLayoutCursorHoverSceneHandle() {
    InteractionManager manager = setupConstraintLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    Mockito.when(((NlDesignSurface)surface).hasCustomDevice()).thenReturn(true);
    ScreenView screenView = (ScreenView)surface.getSceneView(0, 0);
    manager.updateCursor(screenView.getX() + screenView.getSize().width,
                         screenView.getY() + screenView.getSize().height);
    Mockito.verify(surface).setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
  }

  public void testCursorChangeWhenSetPanningTrue() {
    InteractionManager manager = setupConstraintLayoutCursorTest();
    DesignSurface surface = manager.getSurface();

    manager.setPanning(true);

    Mockito.verify(surface).setCursor(AdtUiCursors.GRAB);
  }

  public void testCursorChangeOnModifiedKeyPressed() {
    InteractionManager manager = setupConstraintLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    Point moved = new Point(0, 0);
    Mockito.when(surface.getScrollPosition()).thenReturn(moved);
    int modifierKeyMask = InputEvent.BUTTON1_DOWN_MASK |
                          (SystemInfo.isMac ? InputEvent.META_DOWN_MASK
                                            : InputEvent.CTRL_DOWN_MASK);

    assertTrue(manager.interceptPanInteraction(setupPanningMouseEvent(MouseEvent.MOUSE_PRESSED, modifierKeyMask), 0, 0));
    Mockito.verify(surface).setCursor(AdtUiCursors.GRABBING);
  }

  public void testInterceptPanModifiedKeyReleased() {
    InteractionManager manager = setupConstraintLayoutCursorTest();
    DesignSurface surface = manager.getSurface();
    Mockito.when(surface.getScrollPosition()).thenReturn(new Point(0, 0));

    assertFalse(manager.interceptPanInteraction(setupPanningMouseEvent(MouseEvent.MOUSE_RELEASED, 0), 0, 0));
    Mockito.verify(surface, Mockito.never()).setCursor(AdtUiCursors.GRABBING);
  }

  public void testReusingNlComponentWhenDraggingFromComponentTree() {
    SyncNlModel model = model("model.xml",
                              component(LINEAR_LAYOUT)
                                .withBounds(0, 0, 100, 100)
                                .id("@+id/outer")
                                .children(
                                  component(BUTTON)
                                    .withBounds(0, 0, 10, 10)
                                    .id("@+id/button"),
                                  component(LINEAR_LAYOUT)
                                    .withBounds(10, 0, 90, 100)
                                    .id("@+id/inner")
                                    .children(
                                      component(TEXT_VIEW)
                                        .withBounds(10, 0, 10, 10)
                                        .id("@+id/textView1"),
                                      component(TEXT_VIEW)
                                        .withBounds(20, 0, 10, 10)
                                        .id("@+id/textView2")
                                    )
                                )).build();
    NlComponent button = model.find("button");
    DesignSurface surface = createScreen(model).getSurface();
    surface.getScene().buildDisplayList(new DisplayList(), 0);
    surface.getSelectionModel().setSelection(ImmutableList.of(button));
    surface.setModel(model);
    Transferable transferable = surface.getSelectionAsTransferable();
    InteractionManager manager = new InteractionManager(surface);
    manager.startListening();
    dragDrop(manager, 0, 0, 40, 0, transferable, DnDConstants.ACTION_MOVE);

    Object listener = manager.getListener();
    assertTrue(listener instanceof DropTargetListener);
    DropTargetListener dropListener = (DropTargetListener)listener;

    DropTargetContext context = createDropTargetContext();
    dropListener.dragEnter(new DropTargetDragEventBuilder(context, 0, 0, transferable).withDropAction(DnDConstants.ACTION_MOVE).build());

    // SceneComponent should be reused.
    SceneComponent buttonSceneComponent = surface.getScene().getSceneComponent(button);
    assertFalse(surface.getScene().getSceneComponent(button) instanceof TemporarySceneComponent);
    assertEquals(button, buttonSceneComponent.getNlComponent());
  }

  private MouseEvent setupPanningMouseEvent(int id, int modifierKeyMask) {
    Component sourceMock = Mockito.mock(Component.class);
    Mockito.when(sourceMock.getLocationOnScreen()).thenReturn(new Point(0, 0));
    return new MouseEvent(
      sourceMock, id, 0, modifierKeyMask, 0, 0, 0, false);
  }

  private InteractionManager setupConstraintLayoutCursorTest() {
    SyncNlModel model = model("constraint.xml", component(CONSTRAINT_LAYOUT.defaultName())
      .withBounds(0, 0, 1000, 1000)
      .matchParentWidth()
      .matchParentHeight()
      .children(
        component(TEXT_VIEW)
          .id("@+id/textView")
          .withBounds(0, 0, 100, 100)
          .wrapContentWidth()
          .wrapContentHeight())).build();

    NlDesignSurface surface = (NlDesignSurface)model.getSurface();
    Mockito.when(surface.getScale()).thenReturn(1.0);
    surface.getScene().buildDisplayList(new DisplayList(), 0);
    return createManager(surface);
  }

  private static class FakeImageViewHandler extends ImageViewHandler {
    @Override
    public boolean onCreate(@NotNull ViewEditor editor,
                            @Nullable NlComponent parent,
                            @NotNull NlComponent newChild,
                            @NotNull InsertType insertType) {
      if (insertType == InsertType.CREATE) { // NOT InsertType.CREATE_PREVIEW
        setSrcAttribute(newChild, "@android:drawable/selected_image");
      }
      else {
        setSrcAttribute(newChild, "@android:drawable/btn_star");
      }
      return true;
    }
  }
}
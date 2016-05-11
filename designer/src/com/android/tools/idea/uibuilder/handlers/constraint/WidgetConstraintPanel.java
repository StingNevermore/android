/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.idea.uibuilder.handlers.constraint;

import android.support.constraint.solver.widgets.ConstraintAnchor;
import com.android.SdkConstants;
import com.android.tools.idea.configurations.Configuration;
import com.android.tools.idea.uibuilder.model.NlComponent;
import com.android.tools.idea.uibuilder.property.NlProperty;
import com.android.tools.sherpa.drawing.BlueprintColorSet;
import com.android.tools.sherpa.drawing.ColorSet;
import com.android.tools.sherpa.structure.WidgetsScene;
import com.intellij.openapi.util.text.StringUtil;

import org.jetbrains.annotations.NotNull;
import android.support.constraint.solver.widgets.ConstraintWidget;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * UI component for Constraint Inspector
 */
public class WidgetConstraintPanel extends JPanel {
  private static final String HORIZONTAL_TOOL_TIP_TEXT = "Horizontal Bias";
  private static final String VERTICAL_TOOL_TIP_TEXT = "Vertical Bias";
  public static final String SIZE_ANY = "0dp";
  final SingleWidgetView mMain;
  final JSlider mVerticalSlider = new JSlider(SwingConstants.VERTICAL);
  final JSlider mHorizontalSlider = new JSlider(SwingConstants.HORIZONTAL);
  private WidgetsScene mScene;
  ConstraintModel mConstraintModel;
  NlComponent mComponent;
  ConstraintWidget mWidget;
  public static final int UNCONNECTED = -1;

  private String mWidgetWidthCache;
  private String mWidgetHeightCache;

  ColorSet mColorSet = new InspectorColorSet();

  public void setProperty(NlProperty property) {
    updateComponents(property.getComponents());
  }

  static class InspectorColorSet extends BlueprintColorSet {
    InspectorColorSet() {
      mDrawBackground = false;
      mDrawWidgetInfos = true;
      mInspectorBackgroundColor = new Color(232, 232, 232);
      mInspectorFillColor = new Color(220, 220, 220);
      mInspectorHighlightsStrokeColor = new Color(3, 169, 244);
      mInspectorStrokeColor = new Color(40, 40, 40);
    }
  }

  public WidgetConstraintPanel(@NotNull List<NlComponent> components) {
    super(new GridBagLayout());
    super.setBorder(new EmptyBorder(4, 0, 0, 0));
    setBackground(mColorSet.getInspectorBackgroundColor());
    mMain = new SingleWidgetView(this, mColorSet);
    setPreferredSize(new Dimension(200, 216));
    mVerticalSlider.setMajorTickSpacing(50);
    mHorizontalSlider.setMajorTickSpacing(50);
    mVerticalSlider.setBackground(mColorSet.getInspectorBackgroundColor());
    mHorizontalSlider.setBackground(mColorSet.getInspectorBackgroundColor());
    mHorizontalSlider.setToolTipText(HORIZONTAL_TOOL_TIP_TEXT);
    mVerticalSlider.setToolTipText(VERTICAL_TOOL_TIP_TEXT);
    updateComponents(components);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridy = 0;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.BOTH;
    add(mVerticalSlider, gbc);
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.gridx = 1;

    add(mMain, gbc);
    gbc.weightx = 0;
    gbc.weighty = 0;
    gbc.gridy = 1;

    add(mHorizontalSlider, gbc);
    mVerticalSlider.setUI(new WidgetSliderUI(mVerticalSlider, mColorSet));
    mHorizontalSlider.setUI(new WidgetSliderUI(mHorizontalSlider, mColorSet));
    mHorizontalSlider.addChangeListener(e -> setHorizontalBias());
    mVerticalSlider.addChangeListener(e -> setVerticalBias());
  }

  /*-----------------------------------------------------------------------*/
  // code for getting values from ConstraintWidget to UI
  /*-----------------------------------------------------------------------*/

  /**
   * Called when mWidget is being changed
   */
  private void widgetChanged() {
    configureUI();
    repaint();
  }

  /**
   * This loads the parameters form mWidget the ConstraintWidget
   */
  private void configureUI() {
    int top = getMargin(ConstraintAnchor.Type.TOP);
    int left = getMargin(ConstraintAnchor.Type.LEFT);
    int right = getMargin(ConstraintAnchor.Type.RIGHT);
    int bottom = getMargin(ConstraintAnchor.Type.BOTTOM);
    boolean baseline = hasBaseline();
    int width = mWidget.getWidth();
    int height = mWidget.getHeight();

    boolean showVerticalSlider = bottom != UNCONNECTED && top != UNCONNECTED;
    boolean showHorizontalSlider = left != UNCONNECTED && right != UNCONNECTED;

    if (showHorizontalSlider) {
      float bias = mWidget.getHorizontalBiasPercent();
      mHorizontalSlider.setValue((int)(bias * 100));
    }

    if (showVerticalSlider) {
      float bias = mWidget.getVerticalBiasPercent();
      mVerticalSlider.setValue(100 - (int)(bias * 100));
    }

    mVerticalSlider.setEnabled(showVerticalSlider);
    mHorizontalSlider.setEnabled(showHorizontalSlider);
    mHorizontalSlider.invalidate();
    mVerticalSlider.invalidate();
    mVerticalSlider.setToolTipText(showVerticalSlider ? VERTICAL_TOOL_TIP_TEXT : null);
    mHorizontalSlider.setToolTipText(showHorizontalSlider ? HORIZONTAL_TOOL_TIP_TEXT : null);

    int widthVal = convert(mWidget.getHorizontalDimensionBehaviour());
    int heightValue = convert(mWidget.getVerticalDimensionBehaviour());
    mMain.configureUi(bottom, top, left, right, baseline, widthVal, heightValue);
  }

  /**
   * Convert Any to SingleWidgetView flags
   *
   * @param behaviour
   * @return
   */
  private int convert(ConstraintWidget.DimensionBehaviour behaviour) {
    switch (behaviour) {
      case FIXED:
        return SingleWidgetView.FIXED;
      case WRAP_CONTENT:
        return SingleWidgetView.WRAP_CONTENT;
      case ANY:
        return SingleWidgetView.ANY;
    }
    return SingleWidgetView.FIXED;
  }

  /**
   * Returns true if mWidget has a baseline
   *
   * @return
   */
  private boolean hasBaseline() {
    ConstraintAnchor anchor = mWidget.getAnchor(ConstraintAnchor.Type.BASELINE);
    return anchor != null && anchor.isConnected();
  }

  public void updateComponents(@NotNull List<NlComponent> components) {
    mComponent = components.isEmpty() ? null : components.get(0);
    if (mComponent != null) {
      if (mComponent != null) {
        mConstraintModel = ConstraintModel.getConstraintModel(mComponent.getModel());
        mScene = mConstraintModel.getScene();
        mConstraintModel.getSelection().setContinuousListener(e -> widgetChanged());
        //TODO: improve the tear-down mechanism
        mWidget = mScene.getWidget(mComponent.getTag());
        configureUI();
      }
    }
  }

  /**
   * Get the margin from connection return -1 if no connection
   *
   * @param type
   * @return
   */
  private int getMargin(ConstraintAnchor.Type type) {
    ConstraintAnchor anchor = mWidget.getAnchor(type);
    if (anchor != null && anchor.isConnected()) {
      return anchor.getMargin();
    }
    return UNCONNECTED;
  }

  /*-----------------------------------------------------------------------*/
  // values from widget & NL component to ui
  /*-----------------------------------------------------------------------*/

  /**
   * Read the values off of the NLcomponent and set up the UI
   *
   * @param component
   */
  public void configureUI(NlComponent component) {
    mComponent = component;
    if (component == null) return;

    String mWidgetName = component.getId();
    int bottom = ConstraintUtilities.getMargin(component, SdkConstants.ATTR_LAYOUT_MARGIN_BOTTOM);
    int top = ConstraintUtilities.getMargin(component, SdkConstants.ATTR_LAYOUT_MARGIN_TOP);
    int left = ConstraintUtilities.getMargin(component, SdkConstants.ATTR_LAYOUT_MARGIN_START);
    int right = ConstraintUtilities.getMargin(component, SdkConstants.ATTR_LAYOUT_MARGIN_END);

    String rl = component.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_RIGHT_TO_LEFT_OF);
    String rr = component.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_RIGHT_TO_RIGHT_OF);
    String ll = component.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_LEFT_TO_LEFT_OF);
    String lr = component.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_LEFT_TO_RIGHT_OF);
    String tt = component.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_TOP_TO_TOP_OF);
    String tb = component.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_TOP_TO_BOTTOM_OF);
    String bt = component.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_BOTTOM_TO_TOP_OF);
    String bb = component.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_BOTTOM_TO_BOTTOM_OF);
    String basline = component.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_BASELINE_TO_BASELINE_OF);
    String hbias = component.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_HORIZONTAL_BIAS);
    String vbias = component.getAttribute(SdkConstants.SHERPA_URI, SdkConstants.ATTR_LAYOUT_VERTICAL_BIAS);
    String widthStr = component.getAttribute(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_WIDTH);
    String heightStr = component.getAttribute(SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_HEIGHT);
    if (rl == null && rr == null) {
      right = -1;
    }
    if (ll == null && lr == null) {
      left = -1;
    }
    if (tt == null && tb == null) {
      top = -1;
    }
    if (bb == null && bt == null) {
      bottom = -1;
    }

    boolean showVerticalSlider = bottom >= 0 && top >= 0;
    boolean showHorizontalSlider = left >= 0 && right >= 0;
    mVerticalSlider.setEnabled(showVerticalSlider);
    mHorizontalSlider.setEnabled(showHorizontalSlider);
    mHorizontalSlider.invalidate();
    mVerticalSlider.invalidate();
    mVerticalSlider.setToolTipText(showVerticalSlider ? VERTICAL_TOOL_TIP_TEXT : null);
    mHorizontalSlider.setToolTipText(showHorizontalSlider ? HORIZONTAL_TOOL_TIP_TEXT : null);

    float horizBias = 0.5f;
    if (hbias != null && hbias.length() > 0) {
      horizBias = Float.parseFloat(hbias);
    }
    float vertBias = 0.5f;
    if (vbias != null && vbias.length() > 0) {
      vertBias = Float.parseFloat(vbias);
    }
    mHorizontalSlider.setValue((int)(horizBias * 100));
    mVerticalSlider.setValue(100 - (int)(vertBias * 100));
    int widthVal = SingleWidgetView.FIXED;
    int heightValue = SingleWidgetView.FIXED;
    if (SdkConstants.VALUE_WRAP_CONTENT.equals(widthStr)) {
      widthVal = SingleWidgetView.WRAP_CONTENT;
    }
    else if (SIZE_ANY.equals(widthStr)) {
      widthVal = SingleWidgetView.ANY;
    }
    else {
      mWidgetWidthCache = widthStr;
    }

    updateCacheSize(heightStr, heightValue);

    mMain.configureUi(bottom, top, left, right, basline != null, widthVal, heightValue);
  }

  private int updateCacheSize(String heightStr, int heightValue) {
    Configuration configuration = mComponent.getModel().getConfiguration();
    float scale = configuration.getDensity().getDpiValue() / 160.0f;

    // We don't have a width cache, which means the widget was initially
    // created in spring or wrap_content mode, we can just use the current
    // widget model's width as a fallback
    if (mWidgetWidthCache == null) {
      mWidgetWidthCache = String.valueOf((int)Math.max(mComponent.w / scale, 1.0)) + "dp";
    }

    if (SdkConstants.VALUE_WRAP_CONTENT.equals(heightStr)) {
      heightValue = SingleWidgetView.WRAP_CONTENT;
    }
    else if (SIZE_ANY.equals(heightStr)) {
      heightValue = SingleWidgetView.ANY;
    }
    else {
      mWidgetHeightCache = heightStr;
    }

    // See width above
    if (mWidgetHeightCache == null) {
      mWidgetHeightCache = String.valueOf((int)Math.max(mComponent.h / scale, 1.0)) + "dp";
    }
    return heightValue;
  }

  /*-----------------------------------------------------------------------*/
  // values from ui to widget & NL component
  /*-----------------------------------------------------------------------*/

  private void setMargin(ConstraintAnchor.Type type, int margin) {
    ConstraintAnchor anchor = mWidget.getAnchor(type);
    if (anchor != null) {
      anchor.setMargin(margin);
    }
  }

  private void killConstraint(ConstraintAnchor.Type type) {
    ConstraintAnchor anchor = mWidget.getAnchor(type);
    anchor.reset();
  }

  public void setHorizontalBias() {
    float bias = (mHorizontalSlider.getValue() / 100f);
    mConstraintModel.allowsUpdate(false);
    mWidget.setHorizontalBiasPercent(bias);
    mConstraintModel.allowsUpdate(true);
    mConstraintModel.renderInLayoutLib();
  }

  public void setVerticalBias() {
    float bias = 1f - (mVerticalSlider.getValue() / 100f);
    mConstraintModel.allowsUpdate(false);
    mWidget.setVerticalBiasPercent(bias);
    mConstraintModel.allowsUpdate(true);
    mConstraintModel.renderInLayoutLib();
  }

  public void setTopMargin(int margin) {
    setMargin(ConstraintAnchor.Type.TOP, margin);
    ConstraintUtilities
      .saveNlAttribute(mComponent, SdkConstants.NS_RESOURCES, SdkConstants.ATTR_LAYOUT_MARGIN_TOP, (margin < 0) ? null : (margin + "dp"));
  }

  public void setLeftMargin(int margin) {
    setMargin(ConstraintAnchor.Type.LEFT, margin);
    ConstraintUtilities
      .saveNlAttribute(mComponent, SdkConstants.NS_RESOURCES, SdkConstants.ATTR_LAYOUT_MARGIN_START, (margin < 0) ? null : (margin + "dp"));
  }

  public void setRightMargin(int margin) {
    setMargin(ConstraintAnchor.Type.RIGHT, margin);
    ConstraintUtilities
      .saveNlAttribute(mComponent, SdkConstants.NS_RESOURCES, SdkConstants.ATTR_LAYOUT_MARGIN_END, (margin < 0) ? null : (margin + "dp"));
  }

  public void setBottomMargin(int margin) {
    setMargin(ConstraintAnchor.Type.BOTTOM, margin);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.NS_RESOURCES, SdkConstants.ATTR_LAYOUT_MARGIN_BOTTOM,
                                        (margin < 0) ? null : (margin + "dp"));
  }

  public void killTopConstraint() {
    killConstraint(ConstraintAnchor.Type.TOP);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.NS_RESOURCES, SdkConstants.ATTR_LAYOUT_MARGIN_TOP, null);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ATTR_LAYOUT_TOP_TO_BOTTOM_OF, null);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ATTR_LAYOUT_TOP_TO_TOP_OF, null);
  }

  public void killLeftConstraint() {
    killConstraint(ConstraintAnchor.Type.LEFT);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.NS_RESOURCES, SdkConstants.ATTR_LAYOUT_MARGIN_START, null);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.NS_RESOURCES, SdkConstants.ATTR_LAYOUT_MARGIN_LEFT, null);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ATTR_LAYOUT_LEFT_TO_LEFT_OF, null);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ATTR_LAYOUT_LEFT_TO_RIGHT_OF, null);
  }

  public void killRightConstraint() {
    killConstraint(ConstraintAnchor.Type.RIGHT);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.NS_RESOURCES, SdkConstants.ATTR_LAYOUT_MARGIN_END, null);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.NS_RESOURCES, SdkConstants.ATTR_LAYOUT_MARGIN_RIGHT, null);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ATTR_LAYOUT_RIGHT_TO_LEFT_OF, null);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ATTR_LAYOUT_RIGHT_TO_RIGHT_OF, null);
  }

  public void killBottomConstraint() {
    killConstraint(ConstraintAnchor.Type.BOTTOM);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.NS_RESOURCES, SdkConstants.ATTR_LAYOUT_MARGIN_BOTTOM, null);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ATTR_LAYOUT_BOTTOM_TO_BOTTOM_OF, null);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ATTR_LAYOUT_BOTTOM_TO_TOP_OF, null);
  }

  public void killBaselineConstraint() {
    killConstraint(ConstraintAnchor.Type.BASELINE);
    ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ATTR_LAYOUT_BASELINE_TO_BASELINE_OF, null);
  }

  public void setHorizontalConstraint(int horizontalConstraint) {
    switch (horizontalConstraint) {
      case SingleWidgetView.ANY:
        ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ANDROID_URI,
                                            SdkConstants.ATTR_LAYOUT_WIDTH, "0dp");
        break;
      case SingleWidgetView.FIXED:
        ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ANDROID_URI,
                                            SdkConstants.ATTR_LAYOUT_WIDTH, mWidgetWidthCache);
        break;
      case SingleWidgetView.WRAP_CONTENT:
        ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ANDROID_URI, SdkConstants.ATTR_LAYOUT_WIDTH,
                                            SdkConstants.VALUE_WRAP_CONTENT);
        break;
    }
  }

  public void setVerticalConstraint(int verticalConstraint) {

    switch (verticalConstraint) {
      case SingleWidgetView.ANY:
        ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ANDROID_URI,
                                            SdkConstants.ATTR_LAYOUT_HEIGHT, "0dp");
        break;
      case SingleWidgetView.FIXED:
        ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ANDROID_URI,
                                            SdkConstants.ATTR_LAYOUT_HEIGHT, mWidgetHeightCache);
        break;
      case SingleWidgetView.WRAP_CONTENT:
        ConstraintUtilities.saveNlAttribute(mComponent, SdkConstants.ANDROID_URI,
                                            SdkConstants.ATTR_LAYOUT_HEIGHT, SdkConstants.VALUE_WRAP_CONTENT);
        break;
    }
  }

  /*-----------------------------------------------------------------------*/
  //Look and Feel for the sliders
  /*-----------------------------------------------------------------------*/

  static class WidgetSliderUI extends BasicSliderUI {
    static final int thumbSize = 22;
    private static Font sSmallFont = new Font("Helvetica", Font.PLAIN, 10);
    ColorSet mColorSet;

    WidgetSliderUI(JSlider s, ColorSet colorSet) {
      super(s);
      mColorSet = colorSet;
    }

    @Override
    protected Dimension getThumbSize() {
      return new Dimension(thumbSize, thumbSize);
    }

    @Override
    public void paintTrack(Graphics g) {
      if (slider.isEnabled()) {
        super.paintTrack(g);
      }
    }

    @Override
    protected Color getShadowColor() {
      return mColorSet.getInspectorStrokeColor();
    }

    @Override
    protected Color getHighlightColor() {
      return mColorSet.getInspectorStrokeColor();
    }

    @Override
    protected Color getFocusColor() {
      return new Color(0, 0, 0, 0);
    }

    @Override
    public void paintThumb(Graphics g) {
      String percentText = Integer.toString(slider.getValue());
      if (!slider.isEnabled()) {
        return;
      }
      g.setColor(mColorSet.getInspectorFillColor());
      ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.fillRoundRect(thumbRect.x + 1, thumbRect.y + 1, thumbRect.width - 2, thumbRect.height - 2, thumbRect.width - 2,
                      thumbRect.height - 2);
      g.setColor(mColorSet.getInspectorStrokeColor());
      g.drawRoundRect(thumbRect.x + 1, thumbRect.y + 1, thumbRect.width - 2, thumbRect.height - 2, thumbRect.width - 2,
                      thumbRect.height - 2);
      int x = thumbRect.x + thumbRect.width / 2;
      int y = thumbRect.y + thumbRect.height / 2 - 1;
      g.setFont(sSmallFont);
      FontMetrics fm = g.getFontMetrics();
      Rectangle2D bounds = fm.getStringBounds(percentText, g);
      double tw = bounds.getWidth();

      g.setColor(mColorSet.getInspectorStrokeColor());
      g.drawString(percentText, (int)(x - tw / 2), (y + fm.getAscent() / 2));
    }
  }
}

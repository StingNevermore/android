/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.profilers.cpu.analysis;

import com.android.tools.adtui.AxisComponent;
import com.android.tools.adtui.FilterComponent;
import com.android.tools.adtui.TabularLayout;
import com.android.tools.adtui.model.AspectObserver;
import com.android.tools.adtui.model.ViewBinder;
import com.android.tools.adtui.model.filter.Filter;
import com.android.tools.adtui.model.filter.FilterHandler;
import com.android.tools.adtui.model.filter.FilterResult;
import com.android.tools.perflib.vmtrace.ClockType;
import com.android.tools.profilers.JComboBoxView;
import com.android.tools.profilers.ProfilerLayout;
import com.android.tools.profilers.StudioProfilersView;
import com.android.tools.profilers.cpu.capturedetails.CaptureDetails;
import com.android.tools.profilers.cpu.capturedetails.CaptureDetailsView;
import com.android.tools.profilers.cpu.capturedetails.ChartDetailsView;
import com.android.tools.profilers.cpu.capturedetails.TreeDetailsView;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.SimpleListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/**
 * Analysis tab that displays charts of type {@link CaptureDetailsView}
 */
public class CpuAnalysisChart extends CpuAnalysisTab<CpuAnalysisChartModel<?>> {
  private final StudioProfilersView myProfilersView;
  private final JPanel myCaptureDetailsPanel = new JPanel(new BorderLayout());
  private CaptureDetailsView myActiveDetailsView;

  @NotNull
  private final ViewBinder<StudioProfilersView, CaptureDetails, CaptureDetailsView> myBinder;

  @NotNull private final AspectObserver myObserver = new AspectObserver();

  public CpuAnalysisChart(@NotNull StudioProfilersView view, @NotNull CpuAnalysisTabModel<?> model) {
    super((CpuAnalysisChartModel<?>)model);
    myProfilersView = view;
    myBinder = new ViewBinder<>();
    myBinder.bind(CaptureDetails.TopDown.class, TreeDetailsView.TopDownDetailsView::new);
    myBinder.bind(CaptureDetails.BottomUp.class, TreeDetailsView.BottomUpDetailsView::new);
    myBinder.bind(CaptureDetails.CallChart.class, ChartDetailsView.CallChartDetailsView::new);
    myBinder.bind(CaptureDetails.FlameChart.class, ChartDetailsView.FlameChartDetailsView::new);

    getModel().getAspectModel().addDependency(myObserver).onChange(CpuAnalysisChartModel.Aspect.CLOCK_TYPE, this::updateDetailsView);

    buildComponents();
  }

  private void buildComponents() {
    setLayout(new TabularLayout("*", "Fit,*"));

    // Toolbar
    JPanel toolbar = new JPanel(new BorderLayout());
    toolbar.add(buildFilterComponent(), BorderLayout.WEST);
    toolbar.add(buildClockTypeSelector(), BorderLayout.EAST);

    updateDetailsView();
    add(toolbar, new TabularLayout.Constraint(0, 0));
    add(myCaptureDetailsPanel, new TabularLayout.Constraint(1, 0));

    boolean hasAxisComponent = getModel().getDetailsType() == CaptureDetails.Type.FLAME_CHART;
    // For backwards compatibility adding the percentage AxisComponent here. This really should live in the CaptureDetails.Type.FLAME_CHART.
    if (hasAxisComponent) {
      AxisComponent percentAxis = new AxisComponent(getModel().getAxisComponentModel(), AxisComponent.AxisOrientation.BOTTOM);
      percentAxis.setShowAxisLine(true);
      percentAxis.setMinimumSize(new Dimension(0, ProfilerLayout.TIME_AXIS_HEIGHT));
      add(percentAxis, new TabularLayout.Constraint(3, 0));
    }
  }

  private FilterComponent buildFilterComponent() {
    FilterComponent filterComponent = new FilterComponent(
      Filter.EMPTY_FILTER,
      ProfilerLayout.FILTER_TEXT_FIELD_WIDTH,
      ProfilerLayout.FILTER_TEXT_HISTORY_SIZE,
      ProfilerLayout.FILTER_TEXT_FIELD_TRIGGER_DELAY_MS)
      // TODO(b/112703942): Show again when we can completely support this value
      .setMatchCountVisibility(false);
    filterComponent.getModel().setFilterHandler(new FilterHandler() {
      @NotNull
      @Override
      protected FilterResult applyFilter(@NotNull Filter filter) {
        CpuAnalysisChartModel.CaptureDetailsWithFilterResult results = getModel().applyFilterAndCreateDetails(filter);
        updateDetailsView();
        // Return filter result.
        return results.getFilterResult();
      }
    });
    filterComponent.setVisible(true);
    return filterComponent;
  }

  private JComponent buildClockTypeSelector() {
    JComboBox<ClockType> clockTypeSelector = new ComboBox<>();
    JComboBoxView<ClockType, CpuAnalysisChartModel.Aspect> clockTypes =
      new JComboBoxView<>(clockTypeSelector, getModel().getAspectModel(), CpuAnalysisChartModel.Aspect.CLOCK_TYPE,
                          getModel()::getClockTypes, getModel()::getClockType, getModel()::setClockType);
    clockTypes.bind();
    clockTypeSelector.setRenderer(SimpleListCellRenderer.create("", value ->
      value == ClockType.GLOBAL ? "Wall Clock Time" :
      value == ClockType.THREAD ? "Thread Time" : ""));
    clockTypeSelector.setEnabled(getModel().isCaptureDualClock());
    return clockTypeSelector;
  }

  private void updateDetailsView() {
    myCaptureDetailsPanel.removeAll();
    // Need to hold a hard reference to the capture details view otherwise soft dependencies get cleaned up.
    myActiveDetailsView = myBinder.build(myProfilersView, getModel().createDetails());
    // Capture details view
    myCaptureDetailsPanel.add(myActiveDetailsView.getComponent(), BorderLayout.CENTER);
  }
}

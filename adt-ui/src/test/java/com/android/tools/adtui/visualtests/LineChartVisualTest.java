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

package com.android.tools.adtui.visualtests;

import com.android.tools.adtui.Animatable;
import com.android.tools.adtui.AnimatedComponent;
import com.android.tools.adtui.AnimatedTimeRange;
import com.android.tools.adtui.SelectionComponent;
import com.android.tools.adtui.chart.linechart.DurationDataRenderer;
import com.android.tools.adtui.chart.linechart.LineChart;
import com.android.tools.adtui.chart.linechart.LineConfig;
import com.android.tools.adtui.chart.linechart.OverlayComponent;
import com.android.tools.adtui.common.AdtUiUtils;
import com.android.tools.adtui.model.*;
import com.intellij.util.containers.ImmutableList;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.android.tools.adtui.common.AdtUiUtils.GBC_FULL;
import static com.android.tools.adtui.model.DurationData.UNSPECIFIED_DURATION;

public class LineChartVisualTest extends VisualTest {

  private final JLabel mClickDisplayLabel = new JLabel();

  private LineChart mLineChart;

  private SelectionComponent mySelectionComponent;

  private OverlayComponent myOverlayComponent;

  private List<RangedContinuousSeries> mRangedData;

  private List<DefaultDataSeries<Long>> mData;

  private AnimatedTimeRange mAnimatedTimeRange;

  private DefaultDataSeries<DefaultDurationData> mDurationData1;

  private DefaultDataSeries<DefaultDurationData> mDurationData2;

  private DurationDataRenderer<DefaultDurationData> mDurationRendererBlocking;

  private DurationDataRenderer<DefaultDurationData> mDurationRendererAttached;

  @Override
  protected List<Animatable> createComponentsList() {
    mRangedData = new ArrayList<>();
    mData = new ArrayList<>();

    long nowUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
    Range timeGlobalRangeUs = new Range(nowUs, nowUs + TimeUnit.SECONDS.toMicros(60));
    mLineChart = new LineChart();
    mAnimatedTimeRange = new AnimatedTimeRange(timeGlobalRangeUs, 0);

    List<Animatable> componentsList = new ArrayList<>();

    mySelectionComponent = new SelectionComponent(new Range(0, 0), timeGlobalRangeUs);
    myOverlayComponent = new OverlayComponent(mySelectionComponent);

    // Add the scene components to the list
    componentsList.add(mAnimatedTimeRange);
    componentsList.add(mLineChart);
    componentsList.add(mySelectionComponent);


    Range yRange = new Range(0.0, 100.0);
    for (int i = 0; i < 4; i++) {
      if (i % 2 == 0) {
        yRange = new Range(0.0, 100.0);
      }
      DefaultDataSeries<Long> series = new DefaultDataSeries<>();
      RangedContinuousSeries ranged =
        new RangedContinuousSeries("Widgets", timeGlobalRangeUs, yRange, series);
      mRangedData.add(ranged);
      mData.add(series);
    }
    mLineChart.addLines(mRangedData);

    mDurationData1 = new DefaultDataSeries<>();
    mDurationData2 = new DefaultDataSeries<>();
    RangedSeries<DefaultDurationData> series1 = new RangedSeries<>(timeGlobalRangeUs, mDurationData1);
    RangedSeries<DefaultDurationData> series2 = new RangedSeries<>(timeGlobalRangeUs, mDurationData2);
    mDurationRendererBlocking = new DurationDataRenderer.Builder(series1, Color.WHITE)
      .setLabelColors(Color.DARK_GRAY, Color.GRAY, Color.lightGray, Color.WHITE)
      .setIsBlocking(true)
      .setIcon(UIManager.getIcon("Tree.leafIcon"))
      .setLabelProvider(durationdata -> "Blocking")
      .setClickHander(durationData -> mClickDisplayLabel.setText(durationData.toString())).build();

    mDurationRendererAttached = new DurationDataRenderer.Builder(series2, Color.WHITE)
      .setLabelColors(Color.DARK_GRAY, Color.GRAY, Color.lightGray, Color.WHITE)
      .setIcon(UIManager.getIcon("Tree.leafIcon"))
      .setLabelProvider(durationdata -> "Attached")
      .setAttachLineSeries(mRangedData.get(0))
      .setClickHander(durationData -> mClickDisplayLabel.setText(durationData.toString())).build();
    mLineChart.addCustomRenderer(mDurationRendererBlocking);
    mLineChart.addCustomRenderer(mDurationRendererAttached);
    myOverlayComponent.addDurationDataRenderer(mDurationRendererBlocking);
    myOverlayComponent.addDurationDataRenderer(mDurationRendererAttached);
    componentsList.add(mDurationRendererBlocking);
    componentsList.add(mDurationRendererAttached);
    componentsList.add(myOverlayComponent);

    return componentsList;
  }

  @Override
  protected List<AnimatedComponent> getDebugInfoComponents() {
    return Collections.singletonList(mLineChart);
  }

  @Override
  public String getName() {
    return "LineChart";
  }

  @Override
  protected void populateUi(@NotNull JPanel panel) {
    JPanel layered = new JPanel(new GridBagLayout());
    JPanel controls = VisualTest.createControlledPane(panel, layered);
    mLineChart.setBorder(BorderFactory.createLineBorder(AdtUiUtils.DEFAULT_BORDER_COLOR));
    layered.add(myOverlayComponent, GBC_FULL);
    layered.add(mySelectionComponent, GBC_FULL);
    layered.add(mLineChart, GBC_FULL);

    final AtomicInteger variance = new AtomicInteger(10);
    final AtomicInteger delay = new AtomicInteger(100);
    Thread updateDataThread = new Thread() {
      @Override
      public void run() {
        super.run();
        try {
          while (true) {
            int v = variance.get();
            long nowUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
            for (DefaultDataSeries<Long> series : mData) {
              ImmutableList<SeriesData<Long>> data = series.getAllData();
              long last = data.isEmpty() ? 0 : data.get(data.size() - 1).value;
              float delta = ((float)Math.random() - 0.45f) * v;
              // Make sure not to add negative numbers.
              long current = Math.max(last + (long)delta, 0);
              series.add(nowUs, current);
            }
            Thread.sleep(delay.get());
          }
        }
        catch (InterruptedException e) {
        }
      }
    };

    updateDataThread.start();
    controls.add(VisualTest.createVariableSlider("Delay", 10, 5000, new VisualTests.Value() {
      @Override
      public void set(int v) {
        delay.set(v);
      }

      @Override
      public int get() {
        return delay.get();
      }
    }));
    controls.add(VisualTest.createVariableSlider("Variance", 0, 50, new VisualTests.Value() {
      @Override
      public void set(int v) {
        variance.set(v);
      }

      @Override
      public int get() {
        return variance.get();
      }
    }));
    controls.add(VisualTest.createVariableSlider("Line width", 1, 10, new VisualTests.Value() {
      @Override
      public void set(int v) {
        Stroke stroke = new BasicStroke(v);
        for (int i = 0; i < mRangedData.size(); i += 2) {
          RangedContinuousSeries series = mRangedData.get(i);
          mLineChart.getLineConfig(series).setStroke(stroke);
        }
      }

      @Override
      public int get() {
        // Returns the stroke width of the first line, in case there is one, or a default (1) value
        RangedContinuousSeries firstSeries = mRangedData.get(0);
        Stroke firstLineStroke = mLineChart.getLineConfig(firstSeries).getStroke();
        return firstLineStroke instanceof BasicStroke ? (int)((BasicStroke)firstLineStroke).getLineWidth() : 1;
      }
    }));
    controls.add(VisualTest.createCheckbox("Shift xRange Min", itemEvent ->
      mAnimatedTimeRange.setShift(itemEvent.getStateChange() == ItemEvent.SELECTED)));
    controls.add(VisualTest.createCheckbox("Stepped chart", itemEvent -> {
      boolean isStepped = itemEvent.getStateChange() == ItemEvent.SELECTED;
      // Make only some lines stepped
      for (int i = 0; i < mRangedData.size(); i += 2) {
        RangedContinuousSeries series = mRangedData.get(i);
        mLineChart.getLineConfig(series).setStepped(isStepped);
      }
    }));
    controls.add(VisualTest.createCheckbox("Dashed lines", itemEvent -> {
      Stroke stroke = itemEvent.getStateChange() == ItemEvent.SELECTED ? LineConfig.DEFAULT_DASH_STROKE : LineConfig.DEFAULT_LINE_STROKE;
      // Dash only some lines
      for (int i = 0; i < mRangedData.size(); i += 2) {
        RangedContinuousSeries series = mRangedData.get(i);
        mLineChart.getLineConfig(series).setStroke(stroke);
      }
    }));
    controls.add(VisualTest.createCheckbox("Filled lines", itemEvent -> {
      boolean isFilled = itemEvent.getStateChange() == ItemEvent.SELECTED;
      // Fill only some lines
      for (int i = 0; i < mRangedData.size(); i += 2) {
        RangedContinuousSeries series = mRangedData.get(i);
        mLineChart.getLineConfig(series).setFilled(isFilled);
      }
    }));
    controls.add(VisualTest.createCheckbox("Stacked lines", itemEvent -> {
      boolean isStacked = itemEvent.getStateChange() == ItemEvent.SELECTED;
      // Stack only some lines
      for (int i = 0; i < mRangedData.size(); i += 2) {
        RangedContinuousSeries series = mRangedData.get(i);
        mLineChart.getLineConfig(series).setStacked(isStacked);
      }
    }));

    JButton tapButton = VisualTest.createButton("Generate Duration1 (Hold)");
    tapButton.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        // Starts a new test event and give it a max duration.
        long nowUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
        DefaultDurationData newEvent = new DefaultDurationData(UNSPECIFIED_DURATION);
        mDurationData1.add(nowUs, newEvent);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        // Wraps up the latest event by assigning it a duration value relative to where it was started.
        long nowUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
        ImmutableList<SeriesData<DefaultDurationData>> allEvents = mDurationData1.getAllData();
        SeriesData<DefaultDurationData> lastEvent = allEvents.get(allEvents.size() - 1);
        lastEvent.value.setDuration(nowUs - lastEvent.x);
      }
    });
    controls.add(tapButton);

    tapButton = VisualTest.createButton("Generate Duration2 (Hold)");
    tapButton.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        // Starts a new test event and give it a max duration.
        long nowUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
        DefaultDurationData newEvent = new DefaultDurationData(UNSPECIFIED_DURATION);
        mDurationData2.add(nowUs, newEvent);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        // Wraps up the latest event by assigning it a duration value relative to where it was started.
        long nowUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
        ImmutableList<SeriesData<DefaultDurationData>> allEvents = mDurationData2.getAllData();
        SeriesData<DefaultDurationData> lastEvent = allEvents.get(allEvents.size() - 1);
        lastEvent.value.setDuration(nowUs - lastEvent.x);
      }
    });
    controls.add(tapButton);

    controls.add(mClickDisplayLabel);

    controls.add(
      new Box.Filler(new Dimension(0, 0), new Dimension(300, Integer.MAX_VALUE),
                     new Dimension(300, Integer.MAX_VALUE)));
  }
}

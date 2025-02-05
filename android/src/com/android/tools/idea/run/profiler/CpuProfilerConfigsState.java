/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.tools.idea.run.profiler;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * A Service that manages and persists states of {@link CpuProfilerConfig}.
 */
@State(name = "CpuProfilerConfigsState", storages = @Storage("cpuProfilingConfigs.xml"))
public class CpuProfilerConfigsState implements PersistentStateComponent<CpuProfilerConfigsState> {
  @NotNull
  private List<CpuProfilerConfig> myUserConfigs;

  /**
   * Default constructor is required and used by {@link PersistentStateComponent}.
   *
   * @see <a href="https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html">Persisting State of Components (IntelliJ Platform Docs)</a>
   */
  public CpuProfilerConfigsState() {
    myUserConfigs = new ArrayList<>();
  }

  @NotNull
  public static CpuProfilerConfigsState getInstance(Project project) {
    return project.getService(CpuProfilerConfigsState.class);
  }

  @NotNull
  public List<CpuProfilerConfig> getUserConfigs() {
    return myUserConfigs;
  }

  public void setUserConfigs(@NotNull List<CpuProfilerConfig> configs) {
    myUserConfigs = configs;
  }

  /**
   * @param config - adds the given {@param config} if there is no configuration with the same name
   *                (including default configurations), otherwise ignores.
   * @return true if the config was added.
   */
  public boolean addUserConfig(@NotNull CpuProfilerConfig config) {
    if (getConfigByName(config.getName()) == null) {
      myUserConfigs.add(config);
      return true;
    }
    return false;
  }

  @NotNull
  public static List<CpuProfilerConfig> getDefaultConfigs() {
    ImmutableList.Builder<CpuProfilerConfig> configs = new ImmutableList.Builder<CpuProfilerConfig>()
      .add(new CpuProfilerConfig(CpuProfilerConfig.Technology.SAMPLED_NATIVE))
      .add(new CpuProfilerConfig(CpuProfilerConfig.Technology.SYSTEM_TRACE))
      .add(new CpuProfilerConfig(CpuProfilerConfig.Technology.INSTRUMENTED_JAVA))
      .add(new CpuProfilerConfig(CpuProfilerConfig.Technology.SAMPLED_JAVA));
    return configs.build();
  }

  @NotNull
  public List<CpuProfilerConfig> getConfigs() {
    return ImmutableList.<CpuProfilerConfig>builder().addAll(getDefaultConfigs()).addAll(getUserConfigs()).build();
  }

  @Nullable
  public CpuProfilerConfig getConfigByName(@NotNull String name) {
    return getConfigs().stream()
      .filter(c -> name.equals(c.getName()))
      .findFirst()
      .orElse(null);
  }

  @Nullable
  @Override
  public CpuProfilerConfigsState getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull CpuProfilerConfigsState state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
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
package com.android.tools.idea.gradle.project.facet.ndk;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import icons.AndroidIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * {@link NdkFacet}'s type.
 */
public class NdkFacetType extends FacetType<NdkFacet, NdkFacetConfiguration> {
  public NdkFacetType() {
    super(NdkFacet.getFacetTypeId(), NdkFacet.getFacetId(), NdkFacet.getFacetName());
  }

  @NotNull
  @Override
  public NdkFacetConfiguration createDefaultConfiguration() {
    return new NdkFacetConfiguration();
  }

  @NotNull
  @Override
  public NdkFacet createFacet(@NotNull Module module,
                              @NotNull String name,
                              @NotNull NdkFacetConfiguration configuration,
                              @Nullable Facet underlyingFacet) {
    return new NdkFacet(module, name, configuration);
  }

  @Override
  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AndroidIcons.Android;
  }
}


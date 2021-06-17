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
package com.android.tools.idea.gradle.dsl.api.android;

import com.android.tools.idea.gradle.dsl.api.ExternalNativeBuildModel;
import com.android.tools.idea.gradle.dsl.api.ext.ResolvedPropertyModel;
import com.android.tools.idea.gradle.dsl.api.util.GradleDslModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AndroidModel extends GradleDslModel {
  @NotNull
  AaptOptionsModel aaptOptions();

  @NotNull
  AdbOptionsModel adbOptions();

  @NotNull
  ResolvedPropertyModel aidlPackagedList();

  @NotNull
  AndroidResourcesModel androidResources();

  @NotNull
  ResolvedPropertyModel assetPacks();

  @NotNull
  ResolvedPropertyModel buildToolsVersion();

  @NotNull
  List<BuildTypeModel> buildTypes();

  @NotNull
  BuildTypeModel addBuildType(@NotNull String buildType);

  void removeBuildType(@NotNull String buildType);

  @NotNull
  CompileOptionsModel compileOptions();

  @NotNull
  ResolvedPropertyModel compileSdkVersion();

  @NotNull
  ComposeOptionsModel composeOptions();

  @NotNull
  DataBindingModel dataBinding();

  @NotNull
  ProductFlavorModel defaultConfig();

  @NotNull
  ResolvedPropertyModel defaultPublishConfig();

  @NotNull
  DexOptionsModel dexOptions();

  @NotNull
  ResolvedPropertyModel dynamicFeatures();

  @NotNull
  ExternalNativeBuildModel externalNativeBuild();

  @NotNull
  ResolvedPropertyModel flavorDimensions();

  @NotNull
  ResolvedPropertyModel generatePureSplits();

  @NotNull
  InstallationModel installation();

  @NotNull
  KotlinOptionsModel kotlinOptions();

  @NotNull
  LintOptionsModel lintOptions();

  @NotNull
  ResolvedPropertyModel namespace();

  @NotNull
  ResolvedPropertyModel ndkVersion();

  @NotNull
  PackagingOptionsModel packagingOptions();

  @NotNull
  List<ProductFlavorModel> productFlavors();

  @NotNull
  ProductFlavorModel addProductFlavor(@NotNull String flavor);

  void removeProductFlavor(@NotNull String flavor);

  @NotNull
  List<SigningConfigModel> signingConfigs();

  @NotNull
  SigningConfigModel addSigningConfig(@NotNull String config);

  void removeSigningConfig(@NotNull String configName);

  @NotNull
  List<SourceSetModel> sourceSets();

  @NotNull
  SourceSetModel addSourceSet(@NotNull String sourceSet);

  void removeSourceSet(@NotNull String sourceSet);

  @NotNull
  SplitsModel splits();

  @NotNull
  ResolvedPropertyModel targetProjectPath();

  @NotNull
  ResolvedPropertyModel testNamespace();

  @NotNull
  TestOptionsModel testOptions();

  @NotNull
  ResolvedPropertyModel publishNonDefault();

  @NotNull
  ResolvedPropertyModel resourcePrefix();

  @NotNull
  ViewBindingModel viewBinding();

  @NotNull // TODO(b/149459214): but maybe should be nullable?  return null if we are not an application AndroidModel?
  DependenciesInfoModel dependenciesInfo();

  @NotNull
  BuildFeaturesModel buildFeatures();
}

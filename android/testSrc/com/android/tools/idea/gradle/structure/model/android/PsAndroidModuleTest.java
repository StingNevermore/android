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
package com.android.tools.idea.gradle.structure.model.android;

import com.android.tools.idea.gradle.structure.model.PsArtifactDependencySpec;
import com.android.tools.idea.gradle.structure.model.PsProject;
import com.android.tools.idea.gradle.structure.model.meta.ParsedValue;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static com.android.builder.model.AndroidProject.ARTIFACT_MAIN;
import static com.android.tools.idea.testing.TestProjectPaths.*;
import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.toList;

/**
 * Tests for {@link PsAndroidModule}.
 */
public class PsAndroidModuleTest extends DependencyTestCase {

  public void testFlavorDimensions() throws Throwable {
    loadProject(PSD_SAMPLE);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByName("app");
    assertNotNull(appModule);

    List<String> flavorDimensions = getFlavorDimensions(appModule);
    assertThat(flavorDimensions)
      .containsExactly("foo", "bar").inOrder();
  }

  public void testAddFlavorDimension() throws Throwable {
    loadProject(PSD_SAMPLE);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByName("app");
    assertNotNull(appModule);

    appModule.addNewFlavorDimension("new");
    // A product flavor is required for successful sync.
    PsProductFlavor newInNew = appModule.addNewProductFlavor("new_in_new");
    newInNew.setDimension(new ParsedValue.Set.Parsed<String>("new", null));
    appModule.applyChanges();

    requestSyncAndWait();
    project = new PsProject(resolvedProject);
    appModule = (PsAndroidModule)project.findModuleByName("app");

    List<String> flavorDimensions = getFlavorDimensions(appModule);
    assertThat(flavorDimensions)
      .containsExactly("foo", "bar", "new").inOrder();
  }

  public void testRemoveFlavorDimension() throws Throwable {
    loadProject(PSD_SAMPLE);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByName("app");
    assertNotNull(appModule);

    appModule.removeFlavorDimension("bar");
    // A product flavor must be removed for successful sync.
    appModule.removeProductFlavor(appModule.findProductFlavor("bar"));
    List<String> flavorDimensions = getFlavorDimensions(appModule);
    assertThat(flavorDimensions).containsExactly("foo", "bar").inOrder();
    appModule.applyChanges();

    requestSyncAndWait();
    project = new PsProject(resolvedProject);
    appModule = (PsAndroidModule)project.findModuleByName("app");

    flavorDimensions = getFlavorDimensions(appModule);
    assertThat(flavorDimensions).containsExactly("foo");
  }

  @NotNull
  private static List<String> getFlavorDimensions(@NotNull PsAndroidModule module) {
    return Lists.newArrayList(module.getFlavorDimensions());
  }

  public void testProductFlavors() throws Throwable {
    loadProject(PROJECT_WITH_APPAND_LIB);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByName("app");
    assertNotNull(appModule);

    List<PsProductFlavor> productFlavors = getProductFlavors(appModule);
    assertThat(productFlavors.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("basic", "paid").inOrder();
    assertThat(productFlavors).hasSize(2);

    PsProductFlavor basic = appModule.findProductFlavor("basic");
    assertNotNull(basic);
    assertTrue(basic.isDeclared());

    PsProductFlavor release = appModule.findProductFlavor("paid");
    assertNotNull(release);
    assertTrue(release.isDeclared());
  }

  public void testAddProductFlavor() throws Throwable {
    loadProject(PROJECT_WITH_APPAND_LIB);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByName("app");
    assertNotNull(appModule);

    List<PsProductFlavor> productFlavors = getProductFlavors(appModule);
    assertThat(productFlavors.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("basic", "paid").inOrder();

    appModule.addNewProductFlavor("new_flavor");

    productFlavors = getProductFlavors(appModule);
    assertThat(productFlavors.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("basic", "paid", "new_flavor").inOrder();

    PsProductFlavor newFlavor = appModule.findProductFlavor("new_flavor");
    assertNotNull(newFlavor);
    assertNull(newFlavor.getResolvedModel());

    appModule.applyChanges();
    requestSyncAndWait();
    project = new PsProject(resolvedProject);
    appModule = (PsAndroidModule)project.findModuleByName("app");

    productFlavors = getProductFlavors(appModule);
    assertThat(productFlavors.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("basic", "paid", "new_flavor").inOrder();

    newFlavor = appModule.findProductFlavor("new_flavor");
    assertNotNull(newFlavor);
    assertNotNull(newFlavor.getResolvedModel());
  }

  public void testRemoveProductFlavor() throws Throwable {
    loadProject(PSD_SAMPLE);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByName("app");
    assertNotNull(appModule);

    List<PsProductFlavor> productFlavors = getProductFlavors(appModule);
    assertThat(productFlavors.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("basic", "paid", "bar").inOrder();

    appModule.removeProductFlavor(appModule.findProductFlavor("paid"));

    productFlavors = getProductFlavors(appModule);
    assertThat(productFlavors.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("basic", "bar").inOrder();

    appModule.applyChanges();
    requestSyncAndWait();
    project = new PsProject(resolvedProject);
    appModule = (PsAndroidModule)project.findModuleByName("app");

    productFlavors = getProductFlavors(appModule);
    assertThat(productFlavors.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("basic", "bar").inOrder();
  }

  @NotNull
  private static List<PsProductFlavor> getProductFlavors(@NotNull PsAndroidModule module) {
    List<PsProductFlavor> productFlavors = Lists.newArrayList();
    module.forEachProductFlavor(productFlavors::add);
    return productFlavors;
  }

  public void testBuildTypes() throws Throwable {
    loadProject(PROJECT_WITH_APPAND_LIB);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByName("app");
    assertNotNull(appModule);

    List<PsBuildType> buildTypes = getBuildTypes(appModule);
    assertThat(buildTypes.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("release", "debug").inOrder();
    assertThat(buildTypes).hasSize(2);

    PsBuildType release = appModule.findBuildType("release");
    assertNotNull(release);
    assertTrue(release.isDeclared());

    PsBuildType debug = appModule.findBuildType("debug");
    assertNotNull(debug);
    assertTrue(!debug.isDeclared());
  }

  public void testAddBuildType() throws Throwable {
    loadProject(PROJECT_WITH_APPAND_LIB);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByName("app");
    assertNotNull(appModule);

    List<PsBuildType> buildTypes = getBuildTypes(appModule);
    assertThat(buildTypes.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("release", "debug").inOrder();

    appModule.addNewBuildType("new_build_type");

    buildTypes = getBuildTypes(appModule);
    assertThat(buildTypes.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("release", "debug", "new_build_type").inOrder();

    PsBuildType newBuildType = appModule.findBuildType("new_build_type");
    assertNotNull(newBuildType);
    assertNull(newBuildType.getResolvedModel());

    appModule.applyChanges();
    requestSyncAndWait();
    project = new PsProject(resolvedProject);
    appModule = (PsAndroidModule)project.findModuleByName("app");

    buildTypes = getBuildTypes(appModule);
    assertThat(buildTypes.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("release", "new_build_type", "debug").inOrder();  // "debug" is not declared and goes last.

    newBuildType = appModule.findBuildType("new_build_type");
    assertNotNull(newBuildType);
    assertNotNull(newBuildType.getResolvedModel());
  }

  public void testRemoveBuildType() throws Throwable {
    loadProject(PSD_SAMPLE);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByName("app");
    assertNotNull(appModule);

    List<PsBuildType> buildTypes = getBuildTypes(appModule);
    assertThat(buildTypes.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("release", "debug").inOrder();

    appModule.removeBuildType(appModule.findBuildType("release"));

    buildTypes = getBuildTypes(appModule);
    assertThat(buildTypes.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("debug");

    appModule.applyChanges();
    requestSyncAndWait();
    project = new PsProject(resolvedProject);
    appModule = (PsAndroidModule)project.findModuleByName("app");

    buildTypes = getBuildTypes(appModule);
    assertThat(buildTypes.stream().map(v -> v.getName()).collect(toList()))
      .containsExactly("debug", "release").inOrder();  // "release" is not declared and goes last.

    PsBuildType release = appModule.findBuildType("release");
    assertNotNull(release);
    assertFalse(release.isDeclared());
  }

  @NotNull
  private static List<PsBuildType> getBuildTypes(@NotNull PsAndroidModule module) {
    List<PsBuildType> buildTypes = Lists.newArrayList();
    module.forEachBuildType(buildTypes::add);
    return buildTypes;
  }

  public void testVariants() throws Throwable {
    loadProject(PROJECT_WITH_APPAND_LIB);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByName("app");
    assertNotNull(appModule);

    Collection<PsVariant> variants = getVariants(appModule);
    assertThat(variants).hasSize(4);

    PsVariant paidDebug = appModule.findVariant("paidDebug");
    assertNotNull(paidDebug);
    List<String> flavors = paidDebug.getProductFlavors();
    assertThat(flavors).containsExactly("paid");

    PsVariant paidRelease = appModule.findVariant("paidRelease");
    assertNotNull(paidRelease);
    flavors = paidRelease.getProductFlavors();
    assertThat(flavors).containsExactly("paid");

    PsVariant basicDebug = appModule.findVariant("basicDebug");
    assertNotNull(basicDebug);
    flavors = basicDebug.getProductFlavors();
    assertThat(flavors).containsExactly("basic");

    PsVariant basicRelease = appModule.findVariant("basicRelease");
    assertNotNull(basicRelease);
    flavors = basicRelease.getProductFlavors();
    assertThat(flavors).containsExactly("basic");
  }

  @NotNull
  private static List<PsVariant> getVariants(@NotNull PsAndroidModule module) {
    List<PsVariant> variants = Lists.newArrayList();
    module.forEachVariant(variants::add);
    return variants;
  }

  public void testEditableDependencies() throws Throwable {
    loadProject(PSD_DEPENDENCY);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule module = (PsAndroidModule)project.findModuleByName("mainModule");
    assertNotNull(module);

    List<PsAndroidDependency> declaredDependencies = module.getDependencies().items();
    assertThat(declaredDependencies).hasSize(3);
    {
      // Verify that lib1:1.0 is considered a "editable" dependency, and it was matched properly
      PsLibraryAndroidDependency lib1 = (PsLibraryAndroidDependency)declaredDependencies.get(1);
      assertTrue(lib1.isDeclared());

      PsArtifactDependencySpec spec = lib1.getSpec();
      assertEquals("com.example.libs", spec.getGroup());
      assertEquals("lib1", spec.getName());
      assertEquals("1.0", spec.getVersion());

      Collection<String> variants = lib1.getVariants();
      assertThat(variants).containsExactly("debug", "release");

      for (String variant : variants) {
        assertNotNull(module.findVariant(variant));
        module.findVariant(variant).forEachArtifact(artifact -> {
          if (artifact.getResolvedName().equals(ARTIFACT_MAIN)) {
            if (artifact instanceof PsAndroidArtifact) {
              PsAndroidArtifactDependencyCollection resolvedDependencies = new PsAndroidArtifactDependencyCollection(artifact);
              // Verify that lib1 is considered a "editable" dependency, and it was matched properly
              PsLibraryAndroidDependency resolved =
                resolvedDependencies.findLibraryDependency("com.example.libs:lib1:1.0");
              assertTrue(resolved.isDeclared());
              assertFalse(resolved.getParsedModels().isEmpty());
            }
          }
        });
      }
    }
    {
      // Verify that lib1:0.9.1 is considered a "editable" dependency, and it was matched properly
      PsLibraryAndroidDependency lib1 = (PsLibraryAndroidDependency)declaredDependencies.get(2);
      assertTrue(lib1.isDeclared());

      PsArtifactDependencySpec spec = lib1.getSpec();
      assertEquals("com.example.libs", spec.getGroup());
      assertEquals("lib1", spec.getName());
      assertEquals("0.9.1", spec.getVersion());

      Collection<String> variants = lib1.getVariants();
      assertThat(variants).containsExactly("release");

      for (String variant : variants) {
        assertNotNull(module.findVariant(variant));
        module.findVariant(variant).forEachArtifact(artifact -> {
          if (artifact.getResolvedName().equals(ARTIFACT_MAIN)) {
            if (artifact instanceof PsAndroidArtifact) {
              PsAndroidArtifactDependencyCollection resolvedDependencies = new PsAndroidArtifactDependencyCollection(artifact);
              // Verify that lib1 is considered a "editable" dependency, and it was matched properly
              PsLibraryAndroidDependency resolved =
                resolvedDependencies.findLibraryDependency("com.example.libs:lib1:1.0");
              assertTrue(resolved.isDeclared());
              assertFalse(resolved.getParsedModels().isEmpty());
            }
          }
        });
      }
    }
  }

  public void testEditableDependenciesWithPlusInVersion() throws Throwable {
    loadProject(PSD_DEPENDENCY);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule modulePlus = (PsAndroidModule)project.findModuleByName("modulePlus");
    assertNotNull(modulePlus);

    List<PsAndroidDependency> declaredLib1Dependencies =
      modulePlus
        .getDependencies()
        .items()
        .stream()
        .filter(v -> v instanceof PsLibraryAndroidDependency && ((PsLibraryAndroidDependency)v).getSpec().getName().equals("lib1"))
        .collect(toList());

    // Verify that appcompat is considered a "editable" dependency, and it was matched properly
    PsLibraryAndroidDependency lib1 = (PsLibraryAndroidDependency)declaredLib1Dependencies.get(0);
    assertTrue(lib1.isDeclared());

    PsArtifactDependencySpec spec = lib1.getSpec();
    assertEquals("com.example.libs", spec.getGroup());
    assertEquals("lib1", spec.getName());
    assertThat(spec.getVersion()).isEqualTo("0.+");
    assertEquals("com.example.libs:lib1:0.+", spec.toString());

    // Verify that the variants where appcompat is are properly registered.
    Collection<String> variants = lib1.getVariants();
    assertThat(variants).containsExactly("debug", "release");

    for (String variant : variants) {
      assertNotNull(modulePlus.findVariant(variant));
      modulePlus.findVariant(variant).forEachArtifact(artifact -> {
        if (artifact instanceof PsAndroidArtifact && artifact.getResolvedName().equals(ARTIFACT_MAIN)) {
          PsAndroidArtifactDependencyCollection resolvedDependencies = new PsAndroidArtifactDependencyCollection(artifact);

          // Verify that lib1 is considered a "editable" dependency, and it was matched properly
          PsLibraryAndroidDependency resolvedAppCompatV7 =
            resolvedDependencies.findLibraryDependency("com.example.libs:lib1:0.9.1");
          assertTrue(resolvedAppCompatV7.isDeclared());
          assertFalse(resolvedAppCompatV7.getParsedModels().isEmpty());
        }
      });
    }
  }

  public void testCanDependOnModules() throws Throwable {
    loadProject(PROJECT_WITH_APPAND_LIB);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByName("app");
    assertNotNull(appModule);

    PsAndroidModule libModule = (PsAndroidModule)project.findModuleByName("lib");
    assertNotNull(libModule);

    assertTrue(appModule.canDependOn(libModule));
    assertFalse(libModule.canDependOn(appModule));
  }

  public void testSigningConfigs() throws Throwable {
    loadProject(BASIC);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByGradlePath(":");
    assertNotNull(appModule);

    List<PsSigningConfig> signingConfigs = getSigningConfigs(appModule);
    assertThat(signingConfigs).hasSize(2);

    PsSigningConfig myConfig = appModule.findSigningConfig("myConfig");
    assertNotNull(myConfig);
    assertTrue(myConfig.isDeclared());

    PsSigningConfig debugConfig = appModule.findSigningConfig("debug");
    assertNotNull(debugConfig);
    assertTrue(!debugConfig.isDeclared());
  }

  public void testAddSigningConfig() throws Throwable {
    loadProject(BASIC);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByGradlePath(":");
    assertNotNull(appModule);

    List<PsSigningConfig> signingConfigs = getSigningConfigs(appModule);
    assertThat(signingConfigs.stream().map(v -> v.getName()).collect(toList())).containsExactly("myConfig", "debug").inOrder();

    PsSigningConfig myConfig = appModule.addNewSigningConfig("config2");
    myConfig.setStoreFile(new ParsedValue.Set.Parsed<File>(new File("/tmp/1"), null));

    assertNotNull(myConfig);
    assertTrue(myConfig.isDeclared());

    signingConfigs = getSigningConfigs(appModule);
    assertThat(signingConfigs.stream().map(v -> v.getName()).collect(toList())).containsExactly("myConfig", "debug", "config2").inOrder();

    appModule.applyChanges();
    requestSyncAndWait();
    project = new PsProject(resolvedProject);
    appModule = (PsAndroidModule)project.findModuleByGradlePath(":");

    signingConfigs = getSigningConfigs(appModule);
    assertThat(signingConfigs.stream().map(v -> v.getName()).collect(toList())).containsExactly("myConfig", "config2", "debug").inOrder();
  }

  public void testRemoveSigningConfig() throws Throwable {
    loadProject(BASIC);

    Project resolvedProject = myFixture.getProject();
    PsProject project = new PsProject(resolvedProject);

    PsAndroidModule appModule = (PsAndroidModule)project.findModuleByGradlePath(":");
    assertNotNull(appModule);

    List<PsSigningConfig> signingConfigs = getSigningConfigs(appModule);
    assertThat(signingConfigs.stream().map(v -> v.getName()).collect(toList())).containsExactly("myConfig", "debug").inOrder();

    appModule.removeSigningConfig(appModule.findSigningConfig("myConfig"));
    appModule.removeBuildType(appModule.findBuildType("debug"));  // Remove (clean) the build type that refers to the signing config.

    signingConfigs = getSigningConfigs(appModule);
    assertThat(signingConfigs.stream().map(v -> v.getName()).collect(toList())).containsExactly("debug");

    appModule.applyChanges();
    requestSyncAndWait();
    project = new PsProject(resolvedProject);
    appModule = (PsAndroidModule)project.findModuleByGradlePath(":");

    signingConfigs = getSigningConfigs(appModule);
    assertThat(signingConfigs.stream().map(v -> v.getName()).collect(toList())).containsExactly("debug");
  }

  @NotNull
  private static List<PsSigningConfig> getSigningConfigs(@NotNull PsAndroidModule module) {
    List<PsSigningConfig> signingConfigs = Lists.newArrayList();
    module.forEachSigningConfig(signingConfigs::add);
    return signingConfigs;
  }
}

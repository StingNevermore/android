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
package com.android.tools.idea.gradle.dsl.model.build;

import static com.android.tools.idea.gradle.dsl.TestFileName.SUB_PROJECTS_OVERRIDE_SUB_PROJECT_SECTION;
import static com.android.tools.idea.gradle.dsl.TestFileName.SUB_PROJECTS_OVERRIDE_SUB_PROJECT_SECTION_SUB;
import static com.android.tools.idea.gradle.dsl.TestFileName.SUB_PROJECTS_SUB_PROJECTS_SECTION;
import static com.android.tools.idea.gradle.dsl.TestFileName.SUB_PROJECTS_SUB_PROJECTS_SECTION_WITH_LOCAL_PROPERTIES;

import com.android.tools.idea.gradle.dsl.api.java.JavaModel;
import com.android.tools.idea.gradle.dsl.model.GradleFileModelTestCase;
import com.intellij.pom.java.LanguageLevel;
import org.junit.Test;


/**
 * Tests subprojects section of the build.gradle file.
 */
public class SubProjectsTest extends GradleFileModelTestCase {
  @Test
  public void testSubProjectsSection() throws Exception {
    writeToSettingsFile(getSubModuleSettingsText());
    writeToBuildFile(SUB_PROJECTS_SUB_PROJECTS_SECTION);
    writeToSubModuleBuildFile("");

    JavaModel java = getGradleBuildModel().java();
    assertMissingProperty(java.sourceCompatibility());
    assertMissingProperty(java.targetCompatibility());

    JavaModel subModuleJava = getSubModuleGradleBuildModel().java();
    assertEquals(LanguageLevel.JDK_1_5, subModuleJava.sourceCompatibility().toLanguageLevel());
    assertEquals(LanguageLevel.JDK_1_6, subModuleJava.targetCompatibility().toLanguageLevel());
  }

  @Test
  public void testSubProjectsSectionWithLocalProperties() throws Exception {
    writeToSettingsFile(getSubModuleSettingsText());
    writeToBuildFile(SUB_PROJECTS_SUB_PROJECTS_SECTION_WITH_LOCAL_PROPERTIES);
    writeToSubModuleBuildFile("");

    JavaModel java = getGradleBuildModel().java();
    assertEquals(LanguageLevel.JDK_1_4, java.sourceCompatibility().toLanguageLevel()); // subprojects section applies only for sub projects.
    assertEquals(LanguageLevel.JDK_1_5, java.targetCompatibility().toLanguageLevel()); // subprojects section applies only for sub projects.

    JavaModel subModuleJava = getSubModuleGradleBuildModel().java();
    assertEquals(LanguageLevel.JDK_1_5,
                 subModuleJava.sourceCompatibility().toLanguageLevel()); // Subproject got 1_5 from SubProjects section
    assertEquals(LanguageLevel.JDK_1_6,
                 subModuleJava.targetCompatibility().toLanguageLevel()); // Subproject got 1_6 from SubProjects section
  }

  @Test
  public void testOverrideSubProjectsSection() throws Exception {
    writeToSettingsFile(getSubModuleSettingsText());
    writeToBuildFile(SUB_PROJECTS_OVERRIDE_SUB_PROJECT_SECTION);
    writeToSubModuleBuildFile(SUB_PROJECTS_OVERRIDE_SUB_PROJECT_SECTION_SUB);

    JavaModel java = getGradleBuildModel().java();
    assertEquals(LanguageLevel.JDK_1_5, java.sourceCompatibility().toLanguageLevel());
    assertEquals(LanguageLevel.JDK_1_6, java.targetCompatibility().toLanguageLevel());

    JavaModel subModuleJava = getSubModuleGradleBuildModel().java();
    assertEquals(LanguageLevel.JDK_1_6, subModuleJava.sourceCompatibility().toLanguageLevel()); // 1_4 is overridden with 1_6
    assertEquals(LanguageLevel.JDK_1_7, subModuleJava.targetCompatibility().toLanguageLevel()); // 1_5 is overridden with 1_7
  }
}

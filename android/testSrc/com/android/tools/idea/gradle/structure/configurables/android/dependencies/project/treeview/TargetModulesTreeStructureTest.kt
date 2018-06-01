/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.gradle.structure.configurables.android.dependencies.project.treeview

import com.android.tools.idea.gradle.structure.configurables.ui.PsUISettings
import com.android.tools.idea.gradle.structure.configurables.ui.testStructure
import com.android.tools.idea.gradle.structure.configurables.ui.treeview.AbstractPsNode
import com.android.tools.idea.gradle.structure.model.PsProject
import com.android.tools.idea.gradle.structure.model.android.DependencyTestCase
import com.android.tools.idea.gradle.structure.model.android.PsAndroidDependency
import com.android.tools.idea.gradle.structure.model.android.PsAndroidModule
import com.android.tools.idea.testing.TestProjectPaths
import com.intellij.openapi.project.Project
import org.hamcrest.CoreMatchers
import org.junit.Assert

class TargetModulesTreeStructureTest: DependencyTestCase() {
  private lateinit var resolvedProject: Project
  private lateinit var project: PsProject

  override fun setUp() {
    super.setUp()
    loadProject(TestProjectPaths.PSD_DEPENDENCY)
    reparse()
  }

  private fun reparse() {
    resolvedProject = myFixture.project
    project = PsProject(resolvedProject)
  }

  fun testTreeStructure() {
    val targetModulesTreeStructure = TargetModulesTreeStructure(PsUISettings())
    val node = targetModulesTreeStructure.rootElement as AbstractPsNode
    val nodeModels = mutableListOf<PsAndroidDependency>()

    // Simulate all-modules dependencies view single node selection.
    project.forEachModule { module ->
      if (module is PsAndroidModule) {
        nodeModels.addAll(module.dependencies.findLibraryDependencies("com.example.libs", "lib1"))
        module.variants.forEach { variant ->
          variant.forEachArtifact { artifact ->
            nodeModels.addAll(artifact.dependencies.findLibraryDependencies("com.example.libs", "lib1"))
          }
        }
      }
    }

    targetModulesTreeStructure.displayTargetModules(listOf(nodeModels))

    // Note: indentation matters!
    val expectedProjectStructure = """
      (null)
          mainModule
              debugImplementation
              implementation
              releaseImplementation
          modulePlus
              implementation""".trimIndent()
    val treeStructure = node.testStructure({ !it.name.startsWith("appcompat-v7") })
    // Note: If fails see a nice diff by clicking <Click to see difference> in the IDEA output window.
    Assert.assertThat(treeStructure.toString(), CoreMatchers.equalTo(expectedProjectStructure))
  }


}
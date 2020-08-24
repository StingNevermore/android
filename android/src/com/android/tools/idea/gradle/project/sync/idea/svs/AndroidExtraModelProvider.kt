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
package com.android.tools.idea.gradle.project.sync.idea.svs

import com.android.builder.model.AndroidProject
import com.android.builder.model.ModelBuilderParameter
import com.android.builder.model.NativeAndroidProject
import com.android.builder.model.ProjectSyncIssues
import com.android.builder.model.v2.models.ndk.NativeModelBuilderParameter
import com.android.builder.model.v2.models.ndk.NativeModule
import com.android.ide.gradle.model.GradlePluginModel
import com.android.tools.idea.gradle.project.sync.SyncActionOptions
import com.android.tools.idea.gradle.project.sync.idea.UsedInBuildAction
import com.android.tools.idea.gradle.project.sync.idea.getAdditionalClassifierArtifactsModel
import org.gradle.tooling.BuildController
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.tooling.model.Model
import org.gradle.tooling.model.gradle.BasicGradleProject
import org.gradle.tooling.model.gradle.GradleBuild
import org.jetbrains.kotlin.kapt.idea.KaptGradleModel
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider

@UsedInBuildAction
class AndroidExtraModelProvider(private val syncActionOptions: SyncActionOptions) : ProjectImportModelProvider {
  override fun populateBuildModels(
    controller: BuildController,
    buildModel: GradleBuild,
    consumer: ProjectImportModelProvider.BuildModelConsumer
  ) {
    val androidModules = populateAndroidModels(controller, buildModel)
    // Requesting ProjectSyncIssues must be performed "last" since all other model requests may produces addition issues.
    // Note that "last" here means last among Android models since many non-Android models are requested after this point.
    populateProjectSyncIssues(controller, androidModules)

    androidModules.forEach { it.deliverModels(consumer) }
  }

  override fun populateProjectModels(controller: BuildController,
                                     projectModel: Model,
                                     modelConsumer: ProjectImportModelProvider.ProjectModelConsumer) {
    controller.findModel(projectModel, GradlePluginModel::class.java)
      ?.also { pluginModel -> modelConsumer.consume(pluginModel, GradlePluginModel::class.java) }
    controller.findModel(projectModel, KaptGradleModel::class.java)
      ?.also { model -> modelConsumer.consume(model, KaptGradleModel::class.java) }
  }

  /**
   * Requests Android project models for the given [buildModel]
   *
   * We do this by going through each module and query Gradle for the following models:
   *   1. Query for the AndroidProject for the module
   *   2. Query for the NativeModule (V2 API) (only if we also obtain an AndroidProject)
   *   3. Query for the NativeAndroidProject (V1 API) (only if we can obtain AndroidProject but cannot obtain V2 NativeModule)
   *   4. Query for the GlobalLibraryMap for the module (we ALWAYS do this regardless of the other two models)
   *   5. (Single Variant Sync only) Work out which variant for which models we need to request, and request them.
   *      See IdeaSelectedVariantChooser for more details.
   *
   * If single variant sync is enabled then [findParameterizedAndroidModel] will use Gradle parameterized model builder API
   * in order to stop Gradle from building the variant.
   * All of the requested models are registered back to the external project system via the
   * [ProjectImportModelProvider.BuildModelConsumer] callback.
   */
  private fun populateAndroidModels(
    controller: BuildController,
    buildModel: GradleBuild
  ): List<AndroidModule> {
    val androidModules: MutableList<AndroidModule> = mutableListOf()
    buildModel.projects.forEach { gradleProject ->
      val androidProject = findParameterizedAndroidModel(controller, gradleProject, AndroidProject::class.java)
      if (androidProject != null) {
        val nativeModule = controller.getNativeModuleFromGradle(gradleProject)
        val nativeAndroidProject: NativeAndroidProject? =
          if (nativeModule != null) null
          else findParameterizedAndroidModel(controller, gradleProject, NativeAndroidProject::class.java)

        androidModules.add(AndroidModule(gradleProject, androidProject, nativeAndroidProject, nativeModule))
      }
    }

    if (syncActionOptions.isSingleVariantSyncEnabled) {
      // This section is for Single Variant Sync specific models if we have reached here we should have already requested AndroidProjects
      // without any Variant information. Now we need to request that Variant information for the variants that we are interested in.
      // e.g the ones that should be selected by the IDE.
      chooseSelectedVariants(controller, androidModules, syncActionOptions)
    }

    // AdditionalClassiferArtifactsModel must be requested after AndroidProject and Variant model since it requires the library list in dependency model.
    getAdditionalClassifierArtifactsModel(
      controller,
      androidModules,
      syncActionOptions.cachedLibraries,
      syncActionOptions.downloadAndroidxUISamplesSources
    )
    return androidModules
  }

  private fun populateProjectSyncIssues(
    controller: BuildController,
    androidModules: List<AndroidModule>
  ) {
    androidModules.forEach { module ->
      module.projectSyncIssues = controller.findModel(module.gradleProject, ProjectSyncIssues::class.java)
    }
  }

  /**
   * Gets the [AndroidProject] or [NativeAndroidProject] (based on [modelType]) for the given [BasicGradleProject].
   */
  private fun <T> findParameterizedAndroidModel(controller: BuildController,
                                                project: BasicGradleProject,
                                                modelType: Class<T>): T? {
    if (syncActionOptions.isSingleVariantSyncEnabled) {
      try {
        val model = controller.getModel(project, modelType, ModelBuilderParameter::class.java) { parameter ->
          parameter.shouldBuildVariant = false
        }
        if (model != null) return model
      }
      catch (e: UnsupportedVersionException) {
        // Using old version of Gradle. Fall back to full variants sync for this module.
      }
    }
    return controller.findModel(project, modelType)
  }

  private fun BuildController.getNativeModuleFromGradle(project: BasicGradleProject): NativeModule? {
    try {
      if (syncActionOptions.isSingleVariantSyncEnabled) {
        // With single variant mode, we first only collect basic project information. The more complex information will be collected later
        // for the selected variant and ABI.
        return getModel(project, NativeModule::class.java, NativeModelBuilderParameter::class.java) {
          it.variantsToGenerateBuildInformation = emptyList()
          it.abisToGenerateBuildInformation = emptyList()
        }
      }
      else {
        // If single variant is not enabled, we sync all variant and ABIs at once.
        return getModel(project, NativeModule::class.java, NativeModelBuilderParameter::class.java) {
          it.variantsToGenerateBuildInformation = null
          it.abisToGenerateBuildInformation = null
        }
      }
    }
    catch (e: UnsupportedVersionException) {
      // Using old version of Gradle that does not support V2 models.
      return null
    }
  }
}
/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.tools.idea.nav.safeargs.psi

import com.android.ide.common.resources.ResourceItem
import com.android.tools.idea.nav.safeargs.index.NavDestinationData
import com.android.tools.idea.res.getSourceAsVirtualFile
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import org.jetbrains.android.augment.AndroidLightClassBase
import org.jetbrains.android.facet.AndroidFacet

/**
 * Light class for Directions classes generated from navigation xml files.
 *
 * A "Direction" represents functionality that takes you away from one destination to another.
 *
 * For example, if you had the following "nav.xml":
 *
 * ```
 *  <navigation>
 *    <fragment id="@+id/mainMenu">
 *      <action id="@+id/actionToOptions" />
 *      <destination="@id/options" />
 *    </fragment>
 *    <fragment id="@+id/options">
 *  </navigation>
 * ```
 *
 * This would generate a class like the following:
 *
 * ```
 *  class MainMenuDirections {
 *    class ActionToOptions {}
 *    ActionToOptions actionToOptions();
 *  }
 * ```
 */
class LightDirectionsClass(facet: AndroidFacet,
                           modulePackage: String,
                           private val navigationResource: ResourceItem,
                           destination: NavDestinationData)
  : AndroidLightClassBase(PsiManager.getInstance(facet.module.project), setOf(PsiModifier.PUBLIC, PsiModifier.FINAL)) {

  private val name: String
  private val qualifiedName: String
  private val backingFile: PsiJavaFile

  init {
    val fileFactory = PsiFileFactory.getInstance(project)

    qualifiedName = destination.name.let { name ->
      val nameWithoutDirections = if (!name.startsWith('.')) name else "$modulePackage$name"
      "${nameWithoutDirections}Directions"
    }
    name = qualifiedName.substringAfterLast('.')

    // Create a dummy, backing file to represent this light class
    backingFile = fileFactory.createFileFromText("${name}.java", JavaFileType.INSTANCE,
                                                 "// This class is generated on-the-fly by the IDE.") as PsiJavaFile
    backingFile.packageName = (qualifiedName.substringBeforeLast('.'))
  }

  override fun getName() = name
  override fun getQualifiedName() = qualifiedName
  override fun getContainingFile() = backingFile
  override fun getContainingClass(): PsiClass? = null
  override fun isValid() = true
  override fun getNavigationElement(): PsiElement {
    val virtualFile = navigationResource.getSourceAsVirtualFile() ?: return super.getNavigationElement()
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return super.getNavigationElement()
    return psiFile
  }
}

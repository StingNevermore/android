/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.tools.idea.dagger.concepts

import com.android.tools.idea.dagger.index.DaggerConceptIndexer
import com.android.tools.idea.dagger.index.DaggerConceptIndexers
import com.android.tools.idea.dagger.index.IndexEntries
import com.android.tools.idea.dagger.index.IndexValue
import com.android.tools.idea.dagger.index.psiwrappers.DaggerIndexClassWrapper
import com.android.tools.idea.dagger.index.psiwrappers.DaggerIndexMethodWrapper
import com.android.tools.idea.dagger.localization.DaggerBundle
import com.android.tools.idea.kotlin.hasAnnotation
import com.google.wireless.android.sdk.stats.DaggerEditorEvent
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import java.io.DataInput
import java.io.DataOutput
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.idea.core.util.readString
import org.jetbrains.kotlin.idea.core.util.writeString
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction

/**
 * Represents a class used to create an instance of a type via an AssistedInject constructor.
 *
 * Example:
 * ```java
 *    @AssistedFactory
 *    interface DataServiceFactory {
 *      DataService create(Config config);
 *    }
 * ```
 *
 * This concept creates two types of index entries:
 * 1. The factory class.
 * 2. The parameters of its creation method.
 *
 * See also: [AssistedFactory](https://dagger.dev/api/latest/dagger/assisted/AssistedFactory.html)
 * and [AssistedInject](https://dagger.dev/api/latest/dagger/assisted/AssistedInject.html)
 */
object AssistedFactoryDaggerConcept : DaggerConcept {
  override val indexers =
    DaggerConceptIndexers(
      classIndexers = listOf(AssistedFactoryIndexer),
      methodIndexers = listOf(AssistedFactoryMethodIndexer)
    )
  override val indexValueReaders =
    listOf(AssistedFactoryClassIndexValue.Reader, AssistedFactoryMethodIndexValue.Reader)
  override val daggerElementIdentifiers =
    DaggerElementIdentifiers.of(
      AssistedFactoryClassIndexValue.identifiers,
      AssistedFactoryMethodIndexValue.identifiers
    )
}

private object AssistedFactoryIndexer : DaggerConceptIndexer<DaggerIndexClassWrapper> {
  override fun addIndexEntries(wrapper: DaggerIndexClassWrapper, indexEntries: IndexEntries) {
    if (!wrapper.getIsAnnotatedWith(DaggerAnnotations.ASSISTED_FACTORY)) return

    val classFqName = wrapper.getFqName()
    indexEntries.addIndexValue(classFqName, AssistedFactoryClassIndexValue(classFqName))
  }
}

private object AssistedFactoryMethodIndexer : DaggerConceptIndexer<DaggerIndexMethodWrapper> {
  override fun addIndexEntries(wrapper: DaggerIndexMethodWrapper, indexEntries: IndexEntries) {
    if (wrapper.getIsConstructor()) return

    val containingClass = wrapper.getContainingClass() ?: return
    if (!containingClass.getIsAnnotatedWith(DaggerAnnotations.ASSISTED_FACTORY)) return

    // If the method doesn't have a defined return type, then we don't need to index it - the
    // function is abstract so type inference can't be used to figure out the intended type, and
    // this won't actually build.
    val methodReturnTypeSimpleName = wrapper.getReturnType()?.getSimpleName() ?: return

    val classFqName = containingClass.getFqName()
    val methodSimpleName = wrapper.getSimpleName()

    indexEntries.addIndexValue(
      methodReturnTypeSimpleName,
      AssistedFactoryMethodIndexValue(classFqName, methodSimpleName)
    )
  }
}

@VisibleForTesting
internal data class AssistedFactoryClassIndexValue(val classFqName: String) : IndexValue() {
  override val dataType = Reader.supportedType

  override fun save(output: DataOutput) {
    output.writeString(classFqName)
  }

  object Reader : IndexValue.Reader {
    override val supportedType = DataType.ASSISTED_FACTORY_CLASS
    override fun read(input: DataInput) = AssistedFactoryClassIndexValue(input.readString())
  }

  companion object {
    private fun identify(psiElement: KtClassOrObject): DaggerElement? =
      if (psiElement.hasAnnotation(DaggerAnnotations.ASSISTED_FACTORY)) {
        ProviderDaggerElement(psiElement)
      } else {
        null
      }

    private fun identify(psiElement: PsiClass): DaggerElement? =
      if (psiElement.hasAnnotation(DaggerAnnotations.ASSISTED_FACTORY)) {
        ProviderDaggerElement(psiElement)
      } else {
        null
      }

    internal val identifiers =
      DaggerElementIdentifiers(
        ktClassIdentifiers = listOf(DaggerElementIdentifier(this::identify)),
        psiClassIdentifiers = listOf(DaggerElementIdentifier(this::identify))
      )
  }

  override fun getResolveCandidates(project: Project, scope: GlobalSearchScope): List<PsiElement> =
    JavaPsiFacade.getInstance(project).findClass(classFqName, scope)?.let { listOf(it) }
      ?: emptyList()

  override val daggerElementIdentifiers = identifiers
}

@VisibleForTesting
internal data class AssistedFactoryMethodIndexValue(
  val classFqName: String,
  val methodSimpleName: String,
) : IndexValue() {
  override val dataType = Reader.supportedType

  override fun save(output: DataOutput) {
    output.writeString(classFqName)
    output.writeString(methodSimpleName)
  }

  object Reader : IndexValue.Reader {
    override val supportedType = DataType.ASSISTED_FACTORY_METHOD
    override fun read(input: DataInput) =
      AssistedFactoryMethodIndexValue(input.readString(), input.readString())
  }

  companion object {
    private fun identify(psiElement: KtFunction): DaggerElement? {
      if (
        psiElement
          .parentOfType<KtClassOrObject>()
          ?.hasAnnotation(DaggerAnnotations.ASSISTED_FACTORY) != true
      )
        return null

      return AssistedFactoryMethodDaggerElement(psiElement)
    }

    private fun identify(psiElement: PsiMethod): DaggerElement? {
      if (
        psiElement.parentOfType<PsiClass>()?.hasAnnotation(DaggerAnnotations.ASSISTED_FACTORY) !=
          true
      )
        return null

      return AssistedFactoryMethodDaggerElement(psiElement)
    }

    internal val identifiers =
      DaggerElementIdentifiers(
        ktFunctionIdentifiers = listOf(DaggerElementIdentifier(this::identify)),
        psiMethodIdentifiers = listOf(DaggerElementIdentifier(this::identify))
      )
  }

  override fun getResolveCandidates(project: Project, scope: GlobalSearchScope): List<PsiElement> {
    val psiClass =
      JavaPsiFacade.getInstance(project).findClass(classFqName, scope) ?: return emptyList()
    return psiClass.methods.filter { it.name == methodSimpleName }
  }

  override val daggerElementIdentifiers = identifiers
}

internal data class AssistedFactoryMethodDaggerElement(
  override val psiElement: PsiElement,
  internal val returnedType: PsiType,
  internal val methodName: String?
) : DaggerElement() {

  internal constructor(
    psiElement: KtFunction
  ) : this(psiElement, psiElement.getReturnedPsiType(), psiElement.name)
  internal constructor(
    psiElement: PsiMethod
  ) : this(psiElement, psiElement.getReturnedPsiType(), psiElement.name)

  override val metricsElementType = DaggerEditorEvent.ElementType.ASSISTED_FACTORY_METHOD

  override fun getRelatedDaggerElements(): List<DaggerRelatedElement> {
    // The assisted inject constructor is always indexed with a fully-qualified name, so that's all
    // we have to look up.
    val indexKeys = listOf(returnedType.canonicalText)
    return getRelatedDaggerElementsFromIndex<AssistedInjectConstructorDaggerElement>(indexKeys)
      .map {
        DaggerRelatedElement(
          it,
          DaggerBundle.message("assisted.inject"),
          "navigate.to.assisted.inject",
          it.methodName
        )
      }
  }

  override fun filterResolveCandidate(resolveCandidate: DaggerElement) =
    resolveCandidate is AssistedInjectConstructorDaggerElement &&
      resolveCandidate.constructedType == this.returnedType
}

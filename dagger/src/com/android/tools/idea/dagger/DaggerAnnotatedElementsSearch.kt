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
package com.android.tools.idea.dagger

import com.android.annotations.concurrency.WorkerThread
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.impl.search.AnnotatedElementsSearcher
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.jetbrains.kotlin.analysis.api.KtAllowAnalysisOnEdt
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.lifetime.allowAnalysisOnEdt
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toPsiParameters
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.idea.base.searching.KotlinAnnotatedElementsSearcher
import org.jetbrains.kotlin.idea.stubindex.KotlinAnnotationsIndex
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType

/**
 * Searches for methods, fields and parameters of methods annotated with a given annotation, within
 * a given scope in Java and Kotlin files.
 *
 * It is a replacement for [AnnotatedElementsSearch] that uses [AnnotatedElementsSearcher] and
 * [KotlinAnnotatedElementsSearcher].
 *
 * [DaggerAnnotatedElementsSearch] uses [AnnotatedElementsSearcher] for JAVA.
 * [DaggerAnnotatedElementsSearch] works directly with [KotlinAnnotationsIndex] for Kotlin. The main
 * goal is to minimize calls of [KtElement.analyze]. [KtElement.analyze] is the main bottleneck in
 * [KotlinAnnotatedElementsSearcher]. It allows to speed up computation up to 30-40 times for large
 * projects.
 */
@Service
class DaggerAnnotatedElementsSearch(private val project: Project) {
  companion object {
    @JvmStatic
    fun getInstance(project: Project) =
      project.getService(DaggerAnnotatedElementsSearch::class.java)!!
  }

  fun getAnnotation(annotationFQN: String): PsiClass? {
    return JavaPsiFacade.getInstance(project)
      .findClass(annotationFQN, GlobalSearchScope.allScope(project))
  }

  private fun search(
    annotationFQN: String,
    scope: SearchScope,
    kotlinProcessor: (KtDeclaration) -> Unit,
    javaClassToSearch: Class<out PsiModifierListOwner>,
    javaProcessor: (PsiElement) -> Unit
  ) {
    val annotationClass = getAnnotation(annotationFQN) ?: return
    KotlinAnnotatedElementsSearcher.processAnnotatedMembers(annotationClass, scope) { declaration ->
      kotlinProcessor(declaration)
      true
    }

    AnnotatedElementsSearcher().execute(
      AnnotatedElementsSearch.Parameters(annotationClass, scope, javaClassToSearch)
    ) {
      javaProcessor(it)
      true
    }
  }

  fun searchClasses(annotationFQN: String, scope: SearchScope): Collection<PsiClass> {
    val result = mutableListOf<PsiClass>()

    val kotlinProcessor: (KtDeclaration) -> Unit =
      kotlinProcessor@{ declaration ->
        if (declaration is KtClassOrObject && declaration !is KtEnumEntry) {
          val psiClass = declaration.toLightClass() ?: return@kotlinProcessor
          result.add(psiClass)
        }
      }

    val javaProcessor: (PsiElement) -> Unit = {
      if (it is PsiClass) {
        result.add(it)
      }
    }

    search(annotationFQN, scope, kotlinProcessor, PsiClass::class.java, javaProcessor)
    return result
  }

  fun searchParameters(
    annotationFQN: String,
    scope: SearchScope,
    unboxedPsiType: PsiType
  ): Collection<PsiParameter> {
    val result = mutableListOf<PsiParameter>()

    val kotlinProcessor: (KtDeclaration) -> Unit =
      kotlinProcessor@{ declaration ->
        val parameter = declaration as? KtParameter ?: return@kotlinProcessor
        if (parameter.typeReference?.equalsToPsiType(unboxedPsiType) == true) {
          val param = parameter.toPsiParameters().firstOrNull() ?: return@kotlinProcessor
          result.add(param)
        }
      }

    val javaProcessor: (PsiElement) -> Unit = {
      if (it is PsiParameter && it.type.unboxed == unboxedPsiType) {
        result.add(it)
      }
    }

    search(annotationFQN, scope, kotlinProcessor, PsiParameter::class.java, javaProcessor)
    return result
  }

  fun searchMethods(
    annotationFQN: String,
    scope: SearchScope,
    unboxedPsiType: PsiType
  ): Collection<PsiMethod> {
    val result = mutableListOf<PsiMethod>()

    val kotlinProcessor: (KtDeclaration) -> Unit =
      kotlinProcessor@{ declaration ->
        val func = declaration as? KtNamedFunction ?: return@kotlinProcessor
        if (func.typeReference?.equalsToPsiType(unboxedPsiType) == true) {
          val method = LightClassUtil.getLightClassMethod(func) ?: return@kotlinProcessor
          result.add(method)
        }
      }

    val javaProcessor: (PsiElement) -> Unit = {
      if (it is PsiMethod && it.returnType?.unboxed == unboxedPsiType) {
        result.add(it)
      }
    }

    search(annotationFQN, scope, kotlinProcessor, PsiMethod::class.java, javaProcessor)
    return result
  }

  fun searchFields(
    annotationFQN: String,
    scope: SearchScope,
    unboxedPsiType: PsiType
  ): Collection<PsiField> {
    val result = mutableListOf<PsiField>()

    val kotlinProcessor: (KtDeclaration) -> Unit =
      kotlinProcessor@{ declaration ->
        val property = declaration as? KtProperty ?: return@kotlinProcessor
        if (property.typeReference?.equalsToPsiType(unboxedPsiType) == true) {
          val field = LightClassUtil.getLightClassBackingField(property) ?: return@kotlinProcessor
          result.add(field)
        }
      }

    val javaProcessor: (PsiElement) -> Unit = {
      if (it is PsiField && it.type.unboxed == unboxedPsiType) {
        result.add(it)
      }
    }

    search(annotationFQN, scope, kotlinProcessor, PsiField::class.java, javaProcessor)
    return result
  }

  fun searchParameterOfMethodAnnotatedWith(
    annotationFQN: String,
    scope: SearchScope,
    unboxedPsiType: PsiType
  ): Collection<PsiParameter> {
    val result = mutableListOf<PsiParameter>()

    val kotlinProcessor: (KtDeclaration) -> Unit =
      kotlinProcessor@{ declaration ->
        val ktFunction = declaration as? KtFunction ?: return@kotlinProcessor
        ktFunction.valueParameters.forEach {
          if (it.typeReference?.equalsToPsiType(unboxedPsiType) == true) {
            val param = it.toPsiParameters().firstOrNull() ?: return@kotlinProcessor
            result.add(param)
          }
        }
      }

    val javaProcessor: (PsiElement) -> Unit = { method ->
      if (method is PsiMethod) {
        result.addAll(method.parameterList.parameters.filter { it.type.unboxed == unboxedPsiType })
      }
    }

    search(annotationFQN, scope, kotlinProcessor, PsiMethod::class.java, javaProcessor)
    return result
  }
}

/**
 * Returns true if KtTypeReference corresponds to the given PsiType.
 *
 * This method aim to minimise number of calls for KtTypeReference.analyze(BodyResolveMode.PARTIAL).
 * KtTypeReference.analyze(BodyResolveMode.PARTIAL) is slow. It allows to speed up
 * DaggerAnnotatedElementsSearch up to 6 times.
 */
@WorkerThread
@OptIn(KtAllowAnalysisOnEdt::class)
private fun KtTypeReference.equalsToPsiType(unboxedPsiType: PsiType): Boolean {
  val notNullableTypeElement =
    if (typeElement is KtNullableType) (typeElement as KtNullableType).innerType else typeElement

  if (notNullableTypeElement !is KtUserType) return false
  val shortName = notNullableTypeElement.referencedName ?: return false
  if (unboxedPsiType is PsiClassType) {
    if (
      shortName != unboxedPsiType.name &&
        containingKtFile.findImportByAlias(shortName)?.importedFqName?.shortName()?.asString() !=
          unboxedPsiType.name
    ) {
      return false
    }
  }

  val kotlinPrimitiveTypeFqName =
    PrimitiveType.getByShortName(shortName)?.typeFqName
      ?: PrimitiveType.getByShortArrayName(shortName)?.arrayTypeFqName

  if (kotlinPrimitiveTypeFqName != null) {
    return kotlinPrimitiveTypeFqNameToPsiType[kotlinPrimitiveTypeFqName.asString()] ==
      unboxedPsiType
  }

  ProgressManager.checkCanceled()
  allowAnalysisOnEdt {
    analyze(this) {
      val ktType = this@equalsToPsiType.getKtType()
      val psiType = ktType.asPsiType(this@equalsToPsiType, allowErrorTypes = false)
      return psiType == unboxedPsiType
    }
  }
}

// Inspired by KtLightAnnotationsValues.kt#psiType
private val kotlinPrimitiveTypeFqNameToPsiType: Map<String, PsiType> =
  mapOf(
    "kotlin.Int" to PsiTypes.intType(),
    "kotlin.Long" to PsiTypes.longType(),
    "kotlin.Short" to PsiTypes.shortType(),
    "kotlin.Boolean" to PsiTypes.booleanType(),
    "kotlin.Byte" to PsiTypes.byteType(),
    "kotlin.Char" to PsiTypes.charType(),
    "kotlin.Double" to PsiTypes.doubleType(),
    "kotlin.Float" to PsiTypes.floatType(),
    "kotlin.IntArray" to PsiTypes.intType().createArrayType(),
    "kotlin.LongArray" to PsiTypes.longType().createArrayType(),
    "kotlin.ShortArray" to PsiTypes.shortType().createArrayType(),
    "kotlin.BooleanArray" to PsiTypes.booleanType().createArrayType(),
    "kotlin.ByteArray" to PsiTypes.byteType().createArrayType(),
    "kotlin.CharArray" to PsiTypes.charType().createArrayType(),
    "kotlin.DoubleArray" to PsiTypes.doubleType().createArrayType(),
    "kotlin.FloatArray" to PsiTypes.floatType().createArrayType()
  )
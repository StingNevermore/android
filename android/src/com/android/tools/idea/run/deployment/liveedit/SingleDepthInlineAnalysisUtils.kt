/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.tools.idea.run.deployment.liveedit
import com.android.tools.idea.projectsystem.getModuleSystem
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.codegen.inline.InlineCache
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.base.utils.fqname.getKotlinFqName
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import java.nio.file.Files
import java.nio.file.Paths
import java.util.HashSet
import java.util.LinkedHashSet
import kotlin.io.path.exists
import kotlin.system.measureTimeMillis

/**
 * This is a cache of class name (fully qualify name such as java/lang/String) to a inlinable source (bytecode on disk or in memory)
 *
 * We should consider doing these optimizations in the future.
 *
 * 1) Use a LSU based approach to only keep bytecode of build output that is very likely to be used in memory
 * 2) Save Live Edit generated bytecode to disk if they are no longer being used frequently.
 */
typealias SourceInlineCandidateCache = LinkedHashMap<String, SourceInlineCandidate>

/**
 * This class represent a Kotlin source file and the location (memory or file) of the compiled bytecode that can be used to be injected
 * into the inline cache of the code generation process.
 */
data class SourceInlineCandidate (val sourceFile: KtFile, val className : String) {
  companion object {
    val LOGGER = Logger.getInstance(SourceInlineCandidate.javaClass)
  }

  var bytecode : ByteArray? = null
  /**
   * Return true if we can populate the KtFile's inline cache entry of the code generation process.
   * IE: Can we have the bytecode available either in memory or on disk somehow.
   */
  inline fun canFillInlineCache() = bytecode != null

  /**
   * Fill the bytecode cache with the .class content from the last build.
   */
  fun fetchByteCodeFromBuildIfNeeded() {

    // Don't read the disk if we already done it once.
    if (canFillInlineCache()) {
      return
    }

    // Fetch the output of the class file from the module's output directory.
    var vFile = sourceFile.getModuleSystem()?.getClassFileFinderForSourceFile(sourceFile.virtualFile)?.findClassFile(className)

    if (vFile == null) {
      LOGGER.warn("Unable to local $className in the build system.")
    }

    bytecode = vFile?.let {
      val file = Paths.get(it.path)

      if (!file.exists()) {
        LOGGER.warn("Build output $file NOT found")
        return
      }

      return@let Files.readAllBytes(file)
    }
  }

  /**
   * Replaces the current cache with new bytecode. This should be invoked when Live Edit finished compiling a KtFile and the on disk
   * build output is no longer valid.
   */
  fun setByteCode(bytecode: ByteArray) {
    this.bytecode = bytecode
  }

  /**
   * Given the inline cache object of a code generation state, fill this KtFile's entry with the bytecode if they exist.
   */
  fun fillInlineCache(inlineCache : InlineCache) : Boolean {
    if (canFillInlineCache()) {
      inlineCache.classBytes.put(className, bytecode!!)
      return true
    }
    return false
  }
}

/**
 * Given a KtFile (likely a file that has just been modified and ready for Live Edit updates), compute a list of all SourceInlineCandidate
 * that would be needed to the KtFile to successfully compile.
 *
 * In other words, given a A.kt. Compute the set of (B_0.kt, B_1.kt, ..... B_n.kt) where each B_x.kt is a source file from the current
 * project (JAR file dependency ignored) which contains at least one inline function that A.kt references. Each B_n.kt in the result is
 * represented by a SourceInlineCandidate object which the compilation can use to successfully compile A.kt.
 *
 * Note that this function is NOT recursive and only compute inline functions of one depth level. The returned KtFiles
 * themselves can also reference inline functions from another source file that are not part of the return set.
 */
fun analyzeSingleDepthInlinedFunctions(
  file: KtFile,
  bindingContext: BindingContext,
  cache: SourceInlineCandidateCache): Set<SourceInlineCandidate> {
  val referencedClasses = LinkedHashSet<SourceInlineCandidate>()
  analyzeElementWithOneLevelInline(file, bindingContext, referencedClasses, cache)
  return referencedClasses
}

// This is mostly org.jetbrains.kotlin.idea.core.util.inlineAnalysisUtils but non-recursive and fitted with Live edit specific abstraction
private fun analyzeElementWithOneLevelInline(
  element: KtFile,
  bindingContext: BindingContext,
  requestedClasses: LinkedHashSet<SourceInlineCandidate>,
  cache: SourceInlineCandidateCache){
  val project = element.project
  val declarationsWithBody = HashSet<KtDeclarationWithBody>()
  element.accept(object : KtTreeVisitorVoid() {
    override fun visitExpression(expression: KtExpression) {
      super.visitExpression(expression)
      val call = bindingContext.get(BindingContext.CALL, expression) ?: return
      val resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, call)
      checkResolveCall(resolvedCall)
    }
    override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
      super.visitDestructuringDeclaration(destructuringDeclaration)
      for (entry in destructuringDeclaration.entries) {
        val resolvedCall = bindingContext.get(BindingContext.COMPONENT_RESOLVED_CALL, entry)
        checkResolveCall(resolvedCall)
      }
    }
    override fun visitForExpression(expression: KtForExpression) {
      super.visitForExpression(expression)
      checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, expression.loopRange))
      checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, expression.loopRange))
      checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, expression.loopRange))
    }
    private fun checkResolveCall(resolvedCall: ResolvedCall<*>?) {
      if (resolvedCall == null) return
      val descriptor = resolvedCall.resultingDescriptor
      if (descriptor is DeserializedSimpleFunctionDescriptor) return
      isAdditionalResolveNeededForDescriptor(descriptor)
      if (descriptor is PropertyDescriptor) {
        for (accessor in descriptor.accessors) {
          isAdditionalResolveNeededForDescriptor(accessor)
        }
      }
    }
    private fun isAdditionalResolveNeededForDescriptor(descriptor: CallableDescriptor) {
      if (!(InlineUtil.isInline(descriptor))) {
        return
      }
      val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
      if (declaration != null && declaration is KtDeclarationWithBody) {
        declarationsWithBody.add(declaration)
        return
      }
    }
  })
  for (declaration in declarationsWithBody) {
    declaration.javaClass
    val containingClass = declaration.containingClass()
    // Note that any external (outside this source file) function that is getting referenced must have a user defined name.
    // Otherwise, it will be impossible to reference.
    // There are two cases
    // 1) It is a top level function that does not belong to a class.
    if (containingClass == null) {
      val name = declaration.containingKtFile.javaFileFacadeFqName.toString().replace(".", "/")
      val file = declaration.containingKtFile
      if (element != file) {
        requestedClasses.add(cache.computeIfAbsent(name) {
          SourceInlineCandidate(file, it)
        })
      }
    } else {
      val name = containingClass.getKotlinFqName().toString().replace(".", "/")
      val file = declaration.containingKtFile
      if (element != file) {
        requestedClasses.add(cache.computeIfAbsent(name) {
          SourceInlineCandidate(file, it)
        })
      }
    }
  }
}

/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.tools.idea.rendering.classloading.loaders

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.android.uipreview.ClassModificationTimestamp
import org.jetbrains.android.uipreview.INTERNAL_PACKAGE
import org.jetbrains.annotations.TestOnly
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

private fun String.isSystemPrefix(): Boolean = startsWith("java.") ||
                                               startsWith("javax.") ||
                                               startsWith("kotlin.") ||
                                               startsWith(INTERNAL_PACKAGE) ||
                                               startsWith("sun.")

/**
 * A [DelegatingClassLoader.Loader] that loads the classes from a given IntelliJ [Module].
 * It relies in the given [findClassVirtualFileImpl] to find the [VirtualFile] mapping to a given FQCN.
 */
class ProjectSystemClassLoader(private val findClassVirtualFileImpl: (String) -> VirtualFile?) : DelegatingClassLoader.Loader {
  /**
   * Map that contains the mapping from the class FQCN to the [VirtualFile] that contains the `.class` contents and the
   * [ClassModificationTimestamp] representing the loading timestamp.
   */
  private val virtualFileCache = ConcurrentHashMap<String, Pair<VirtualFile, ClassModificationTimestamp>>()

  private val nonProjectFiles: MutableSet<String> = Collections.newSetFromMap(ContainerUtil.createConcurrentSoftMap())

  /**
   * [Sequence] of all the [VirtualFile]s and their associated [ClassModificationTimestamp] that have been loaded
   * by this [ProjectSystemClassLoader].
   */
  val loadedVirtualFiles: Sequence<Triple<String, VirtualFile, ClassModificationTimestamp>>
    get() = virtualFileCache
      .asSequence()
      .filter { it.value.first.isValid }
      .map {
        Triple(it.key, it.value.first, it.value.second)
      }

  /**
   * Finds the [VirtualFile] for the `.class` associated to the given [fqcn].
   */
  fun findClassVirtualFile(fqcn: String): VirtualFile? {
    // Avoid loading a few well known system prefixes for the project class loader and also classes that have failed before.
    if (fqcn.isSystemPrefix() || nonProjectFiles.contains(fqcn)) {
      return null
    }
    val cachedVirtualFile = virtualFileCache[fqcn]

    if (cachedVirtualFile?.first?.isValid == true) return cachedVirtualFile.first
    val vFile = findClassVirtualFileImpl(fqcn)

    if (vFile != null) {
      virtualFileCache[fqcn] = Pair(vFile, ClassModificationTimestamp.fromVirtualFile(vFile))
    }
    else {
      nonProjectFiles.add(fqcn)
    }

    return vFile
  }

  /**
   * Clears all the internal caches. Next `find` call will reload the information directly from the VFS.
   */
  fun invalidateCaches() {
    virtualFileCache.clear()
    nonProjectFiles.clear()
  }

  override fun loadClass(fqcn: String): ByteArray? = try {
    findClassVirtualFile(fqcn)?.contentsToByteArray()
  } catch (_: Throwable) {
    null
  }

  /**
   * Injects the given [virtualFile] with the passed [fqcn] so it looks like loaded from the project. Only for testing.
   */
  @TestOnly
  fun injectClassFile(fqcn: String, virtualFile: VirtualFile) {
    virtualFileCache[fqcn] = Pair(virtualFile, ClassModificationTimestamp.fromVirtualFile(virtualFile))
  }
}
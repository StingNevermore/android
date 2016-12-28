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
package org.jetbrains.android.compiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.GuardedBy;
import java.util.*;

import static org.jetbrains.android.compiler.AndroidCompileUtil.generate;

public class ModuleSourceAutogenerating implements Disposable {
  private static final Key<ModuleSourceAutogenerating> KEY = Key.create("org.jetbrains.android.compiler.SourceAutogenerating");

  private AndroidFacet myFacet;

  private final Set<AndroidAutogeneratorMode> myDirtyModes = EnumSet.noneOf(AndroidAutogeneratorMode.class);

  @GuardedBy("myAutogeneratedFiles")
  private final Map<AndroidAutogeneratorMode, Set<String>> myAutogeneratedFiles = new HashMap<>();

  @Nullable
  public static ModuleSourceAutogenerating get(@NotNull AndroidFacet facet) {
    return facet.getUserData(KEY);
  }

  public static void initialize(@NotNull AndroidFacet facet) {
    if (facet.requiresAndroidModel()) {
      return;
    }

    ModuleSourceAutogenerating autogenerating = new ModuleSourceAutogenerating(facet);
    facet.putUserData(KEY, autogenerating);

    Module module = facet.getModule();
    Project project = module.getProject();

    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      if (facet.isDisposed() || module.isDisposed() || project.isDisposed()) {
        return;
      }

      generate(module, AndroidAutogeneratorMode.AAPT);
      generate(module, AndroidAutogeneratorMode.AIDL);
      generate(module, AndroidAutogeneratorMode.RENDERSCRIPT);
      generate(module, AndroidAutogeneratorMode.BUILDCONFIG);

    });
  }

  private ModuleSourceAutogenerating(@NotNull AndroidFacet facet) {
    myFacet = facet;
    Disposer.register(facet, this);
  }

  public boolean isGeneratedFileRemoved(@NotNull AndroidAutogeneratorMode mode) {
    synchronized (myAutogeneratedFiles) {
      Set<String> filePaths = myAutogeneratedFiles.get(mode);

      if (filePaths != null) {
        for (String path : filePaths) {
          VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);

          if (file == null) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public void clearAutogeneratedFiles(@NotNull AndroidAutogeneratorMode mode) {
    synchronized (myAutogeneratedFiles) {
      Set<String> set = myAutogeneratedFiles.get(mode);
      if (set != null) {
        set.clear();
      }
    }
  }

  public void markFileAutogenerated(@NotNull AndroidAutogeneratorMode mode, @NotNull VirtualFile file) {
    synchronized (myAutogeneratedFiles) {
      Set<String> set = myAutogeneratedFiles.get(mode);

      if (set == null) {
        set = new HashSet<>();
        myAutogeneratedFiles.put(mode, set);
      }
      set.add(file.getPath());
    }
  }

  @NotNull
  public Set<String> getAutogeneratedFiles(@NotNull AndroidAutogeneratorMode mode) {
    synchronized (myAutogeneratedFiles) {
      Set<String> set = myAutogeneratedFiles.get(mode);
      return set != null ? new HashSet<>(set) : Collections.emptySet();
    }
  }

  public void scheduleSourceRegenerating(@NotNull AndroidAutogeneratorMode mode) {
    synchronized (myDirtyModes) {
      myDirtyModes.add(mode);
    }
  }

  public boolean cleanRegeneratingState(@NotNull AndroidAutogeneratorMode mode) {
    synchronized (myDirtyModes) {
      return myDirtyModes.remove(mode);
    }
  }

  public void resetRegeneratingState() {
    synchronized (myDirtyModes) {
      Collections.addAll(myDirtyModes, AndroidAutogeneratorMode.values());
    }
  }

  @Override
  public void dispose() {
    myFacet.putUserData(KEY, null);
    myFacet = null;
  }
}

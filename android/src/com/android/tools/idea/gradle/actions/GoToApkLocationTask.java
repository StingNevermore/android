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
package com.android.tools.idea.gradle.actions;

import static com.intellij.notification.NotificationType.ERROR;
import static com.intellij.notification.NotificationType.INFORMATION;

import com.android.build.OutputFile;
import com.android.builder.model.AndroidArtifactOutput;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.InstantAppProjectBuildOutput;
import com.android.builder.model.InstantAppVariantBuildOutput;
import com.android.builder.model.ProjectBuildOutput;
import com.android.builder.model.VariantBuildOutput;
import com.android.tools.idea.apk.viewer.ApkFileSystem;
import com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker;
import com.android.tools.idea.gradle.project.build.invoker.GradleInvocationResult;
import com.android.tools.idea.gradle.project.model.AndroidModuleModel;
import com.android.tools.idea.gradle.run.OutputBuildAction;
import com.android.tools.idea.gradle.run.PostBuildModel;
import com.android.tools.idea.project.AndroidNotification;
import com.android.tools.idea.project.hyperlink.NotificationHyperlink;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import com.intellij.ide.actions.ShowFilePathAction;
import com.intellij.notification.EventLog;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.event.HyperlinkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

public class GoToApkLocationTask implements GradleBuildInvoker.AfterGradleInvocationTask {
  public static final String ANALYZE = "analyze:";
  public static final String MODULE = "module:";
  @NotNull private final Project myProject;
  @NotNull private final Collection<Module> myModules;
  @NotNull private final String myNotificationTitle;
  @NotNull private final List<String> myBuildVariants;
  @NotNull private Map<String, File> myBuildsAndApkPaths;

  public GoToApkLocationTask(@NotNull Collection<Module> modules, @NotNull String notificationTitle, @NotNull List<String> buildVariants) {
    this(modules.iterator().next().getProject(), modules, notificationTitle, buildVariants);
  }

  @VisibleForTesting
  GoToApkLocationTask(@NotNull Project project,
                      @NotNull Collection<Module> modules,
                      @NotNull String notificationTitle,
                      @NotNull List<String> buildVariants) {
    myProject = project;
    myModules = modules;
    myNotificationTitle = notificationTitle;
    myBuildVariants = buildVariants;
    myBuildsAndApkPaths = new HashMap<>();
  }

  @TestOnly
  void setMyBuildsAndApkPaths(@NotNull Map<String, File> buildsAndApkPaths) {
    myBuildsAndApkPaths = buildsAndApkPaths;
  }

  @TestOnly
  @NotNull
  Map<String, File> getMyBuildsAndApkPaths() {
    return myBuildsAndApkPaths;
  }

  @Override
  public void execute(@NotNull GradleInvocationResult result) {
    boolean isSigned = !myBuildVariants.isEmpty();
    try {
      getBuildsAndPaths(result.getModel(), myBuildVariants);
      if (isSigned) {
        String moduleName = Iterators.getOnlyElement(myModules.iterator()).getName();
        showNotification(result, myBuildVariants, moduleName);
      }
      else {
        List<String> moduleNames = new ArrayList<>();
        for (Map.Entry<String, File> moduleAndApkPath : myBuildsAndApkPaths.entrySet()) {
          String moduleName = moduleAndApkPath.getKey();
          File apkPath = moduleAndApkPath.getValue();
          if (apkPath != null) {
            moduleNames.add(moduleName);
          }
        }
        Collections.sort(moduleNames);
        showNotification(result, moduleNames, null);
      }
    }
    finally {
      // See https://code.google.com/p/android/issues/detail?id=195369
      GradleBuildInvoker.getInstance(myProject).remove(this);
    }
  }

  private void showNotification(@NotNull GradleInvocationResult result,
                                @NotNull List<String> modulesOrBuildVariants,
                                @Nullable String moduleName) {
    AndroidNotification notification = AndroidNotification.getInstance(myProject);
    boolean isSigned = moduleName != null;

    if (result.isBuildSuccessful()) {
      StringBuilder builder = new StringBuilder();
      int count = modulesOrBuildVariants.size();
      builder.append("APK(s) generated successfully for ");
      if (isSigned) {
        builder.append("module '").append(moduleName).append("' with ").append(count)
          .append(count == 1 ? " build" : " builds");
      }
      else {
        builder.append(count).append(count == 1 ? " module" : " modules");
      }

      builder.append(":<br/>");
      if (isShowFilePathActionSupported()) {
        for (int i = 0; i < count; i++) {
          String moduleOrBuildVariant = modulesOrBuildVariants.get(i);
          if (isSigned) {
            builder.append("Build '");
          }
          else {
            builder.append("Module '");
          }
          builder.append(moduleOrBuildVariant).append("': ");
          builder.append("<a href=\"").append(MODULE).append(moduleOrBuildVariant).append("\">locate</a> or ");
          builder.append("<a href=\"").append(ANALYZE).append(moduleOrBuildVariant).append("\">analyze</a> the APK.");
          if (i < count - 1) {
            builder.append("<br/>");
          }
        }
        String text = builder.toString();
        notification
          .showBalloon(myNotificationTitle, text, INFORMATION, new OpenFolderNotificationListener(myBuildsAndApkPaths, myProject));
      }
      else {
        // Platform does not support showing the location of a file.
        // Display file paths in the 'Log' view, since they could be too long to show in a balloon notification.
        for (int i = 0; i < count; i++) {
          String moduleOrBuildVariant = modulesOrBuildVariants.get(i);
          builder.append(" - ").append(moduleOrBuildVariant).append(": ");
          builder.append(myBuildsAndApkPaths.get(moduleOrBuildVariant).getPath());
          if (i < count - 1) {
            builder.append("\n");
          }
        }
        StringBuilder balloonBuilder = new StringBuilder();
        balloonBuilder.append("APK(s) generated successfully for ");
        if (isSigned) {
          balloonBuilder.append("module '").append(moduleName).append("' with ").append(count)
            .append(count == 1 ? " build" : " builds");
        }
        else {
          balloonBuilder.append(count).append(count == 1 ? " module" : " modules");
        }
        notification.showBalloon(myNotificationTitle, balloonBuilder.toString(), INFORMATION, new OpenEventLogHyperlink());
        notification.addLogEvent(myNotificationTitle, builder.toString(), INFORMATION);
      }
    }
    else if (result.isBuildCancelled()) {
      notification.showBalloon(myNotificationTitle, "Build cancelled.", INFORMATION);
    }
    else {
      String msg = "Errors while building APK. You can find the errors in the 'Messages' view.";
      notification.showBalloon(myNotificationTitle, msg, ERROR);
    }
  }

  /**
   * Generates a map from module to the location (either the apk itself if only one or to the folder if multiples).
   */

  @VisibleForTesting
  void getBuildsAndPaths(@Nullable Object model, @NotNull List<String> buildVariants) {
    boolean isSigned = !buildVariants.isEmpty();
    PostBuildModel postBuildModel = null;

    if (model instanceof OutputBuildAction.PostBuildProjectModels) {
      postBuildModel = new PostBuildModel((OutputBuildAction.PostBuildProjectModels)model);
    }

    for (Module module : myModules) {
      AndroidModuleModel androidModel = AndroidModuleModel.get(module);
      if (androidModel == null) {
        continue;
      }
      if (isSigned) {
        assert myModules.size() == 1;

        for (String buildVariant : buildVariants) {
          updateBuildsAndPaths(androidModel, postBuildModel, module, buildVariant, isSigned);
        }
      }
      else {
        String myBuildVariant = androidModel.getSelectedVariant().getName();
        updateBuildsAndPaths(androidModel, postBuildModel, module, myBuildVariant, isSigned);
      }
    }
  }

  private void updateBuildsAndPaths(@NotNull AndroidModuleModel androidModel,
                                    @Nullable PostBuildModel postBuildModel,
                                    @NotNull Module module,
                                    @NotNull String buildVariant,
                                    boolean isSigned) {
    File outputFolderOrApk = null;
    if (postBuildModel != null) {
      outputFolderOrApk = tryToGetOutputPostBuild(androidModel, module, postBuildModel, buildVariant);
      if (outputFolderOrApk == null) {
        outputFolderOrApk = tryToGetOutputPostBuildInstantApp(androidModel, module, postBuildModel, buildVariant);
      }
    }
    if (outputFolderOrApk == null) {
      assert !isSigned;
      outputFolderOrApk = tryToGetOutputPreBuild(androidModel);
    }
    myBuildsAndApkPaths.put(isSigned ? buildVariant : module.getName(), outputFolderOrApk);
  }

  @Nullable
  private static File tryToGetOutputPostBuild(@NotNull AndroidModuleModel androidModel,
                                              @NotNull Module module,
                                              @NotNull PostBuildModel postBuildModel,
                                              @NotNull String buildVariant) {
    if (androidModel.getAndroidProject().getProjectType() == AndroidProject.PROJECT_TYPE_APP) {
      ProjectBuildOutput projectBuildOutput = postBuildModel.findProjectBuildOutput(module);
      if (projectBuildOutput != null) {
        for (VariantBuildOutput variantBuildOutput : projectBuildOutput.getVariantsBuildOutput()) {
          if (variantBuildOutput.getName().equals(buildVariant)) {
            Collection<OutputFile> outputs = variantBuildOutput.getOutputs();
            File outputFolderOrApk;
            if (outputs.size() == 1) {
              outputFolderOrApk = outputs.iterator().next().getOutputFile();
            }
            else {
              outputFolderOrApk = outputs.iterator().next().getOutputFile().getParentFile();
            }
            return outputFolderOrApk;
          }
        }
      }
    }

    return null;
  }

  @Nullable
  private static File tryToGetOutputPostBuildInstantApp(@NotNull AndroidModuleModel androidModel,
                                                        @NotNull Module module,
                                                        @NotNull PostBuildModel postBuildModel,
                                                        @NotNull String buildVariant) {
    if (androidModel.getAndroidProject().getProjectType() == AndroidProject.PROJECT_TYPE_INSTANTAPP) {
      InstantAppProjectBuildOutput instantAppProjectBuildOutput = postBuildModel.findInstantAppProjectBuildOutput(module);
      if (instantAppProjectBuildOutput != null) {
        for (InstantAppVariantBuildOutput variantBuildOutput : instantAppProjectBuildOutput.getInstantAppVariantsBuildOutput()) {
          if (variantBuildOutput.getName().equals(buildVariant)) {
            return variantBuildOutput.getOutput().getOutputFile();
          }
        }
      }
    }

    return null;
  }

  @Nullable
  private static File tryToGetOutputPreBuild(@NotNull AndroidModuleModel androidModel) {
    Collection<AndroidArtifactOutput> outputs = androidModel.getMainArtifact().getOutputs();
    if (outputs.size() == 1) {
      return outputs.iterator().next().getOutputFile();
    }
    return outputs.iterator().next().getOutputFile().getParentFile();
  }

  @VisibleForTesting
  boolean isShowFilePathActionSupported() {
    return ShowFilePathAction.isSupported();
  }

  @VisibleForTesting
  static class OpenFolderNotificationListener extends NotificationListener.Adapter {
    @NotNull private final Map<String, File> myApkPathsPerModule;
    @NotNull private final Project myProject;

    OpenFolderNotificationListener(@NotNull Map<String, File> apkPathsPerModule, @NotNull Project project) {
      myApkPathsPerModule = apkPathsPerModule;
      myProject = project;
    }

    @Override
    protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
      String description = e.getDescription();
      if (description.startsWith(ANALYZE)) {
        File apkPath = myApkPathsPerModule.get(description.substring(ANALYZE.length()));
        VirtualFile apk;
        if (apkPath.isFile()) {
          apk = LocalFileSystem.getInstance().findFileByIoFile(apkPath);
        }
        else {
          FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
            .withDescription("Select APK to analyze")
            .withFileFilter(file -> ApkFileSystem.EXTENSIONS.contains(file.getExtension()));
          apk = FileChooser.chooseFile(descriptor, myProject, LocalFileSystem.getInstance().findFileByIoFile(apkPath));
        }
        if (apk != null) {
          OpenFileDescriptor fd = new OpenFileDescriptor(myProject, apk);
          FileEditorManager.getInstance(myProject).openEditor(fd, true);
        }
      }
      else if (description.startsWith(MODULE)) {
        File apkPath = myApkPathsPerModule.get(description.substring(MODULE.length()));
        assert apkPath != null;
        if (apkPath.isFile()) {
          apkPath = apkPath.getParentFile();
        }
        ShowFilePathAction.openDirectory(apkPath);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      OpenFolderNotificationListener listener = (OpenFolderNotificationListener)o;
      return Objects.equals(myApkPathsPerModule, listener.myApkPathsPerModule);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myApkPathsPerModule);
    }
  }

  @VisibleForTesting
  static class OpenEventLogHyperlink extends NotificationHyperlink {
    OpenEventLogHyperlink() {
      super("open.event.log", "Show APK path(s) in the 'Event Log' view");
    }

    @Override
    protected void execute(@NotNull Project project) {
      ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(EventLog.LOG_TOOL_WINDOW_ID);
      if (tw != null) {
        tw.activate(null, false);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      // There are no fields to compare.
      return true;
    }
  }
}

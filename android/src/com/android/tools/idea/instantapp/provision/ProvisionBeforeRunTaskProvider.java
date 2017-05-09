/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.instantapp.provision;

import com.android.annotations.NonNull;
import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.NullOutputReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.instantapp.provision.ProvisionException;
import com.android.instantapp.provision.ProvisionListener;
import com.android.instantapp.provision.ProvisionRunner;
import com.android.tools.idea.instantapp.InstantApps;
import com.android.tools.idea.run.AndroidRunConfigContext;
import com.android.tools.idea.run.AndroidRunConfigurationBase;
import com.android.tools.idea.run.DeviceFutures;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import icons.AndroidIcons;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.android.tools.idea.instantapp.InstantApps.getInstantAppSdk;
import static com.android.tools.idea.instantapp.InstantApps.isInstantAppSdkEnabled;

/**
 * Provides a {@link ProvisionBeforeRunTask} which is executed before an Instant App is run.
 * This provisions the device.
 */
public class ProvisionBeforeRunTaskProvider extends BeforeRunTaskProvider<ProvisionBeforeRunTaskProvider.ProvisionBeforeRunTask> {
  @NotNull public static final Key<ProvisionBeforeRunTask> ID = Key.create("com.android.instantApps.provision.BeforeRunTask");
  @NotNull private static final String TASK_NAME = "Instant App Provision";

  @NotNull
  @Override
  public Key<ProvisionBeforeRunTask> getId() {
    return ID;
  }

  @NotNull
  @Override
  public String getName() {
    return TASK_NAME;
  }

  @NotNull
  @Override
  public String getDescription(ProvisionBeforeRunTask task) {
    return TASK_NAME;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return AndroidIcons.Android;
  }

  @Nullable
  @Override
  public Icon getTaskIcon(ProvisionBeforeRunTask task) {
    return AndroidIcons.Android;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Nullable
  @Override
  public ProvisionBeforeRunTask createTask(RunConfiguration runConfiguration) {
    // This method is called when a new run configuration is created, and in that moment we don't know if it will be an Instant App or not,
    // so we create the task anyway and later, when running it, we check if it's an instant app context to provision the device or not.
    // This method is also called when reading from persistent data (first an empty task is created and after it's configured).
    if (runConfiguration instanceof AndroidRunConfigurationBase && isInstantAppSdkEnabled()) {
      ProvisionBeforeRunTask task = new ProvisionBeforeRunTask();
      task.setEnabled(true);
      return task;
    }
    return null;
  }

  @Override
  public boolean configureTask(RunConfiguration runConfiguration, ProvisionBeforeRunTask task) {
    ProvisionEditTaskDialog dialog = new ProvisionEditTaskDialog(runConfiguration.getProject(), task.isClearCache());
    if (!dialog.showAndGet()) {
      return false;
    }
    task.setClearCache(dialog.isClearCache());
    return true;
  }

  @Override
  public boolean canExecuteTask(RunConfiguration configuration, ProvisionBeforeRunTask task) {
    return isInstantAppContext((AndroidRunConfigurationBase)configuration);
  }

  @Override
  public boolean executeTask(DataContext context, RunConfiguration configuration, ExecutionEnvironment env, ProvisionBeforeRunTask task) {
    if (!isInstantAppContext((AndroidRunConfigurationBase)configuration)) {
      // If the run configuration is not running an Instant App, there's no need to provision the device. Return early.
      return true;
    }

    AndroidRunConfigContext runConfigContext = env.getCopyableUserData(AndroidRunConfigContext.KEY);
    DeviceFutures deviceFutures = runConfigContext == null ? null : runConfigContext.getTargetDevices();

    if (deviceFutures == null) {
      return false;
    }

    ProgressManager progressManager = ProgressManager.getInstance();

    CountDownLatch countDownLatch = new CountDownLatch(1);
    AtomicBoolean result = new AtomicBoolean(true);

    progressManager.run(new Task.Backgroundable(configuration.getProject(), getDescription(task), true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        AtomicBoolean tryAgain = new AtomicBoolean(true);
        Set<IDevice> succeededProvisioned = new HashSet<>();
        try {
          ProvisionRunner provisionRunner = new ProvisionRunner(getInstantAppSdk(), new ProvisionListener() {
            @Override
            public void printMessage(@NonNull String message) {
              indicator.setText2(message);
            }

            @Override
            public void logMessage(@NonNull String message, ProvisionException e) {
              if (e == null) {
                getLogger().info(message);
              }
              else {
                getLogger().warn(message, e);
              }
            }

            @Override
            public void setProgress(double fraction) {
              indicator.setFraction(fraction);
            }

            @Override
            public boolean isCancelled() {
              return indicator.isCanceled();
            }
          });
          indicator.setIndeterminate(true);
          indicator.setText("Provisioning device");

          while (tryAgain.get()) {
            try {
              for (ListenableFuture<IDevice> deviceListenableFuture : deviceFutures.get()) {
                IDevice device = waitForDevice(deviceListenableFuture, indicator);
                if (device == null) {
                  result.set(false);
                  return;
                }

                if (!succeededProvisioned.contains(device)) {
                  indicator.setIndeterminate(false);
                  provisionRunner.runProvision(device);

                  if (task.isClearCache()) {
                    try {
                      device.executeShellCommand("pm clear com.google.android.instantapps.supervisor", NullOutputReceiver.getReceiver());
                      device.executeShellCommand("pm clear com.google.android.instantapps.devman", NullOutputReceiver.getReceiver());
                    }
                    catch (com.android.ddmlib.TimeoutException | AdbCommandRejectedException | ShellCommandUnresponsiveException | IOException e) {
                      getLogger().warn("Error while clearing supervisor or devman cache on device", e);
                    }
                  }
                  succeededProvisioned.add(device);
                }
              }
              tryAgain.set(false);
            }
            catch (ProvisionException e) {
              ApplicationManager.getApplication().invokeAndWait(() -> {
                int choice = Messages
                  .showYesNoDialog("Provision failed with message: " + e.getMessage() + ". Do you want to retry?", "Instant Apps", null);
                if (choice == Messages.OK) {
                  provisionRunner.clearCache();
                }
                else {
                  tryAgain.set(false);
                  // If there was an error while provisioning, we stop running the RunConfiguration
                  result.set(false);
                }
              });
            }
          }
        }
        catch (FileNotFoundException | ProvisionException e) {
          getLogger().warn("Error while provisioning devices", e);
          result.set(false);
        }
        finally {
          // In case an unknown exception (e.g. runtime) is thrown, the countDownLatch unblocks the other thread so the user doesn't expect the timeout
          countDownLatch.countDown();
        }
      }
    });

    try {
      return countDownLatch.await(deviceFutures.get().size() * 300, TimeUnit.SECONDS) && result.get();
    }
    catch (InterruptedException e) {
      getLogger().error("Background thread interrupted", e);
      return false;
    }
  }

  @VisibleForTesting
  boolean isInstantAppContext(@NotNull AndroidRunConfigurationBase runConfiguration) {
    // This method returning false does not guarantee it's not an Instant App context, since recently created run configurations don't have
    // a module selected yet, and then we can't check if the module is instant app.
    Module module = runConfiguration.getConfigurationModule().getModule();
    return isInstantAppSdkEnabled() && module != null && InstantApps.isInstantAppApplicationModule(module);
  }

  @Nullable
  private static IDevice waitForDevice(@NotNull ListenableFuture<IDevice> deviceFuture, ProgressIndicator indicator) {
    while (true) {
      try {
        return deviceFuture.get(1, TimeUnit.SECONDS);
      }
      catch (InterruptedException | ExecutionException e) {
        return null;
      }
      catch (TimeoutException ignored) {
      }

      if (indicator.isCanceled()) {
        return null;
      }
    }
  }

  private static Logger getLogger() {
    return Logger.getInstance(ProvisionBeforeRunTaskProvider.class);
  }

  public static class ProvisionBeforeRunTask extends BeforeRunTask<ProvisionBeforeRunTask> {
    private boolean myClearCache;

    public ProvisionBeforeRunTask() {
      super(ID);
      myClearCache = false;
    }

    public void setClearCache(boolean clearCache) {
      myClearCache = clearCache;
    }

    public boolean isClearCache() {
      return myClearCache;
    }

    @Override
    public void writeExternal(Element element) {
      super.writeExternal(element);
      element.setAttribute("clearCache", String.valueOf(myClearCache));
    }

    @Override
    public void readExternal(Element element) {
      super.readExternal(element);
      myClearCache = Boolean.valueOf(element.getAttributeValue("clearCache")).booleanValue();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      ProvisionBeforeRunTask that = (ProvisionBeforeRunTask)o;

      if (myClearCache != that.myClearCache) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (myClearCache ? 1 : 0);
      return result;
    }
  }
}

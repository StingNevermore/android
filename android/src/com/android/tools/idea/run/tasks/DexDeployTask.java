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
package com.android.tools.idea.run.tasks;

import com.android.ddmlib.IDevice;
import com.android.tools.fd.client.UpdateMode;
import com.android.tools.idea.fd.InstantRunBuildInfo;
import com.android.tools.idea.fd.InstantRunManager;
import com.android.tools.idea.fd.InstantRunPushFailedException;
import com.android.tools.idea.run.ConsolePrinter;
import com.android.tools.idea.run.util.LaunchStatus;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;

public class DexDeployTask implements LaunchTask {
  @NotNull private final AndroidFacet myFacet;
  @NotNull private final InstantRunBuildInfo myBuildInfo;

  public DexDeployTask(@NotNull AndroidFacet facet, @NotNull InstantRunBuildInfo buildInfo) {
    myFacet = facet;
    myBuildInfo = buildInfo;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Installing restart patches";
  }

  @Override
  public int getDuration() {
    return LaunchTaskDurations.DEPLOY_APK;
  }

  @Override
  public boolean perform(@NotNull IDevice device, @NotNull LaunchStatus launchStatus, @NotNull ConsolePrinter printer) {
      try {
        InstantRunManager.displayVerifierStatus(myFacet, myBuildInfo);

        InstantRunManager manager = InstantRunManager.get(myFacet.getModule().getProject());
        manager.pushArtifacts(device, myFacet, UpdateMode.HOT_SWAP, myBuildInfo);
        // Note that the above method will update the build id on the device
        // and the InstalledPatchCache, so we don't have to do it again.

        return true;
      }
      catch (InstantRunPushFailedException e) {
        launchStatus.terminateLaunch("Error installing cold swap patches: " + e);
        return false;
      }
  }
}

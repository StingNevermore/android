/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.run.deployment;

import com.android.annotations.VisibleForTesting;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

final class ConnectionTimeService {
  private final Map<String, Instant> myKeyToConnectionTimeMap;
  private final Clock myClock;

  @SuppressWarnings("unused")
  private ConnectionTimeService() {
    this(Clock.systemDefaultZone());
  }

  @VisibleForTesting
  ConnectionTimeService(@NotNull Clock clock) {
    myKeyToConnectionTimeMap = new HashMap<>();
    myClock = clock;
  }

  @NotNull
  Instant get(@NotNull String key) {
    return myKeyToConnectionTimeMap.computeIfAbsent(key, k -> myClock.instant());
  }

  void retainAll(@NotNull Collection<String> keys) {
    myKeyToConnectionTimeMap.keySet().retainAll(keys);
  }
}

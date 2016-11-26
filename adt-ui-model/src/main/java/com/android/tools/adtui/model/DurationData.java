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
package com.android.tools.adtui.model;

/**
 * An object that models data with duration
 */
public interface DurationData {
  /**
   * Represents data that have an infinite/unspecified duration (e.g. unfinished events).
   * TODO replace unspecified duration with Long.MAX_VALUE on perfd side so we don't have to deal with this.
   */
  int UNSPECIFIED_DURATION = -1;

  long getDuration();
}

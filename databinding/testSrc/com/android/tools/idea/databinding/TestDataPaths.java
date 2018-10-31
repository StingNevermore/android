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
package com.android.tools.idea.databinding;

import com.android.testutils.TestUtils;

/**
 * Constants for databinding test data locations.
 */
public final class TestDataPaths {
  // TODO(b/117947069): Change to "tools/adt/idea/databinding/testData
  public static final String TEST_DATA_ROOT = TestUtils.getWorkspaceFile("tools/adt/idea/android/testData").getPath();

  public static final String PROJECT_WITH_DATA_BINDING = "projects/projectWithDataBinding";
  public static final String PROJECT_WITH_DATA_BINDING_ANDROID_X = "projects/projectWithDataBindingAndroidX";
  public static final String PROJECT_WITH_DATA_BINDING_AND_SIMPLE_LIB = "projects/projectWithDataBindingAndSimpleLib";
}

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
package com.android.tools.idea.gradle.project.model.ide.android;

import com.android.builder.model.SourceProviderContainer;
import com.android.tools.idea.gradle.project.model.ide.android.stubs.SourceProviderContainerStub;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

import static com.android.tools.idea.gradle.project.model.ide.android.IdeModelTestUtils.*;
import static com.android.tools.idea.gradle.project.model.ide.android.Serialization.deserialize;
import static com.android.tools.idea.gradle.project.model.ide.android.Serialization.serialize;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link IdeSourceProviderContainer}.
 */
public class IdeSourceProviderContainerTest {
  private ModelCache myModelCache;

  @Before
  public void setUp() throws Exception {
    myModelCache = new ModelCache();
  }

  @Test
  public void serializable() {
    assertThat(IdeSourceProviderContainer.class).isAssignableTo(Serializable.class);
  }

  @Test
  public void serialization() throws Exception {
    IdeSourceProviderContainer classField = new IdeSourceProviderContainer(new SourceProviderContainerStub(), myModelCache);
    byte[] bytes = serialize(classField);
    Object o = deserialize(bytes);
    assertEquals(classField, o);
  }

  @Test
  public void constructor() throws Throwable {
    SourceProviderContainer original = new SourceProviderContainerStub();
    IdeSourceProviderContainer copy = new IdeSourceProviderContainer(original, myModelCache);
    assertEqualsOrSimilar(original, copy);
    verifyUsageOfImmutableCollections(copy);
  }

  @Test
  public void equalsAndHashCode() {
    createEqualsVerifier(IdeSourceProviderContainer.class).verify();
  }
}
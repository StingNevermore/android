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
package com.android.tools.idea.gradle.dsl.model.ext;

import com.android.tools.idea.gradle.dsl.api.GradleBuildModel;
import com.android.tools.idea.gradle.dsl.api.ext.GradlePropertyModel;
import com.android.tools.idea.gradle.dsl.api.ext.PropertyType;
import com.android.tools.idea.gradle.dsl.api.util.TypeReference;
import com.android.tools.idea.gradle.dsl.parser.elements.GradleDslElement;
import com.android.tools.idea.gradle.dsl.parser.elements.GradlePropertiesDslElement;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.android.tools.idea.gradle.dsl.api.ext.GradlePropertyModel.ValueType.NONE;

/**
 * Represents a non-existing property in a {@link GradleBuildModel}. It allows other models to return PropertyModels that can have
 * there values set. Once an {@link EmptyPropertyModel} has its value set it created a {@link GradlePropertyModelImpl} with the
 * resulting {@link GradleDslElement} all subsequent calls are delegated to this new model.
 */
public class EmptyPropertyModel implements GradlePropertyModel {
  @Nullable private GradlePropertyModelImpl myRealElement;

  @NotNull private final GradlePropertiesDslElement myPropertyHolder;
  @NotNull private final PropertyType myPropertyType;
  @NotNull private final String myPropertyName;


  public EmptyPropertyModel(@NotNull GradlePropertiesDslElement propertyHolder, @NotNull PropertyType type, @NotNull String propertyName) {
    myPropertyHolder = propertyHolder;
    myPropertyType = type;
    myPropertyName = propertyName;
  }

  @NotNull
  @Override
  public ValueType getValueType() {
    if (myRealElement != null) {
      return myRealElement.getValueType();
    }
    return NONE;
  }

  @NotNull
  @Override
  public PropertyType getPropertyType() {
    if (myRealElement != null) {
      return myRealElement.getPropertyType();
    }
    return myPropertyType;
  }

  @Nullable
  @Override
  public <T> T getValue(@NotNull TypeReference<T> typeReference) {
    if (myRealElement != null) {
      return myRealElement.getValue(typeReference);
    }
    return null;
  }

  @Nullable
  @Override
  public <T> T getUnresolvedValue(@NotNull TypeReference<T> typeReference) {
    if (myRealElement != null) {
      return myRealElement.getUnresolvedValue(typeReference);
    }
    return null;
  }

  @NotNull
  @Override
  public List<GradlePropertyModel> getDependencies() {
    if (myRealElement != null) {
      return myRealElement.getDependencies();
    }
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public String getName() {
    if (myRealElement != null) {
      return myRealElement.getName();
    }
    return myPropertyName;
  }

  @NotNull
  @Override
  public String getFullyQualifiedName() {
    if (myRealElement != null) {
      return myRealElement.getFullyQualifiedName();
    }
    return myPropertyHolder.getQualifiedName() + "." + myPropertyName;
  }

  @NotNull
  @Override
  public VirtualFile getGradleFile() {
    if (myRealElement != null) {
      return myRealElement.getGradleFile();
    }
    return myPropertyHolder.getDslFile().getFile();
  }

  @Override
  public void setValue(@NotNull Object value) {
    if (!(value instanceof Integer || value instanceof String || value instanceof Boolean)) {
      throw new UnsupportedOperationException("Only setting basic types is currently supported");
    }
    GradleDslElement element = myPropertyHolder.setNewLiteral(myPropertyName, value);
    // Set the property type of the created DslElement
    element.setElementType(myPropertyType);
    myRealElement = new GradlePropertyModelImpl(element);
  }

  @Override
  public String toString() {
    if (myRealElement != null) {
      return myRealElement.toString();
    }

    return String.format("[Name: %1$s, Property Type: %2$s, Holder: %3$s, Value: Empty]@%4$s",
                         myPropertyName, myPropertyType, myPropertyHolder, Integer.toHexString(hashCode()));
  }
}

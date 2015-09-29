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
 *
 * THIS FILE WAS GENERATED BY codergen. EDIT WITH CARE.
 */
package com.android.tools.idea.editors.gfxtrace.service;

import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.BinaryClass;
import com.android.tools.rpclib.binary.BinaryID;
import com.android.tools.rpclib.binary.BinaryObject;
import com.android.tools.rpclib.binary.Decoder;
import com.android.tools.rpclib.binary.Encoder;
import com.android.tools.rpclib.binary.Namespace;

import java.io.IOException;

public final class Resources implements BinaryObject {
  //<<<Start:Java.ClassBody:1>>>
  private ResourceInfo[] myTextures;

  // Constructs a default-initialized {@link Resources}.
  public Resources() {}


  public ResourceInfo[] getTextures() {
    return myTextures;
  }

  public Resources setTextures(ResourceInfo[] v) {
    myTextures = v;
    return this;
  }

  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }

  private static final byte[] IDBytes = {125, 114, -81, 111, -101, -125, -107, 57, -66, -6, -33, 83, -103, -5, -119, 109, 120, 120, 17, 104, };
  public static final BinaryID ID = new BinaryID(IDBytes);

  static {
    Namespace.register(ID, Klass.INSTANCE);
  }
  public static void register() {}
  //<<<End:Java.ClassBody:1>>>
  public enum Klass implements BinaryClass {
    //<<<Start:Java.KlassBody:2>>>
    INSTANCE;

    @Override @NotNull
    public BinaryID id() { return ID; }

    @Override @NotNull
    public BinaryObject create() { return new Resources(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
      Resources o = (Resources)obj;
      e.uint32(o.myTextures.length);
      for (int i = 0; i < o.myTextures.length; i++) {
        e.value(o.myTextures[i]);
      }
    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
      Resources o = (Resources)obj;
      o.myTextures = new ResourceInfo[d.uint32()];
      for (int i = 0; i <o.myTextures.length; i++) {
        o.myTextures[i] = new ResourceInfo();
        d.value(o.myTextures[i]);
      }
    }
    //<<<End:Java.KlassBody:2>>>
  }
}

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
package com.android.tools.idea.editors.gfxtrace.service.path;

import com.android.tools.rpclib.binary.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class ArrayIndexPath extends Path {
  @Override
  public StringBuilder stringPath(StringBuilder builder) {
    return myArray.stringPath(builder).append("[").append(myIndex).append("]");
  }

  //<<<Start:Java.ClassBody:1>>>
  private Path myArray;
  private long myIndex;

  // Constructs a default-initialized {@link ArrayIndexPath}.
  public ArrayIndexPath() {}


  public Path getArray() {
    return myArray;
  }

  public ArrayIndexPath setArray(Path v) {
    myArray = v;
    return this;
  }

  public long getIndex() {
    return myIndex;
  }

  public ArrayIndexPath setIndex(long v) {
    myIndex = v;
    return this;
  }

  @Override @NotNull
  public BinaryClass klass() { return Klass.INSTANCE; }

  private static final byte[] IDBytes = {117, -71, 36, -27, -102, 15, 59, -37, -18, -54, -37, 54, -93, 79, 30, 113, -111, 28, -53, 82, };
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
    public BinaryObject create() { return new ArrayIndexPath(); }

    @Override
    public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
      ArrayIndexPath o = (ArrayIndexPath)obj;
      e.object(o.myArray.unwrap());
      e.uint64(o.myIndex);
    }

    @Override
    public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
      ArrayIndexPath o = (ArrayIndexPath)obj;
      o.myArray = Path.wrap(d.object());
      o.myIndex = d.uint64();
    }
    //<<<End:Java.KlassBody:2>>>
  }
}

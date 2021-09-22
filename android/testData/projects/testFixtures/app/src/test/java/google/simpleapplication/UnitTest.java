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
package google.simpleapplication;

import org.junit.Assert;
import org.junit.Test;

import java.lang.Deprecated;

/**
 * A unit test to be executed on the local vm.
 */
public class UnitTest {
  @Test
  public void passingTest() throws Exception {
    Assert.assertEquals(2, 1 + 1);
  }

  @Test
  public void failingTest() throws Exception {
    Assert.assertEquals(5, 2 + 2);
  }

  @Test
  public void testFixtures() throws Exception {
    new com.example.lib.testFixtures.LibInterfaceTester("test").test("test");
  }

  @Test
  public void publishedTestFixtures() throws Exception {
    new com.example.publishedlib.testFixtures.LibInterfaceTester("test").test("test");
  }

  @Test
  public void publishedJavaTestFixtures() throws Exception {
    new com.example.javalib.testFixtures.JavaLibInterfaceTester(12).test(12);
  }
}
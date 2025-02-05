// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.android.tools.idea.uibuilder.handlers;

import static com.android.testutils.AsyncTestUtils.waitForCondition;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.emptyList;

import com.android.tools.idea.uibuilder.api.ViewHandler;
import com.android.tools.idea.uibuilder.api.XmlType;
import com.intellij.psi.PsiClass;
import java.util.concurrent.TimeUnit;
import org.intellij.lang.annotations.Language;
import org.jetbrains.android.AndroidTestCase;
import org.jetbrains.android.dom.converters.PackageClassConverter;
import org.jetbrains.annotations.NotNull;

public class CustomViewHandlerTest extends AndroidTestCase {
  private static final String MY_TEXT_VIEW = "p1.p2.MyTextView";
  private static final String MY_CHECKBOX = "p1.p2.MyTextView.MyCheckBox";

  private String myTagName;
  private String myClassName;
  private ViewHandler myDefaultHandler;

  @Language("JAVA")
  String innerClass =
    "package p1.p2;\n" +
    "\n" +
    "import android.content.Context;\n" +
    "import android.widget.CheckBox;\n" +
    "import android.widget.TextView;\n" +
    "\n" +
    "public class MyTextView extends TextView {\n" +
    "    public MyTextView(Context context) {\n" +
    "        super(context);\n" +
    "    }\n" +
    "    public static class MyCheckBox extends CheckBox {\n" +
    "        public MyCheckBox(Context context) {\n" +
    "            super(context);\n" +
    "        }\n" +
    "    }\n" +
    "}";

  private void setUpMyClasses(@NotNull String tagName) throws Exception {
    myFixture.addClass(innerClass);
    PsiClass psiClass = myFixture.findClass(tagName);
    myTagName = tagName;
    myClassName = PackageClassConverter.getQualifiedName(psiClass);
    ViewHandlerManager manager = ViewHandlerManager.get(getProject());
    boolean[] wasUpdated = new boolean[1];
    Runnable updated = () -> { wasUpdated[0] = true; };
    myDefaultHandler = manager.getHandlerOrDefault(myTagName, updated);
    if (myDefaultHandler == ViewHandlerManager.TEMP) {
      waitForCondition(10, TimeUnit.SECONDS, () -> wasUpdated[0]);
      myDefaultHandler = manager.getHandlerOrDefault(myTagName, updated);
    }
  }

  public void testGetXml() throws Exception {
    setUpMyClasses(MY_TEXT_VIEW);
    ViewHandler handler = new CustomViewHandler(myDefaultHandler, null, myTagName, myClassName,
                                                null, null, "", null, emptyList());
    @Language("XML")
    String expected =
      "<p1.p2.MyTextView\n" +
      "    android:text=\"p1.p2.MyTextView\"\n" +
      "    android:layout_width=\"wrap_content\"\n" +
      "    android:layout_height=\"wrap_content\" />\n";

    assertThat(handler.getXml(myTagName, XmlType.COMPONENT_CREATION)).isEqualTo(expected);
  }

  public void testGetSpecifiedXml() throws Exception {
    setUpMyClasses(MY_CHECKBOX);
    ViewHandler handler = new CustomViewHandler(myDefaultHandler, null, myTagName, myClassName,
                                                "<myxml/>", null, "", null, emptyList());
    assertThat(handler.getXml(myTagName, XmlType.COMPONENT_CREATION)).isEqualTo("<myxml/>");
  }

  public void testGetXmlOfInnerClass() throws Exception {
    setUpMyClasses(MY_CHECKBOX);
    ViewHandler handler = new CustomViewHandler(myDefaultHandler, null, myTagName, myClassName,
                                                null, null, "", null, emptyList());

    @Language("XML")
    String expected =
      "<view\n" +
      "    class=\"p1.p2.MyTextView$MyCheckBox\"\n" +
      "    android:layout_width=\"wrap_content\"\n" +
      "    android:layout_height=\"wrap_content\" />\n";

    assertThat(handler.getXml(myTagName, XmlType.COMPONENT_CREATION)).isEqualTo(expected);
  }
}

import os
import random
import re
import shutil
import subprocess
import sys
import time
import unittest
import zipfile

MAX_RETRIES_TO_FIND_DISPLAY = 20


def extract_file(zip_file, info, extract_dir):
  """Extracts a file preserving file attributes."""
  out_path = zip_file.extract(info.filename, path=extract_dir)
  attr = info.external_attr >> 16
  if attr:
    os.chmod(out_path, attr)


def start_xvfb():
  xvfb_env = {"TEST_TMPDIR": os.getenv("TEST_TMPDIR")}
  retry = MAX_RETRIES_TO_FIND_DISPLAY
  while retry > 0:
    display = ":%d" % random.randint(100, 65535)
    process = subprocess.Popen(["tools/vendor/google/testing/display/launch_xvfb.sh", display, os.getcwd(), "1280x1024x24"], env=xvfb_env)
    time.sleep(1)
    if process.poll():
      # xvfb exited, try a different display
      retry = retry - 1
    else:
      print("Launched xvfb on display %s. Waiting 5s for it to initialize" % display)
      time.sleep(5)
      return process, display
  return None, None


class StartUpTest(unittest.TestCase):
  """Launches Studio with xvfb setup, and monitors the idea.log to verify that expected plugins are loaded."""

  def test_startup(self):

    work_dir = os.getenv("TEST_TMPDIR")
    undeclared_outputs = os.getenv("TEST_UNDECLARED_OUTPUTS_DIR")

    zip_path = os.path.join("tools/adt/idea/studio/android-studio.linux.zip")
    with zipfile.ZipFile(zip_path) as zip_file:
      for info in zip_file.infolist():
        extract_file(zip_file, info, work_dir)

    xvfb, display = start_xvfb()
    if not display:
      self.fail("Failed to initialize xvfb")

    os.mkdir(work_dir + "/config")
    vmoptions_file = "%s/studio.vmoptions" % work_dir
    with open(vmoptions_file, "w") as vmopts:
      vmopts.writelines([
          "-Didea.config.path=%s/config\n" % work_dir,
          "-Didea.plugins.path=%s/config/plugins\n" % work_dir,
          "-Didea.system.path=%s/system\n" % work_dir,
          "-Didea.log.path=%s/system/log\n" % work_dir,
          "-Ddisable.android.analytics.consent.dialog.for.test=true\n"
          "-Duser.home=%s/home\n" % work_dir
      ])

    env = {
        "STUDIO_VM_OPTIONS": vmoptions_file,
        "XDG_DATA_HOME": "%s/data" % work_dir,
        "SHELL": os.getenv("SHELL"),
        "DISPLAY": display,
    }

    bin_path = work_dir + "/android-studio/bin/studio.sh"
    with open(os.path.join(undeclared_outputs, "stdout.txt"), "w") as fout, open(os.path.join(undeclared_outputs, "stderr.txt"), "w") as ferr:
      process = subprocess.Popen([bin_path], stdout=fout, stderr=ferr, env=env)

      logpath = os.path.join(work_dir, "system/log/idea.log")

      # Wait for idea.log to be created
      retry = 60  # 30s / 0.5s
      while retry > 0:
        if os.path.exists(logpath):
          break
        time.sleep(0.5)
      if retry == 0:
        self.fail("Cannot find idea.log generated by the IDE")

      # Wait for PluginManager to load plugins
      marker = "PluginManager - Loaded bundled plugins:"
      with open(logpath, "r") as idealog:
        retry = 240  # 2 minutes / 0.5s
        while retry > 0:
          line = idealog.readline()
          if line:
            ix = line.find(marker)
            if ix != -1:
              plugins = line[60 + len(marker):].split(",")
              descs = [re.match(" *(.*) \\(.*\\)", pl).group(1) for pl in plugins]
              descs.sort()
              self.assertEqual([
                  "Android",
                  "Android APK Support",
                  "Android NDK Support",
                  "App Links Assistant",
                  "C/C++ Language Support",
                  "CIDR Base",
                  "ChangeReminder",
                  "Clangd support",
                  "Code Coverage for Java",
                  "Compose",
                  "Configuration Script",
                  "Copyright",
                  "EditorConfig",
                  "Emoji Picker",
                  "Firebase App Indexing",
                  "Firebase Services",
                  "Firebase Testing",
                  "Git",
                  "GitHub",
                  "Google Cloud Tools Core",
                  "Google Cloud Tools For Android Studio",
                  "Google Developers Samples",
                  "Google Login",
                  "Gradle",
                  "Gradle-Java",
                  "Groovy",
                  "IDEA CORE",
                  "IntelliLang",
                  "JUnit",
                  "Java",
                  "Java Bytecode Decompiler",
                  "Java IDE Customization",
                  "Java Internationalization",
                  "Java Stream Debugger",
                  "JetBrains Repository Search",
                  "JetBrains maven model api classes",
                  "Kotlin",
                  "Layoutlib",
                  "Layoutlib Legacy",
                  "Lombok",
                  "Machine Learning Code Completion",
                  "Machine Learning Code Completion Models",
                  "Mercurial",
                  "Next File Prediction",
                  "Properties",
                  "Resource Bundle Editor",
                  "Settings Repository",
                  "Shell Script",
                  "Smali Support",
                  "Subversion",
                  "Task Management",
                  "Terminal",
                  "Test Recorder",
                  "TestNG",
                  "TextMate Bundles",
                  "WebP Support",
                  "YAML",
                  "com.intellij.platform.images"
              ], descs)
              break
          else:
            retry = retry - 1
            time.sleep(0.5)
        if retry == 0:
          self.fail("Cannot find marker \"%s\" in idea.log" % marker)

      process.kill()

    shutil.copy(logpath, os.path.join(undeclared_outputs, "idea.log"))
    xvfb.kill()

if __name__ == "__main__":
  unittest.main()

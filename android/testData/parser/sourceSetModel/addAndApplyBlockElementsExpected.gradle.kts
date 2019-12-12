android {
  sourceSets {
    getByName("main") {
      aidl {
        srcDirs("aidlSource")
      }
      assets {
        srcDirs("assetsSource")
      }
      java {
        srcDirs("javaSource")
      }
      jni {
        srcDirs("jniSource")
      }
      jniLibs {
        srcDirs("jniLibsSource")
      }
      manifest {
        srcFile("manifestSource.xml")
      }
      renderscript {
        srcDirs("renderscriptSource")
      }
      res {
        srcDirs("resSource")
      }
      resources {
        srcDirs("resourcesSource")
      }
    }
  }
}

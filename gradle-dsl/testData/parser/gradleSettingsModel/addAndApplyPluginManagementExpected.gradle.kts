pluginManagement {
  plugins {
    id("com.android.application") version "7.0.0"
  }
  repositories {
    google()
  }
}
dependencyResolutionManagement {
  repositories {
    jcenter()
  }
}
rootProject.name = "My Application"
include(":app")

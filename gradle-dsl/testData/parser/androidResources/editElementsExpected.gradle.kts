android {
  androidResources {
    additionalParameters += listOf("abcd", "xyz")
    cruncherEnabled = true
    cruncherProcesses = 3
    failOnMissingConfigEntry = false
    ignoreAssetsPattern = "mnop"
    noCompress += listOf("a", "c")
  }
}

// build root project
lazy val root = Project("plugins", file(".")) dependsOn developedPlugin

// depends on the awesomeOS project
lazy val developedPlugin = file("..").getAbsoluteFile.toURI
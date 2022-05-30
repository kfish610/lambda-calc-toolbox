val scala3Version = "3.1.2"

lazy val root = project
  .in(file("."))
  .enablePlugins(
    ScalaJSPlugin,
    ScalaJSBundlerPlugin,
    ScalablyTypedConverterPlugin
  )
  .settings(
    name := "lambda-calc-toolbox",
    version := "1.0.0",
    scalaVersion := scala3Version,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    Compile / fastOptJS / artifactPath := baseDirectory.value / "out" / "extension.js",
    Compile / fullOptJS / artifactPath := baseDirectory.value / "out" / "extension.js",
    Compile / npmDependencies ++= Seq(
      "@types/vscode" -> "1.67.0",
      "vscode-textmate" -> "7.0.1",
      "vscode-oniguruma" -> "1.6.2"
    )
  )

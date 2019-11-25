
enablePlugins(JavaAppPackaging)
enablePlugins(LinuxPlugin)
enablePlugins(DebianPlugin)
enablePlugins(RpmPlugin)
enablePlugins(WindowsPlugin)

name := "HDLtoIMG"

version := "1.0"

maintainer := "Andrea Bellandi <andrea.bellandi@desy.de>"

packageSummary := "HDL to image generator"

packageDescription := """ A generator of the graphical representation of component interface. VHDL and SpinalHDL are supported. The default output file format is SVG"""

wixProductId := ""
wixProductUpgradeId := ""

//scalaVersion := "2.12.10"
scalaVersion := "2.11.12"

EclipseKeys.withSource := true

//addCompilerPlugin("org.scala-lang.plugins" % "scala-continuations-plugin_2.12.2" % "1.0.3")

val circeVersion = "0.11.1"

libraryDependencies ++= Seq(
  "com.github.spinalhdl" % "spinalhdl-core_2.11" % "1.3.6",
  "com.github.spinalhdl" % "spinalhdl-lib_2.11" % "1.3.6",
  "de.erichseifert.vectorgraphics2d" % "VectorGraphics2D" % "0.13",
  "com.github.scopt" %% "scopt" % "4.0.0-RC2",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.typelevel" %% "cats-core" % "2.0.0"
)

//scalacOptions += "-P:continuations:enable"
scalacOptions += "-Ypartial-unification"

fork := true


debianPackageDependencies := Seq("default-jre-headless")

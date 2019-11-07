name := "SpinalHDL-Creative"

version := "1.0"

//scalaVersion := "2.12.10"
scalaVersion := "2.11.12"

EclipseKeys.withSource := true

//addCompilerPlugin("org.scala-lang.plugins" % "scala-continuations-plugin_2.12.2" % "1.0.3")

libraryDependencies ++= Seq(
  "com.github.spinalhdl" % "spinalhdl-core_2.11" % "1.3.6",
  "com.github.spinalhdl" % "spinalhdl-lib_2.11" % "1.3.6",
  "de.erichseifert.vectorgraphics2d" % "VectorGraphics2D" % "0.13"
)

//scalacOptions += "-P:continuations:enable"

fork := true

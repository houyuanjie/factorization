enablePlugins(ScalaJSPlugin)
scalaJSUseMainModuleInitializer := true

name := "factorization"
version := "1.0"

scalaVersion := "2.13.7"

libraryDependencies += "io.monix"     %%% "monix"       % "3.4.0"
libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.1.0"

scalacOptions += "-Xsource:3"
scalacOptions += "-deprecation"
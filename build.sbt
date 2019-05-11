name := "formula-AI"

version := "1.0"

scalaVersion := "2.12.8"

scalacOptions ++= Seq("-feature", "-Xlint", "-Ywarn-numeric-widen", "-Ypartial-unification", "-language:higherKinds")
javaOptions in run += """-Djava.library.path=lib"""""

classpathTypes += "maven-plugin"

val tensorflowVersion = "1.13.1"

libraryDependencies ++= Seq(
  "org.scalafx" %% "scalafx" % "8.0.102-R11",
  "net.java.jinput" % "jinput" % "2.0.6",
  "net.java.dev.jna" % "jna-platform" % "5.2.0",
  "org.tensorflow" % "tensorflow" % tensorflowVersion,
  "org.tensorflow" % "libtensorflow" % tensorflowVersion, // enable gpu
  "org.tensorflow" % "libtensorflow_jni_gpu" % tensorflowVersion, // enable gpu
  "org.tensorflow" % "proto" % tensorflowVersion,
  "com.github.pureconfig" %% "pureconfig" % "0.9.2",
  "org.slf4s" %% "slf4s-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

val copyWin64DLL = taskKey[Unit]("copy native dll to lib folder")

copyWin64DLL := {
  val base = ivyPaths.value.ivyHome.getOrElse(Path.userHome / ".ivy2")

  val jarBase = base / "cache" / "net.java.jinput" / "jinput-platform" / "jars"
  val jars = jarBase * "*windows.jar"

  val filter = new SimpleFilter(_.endsWith("dll"))
  jars.get.foreach { location =>
    IO.unzip(location, file("lib"), filter)
  }
}

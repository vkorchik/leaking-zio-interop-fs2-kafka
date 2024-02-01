
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "leaky-zio-interop-fs2-kafka"
  )
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.4.8",
      "com.github.fd4s" %% "fs2-kafka" % "3.0.0",
      "dev.zio" %% "zio" % "2.0.16",
      "dev.zio" %% "zio-interop-cats" % "23.1.0.0",
      "com.dimafeng" %% "testcontainers-scala-kafka" % "0.41.2",
      "org.testcontainers" % "testcontainers" % "1.19.4",
    )
  )

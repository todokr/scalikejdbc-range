import Dependencies._

ThisBuild / scalaVersion     := "2.13.2"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

val scalikeJdbcVersion = "3.5.0"

lazy val root = (project in file("."))
  .settings(
    name := "scalikejdbc-range",
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % "42.1.4",
      "org.scalikejdbc" %% "scalikejdbc" % scalikeJdbcVersion,
      "org.scalikejdbc" %% "scalikejdbc-config" % scalikeJdbcVersion,
      "org.scalikejdbc" %% "scalikejdbc-config"  % scalikeJdbcVersion,
      "ch.qos.logback"  %  "logback-classic"   % "1.2.3",
      scalaTest % Test
    )
  )
enablePlugins(ScalikejdbcPlugin)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

name := "htwplus"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaCore,
  javaWs,
  javaJpa,
  cache,
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.hibernate" % "hibernate-entitymanager" % "4.3.10.Final",
  "org.hibernate" % "hibernate-core" % "4.3.10.Final",
  "org.hibernate" % "hibernate-commons-annotations" % "3.2.0.Final",
  "org.apache.directory.api" % "api-all" % "1.0.0-M20",
  "commons-io" % "commons-io" % "2.4",
  "com.typesafe.play" %% "play-mailer" % "3.0.1",
  "org.elasticsearch" % "elasticsearch" % "1.4.1",
  "eu.medsea.mimeutil" % "mime-util" % "2.1.3",
  "org.imgscalr" % "imgscalr-lib" % "4.2",
  "joda-time" % "joda-time" % "2.7",
  "org.mockito" % "mockito-all" % "1.10.19",
  "org.easytesting" % "fest-assert" % "1.4"
)

libraryDependencies += evolutions
routesGenerator := InjectedRoutesGenerator
lazy val root = (project in file(".")).enablePlugins(PlayJava)

name := "htwplus"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaCore,
  javaWs,
  javaJpa,
  cache,
  guice,
  "org.postgresql" % "postgresql" % "9.4.1212",
  "org.hibernate" % "hibernate-entitymanager" % "5.2.9.Final",
  "org.hibernate" % "hibernate-core" % "5.2.9.Final",
  "org.hibernate.common" % "hibernate-commons-annotations" % "5.0.1.Final",
  "com.typesafe.play" % "play-mailer_2.11" % "5.0.0", // EmailService
  "org.elasticsearch.client" % "transport" % "5.3.2",
  "org.elasticsearch" % "elasticsearch" % "5.3.2",
  "org.imgscalr" % "imgscalr-lib" % "4.2", // ImageService
  "org.apache.directory.api" % "api-all" % "1.0.0-RC2", // LDAP
  "org.apache.logging.log4j" % "log4j-api" % "2.8.2",
  "org.apache.logging.log4j" % "log4j-core" % "2.8.2"

)

libraryDependencies += evolutions
libraryDependencies += javaForms

PlayKeys.externalizeResources := false

lazy val root = project.in(file(".")).enablePlugins(PlayJava).disablePlugins(PlayFilters)

scalaVersion := "2.11.8"

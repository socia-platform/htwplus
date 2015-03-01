name := "htwplus"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaCore,
  javaWs,
  "com.typesafe.play" %% "play-jdbc" % "2.2.3" exclude("com.jolbox", "bonecp"),
  "com.jolbox" % "bonecp" % "0.8.0.RELEASE",
  javaJpa.exclude("org.hibernate.javax.persistence", "hibernate-jpa-2.0-api"),
  cache,
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "org.hibernate" % "hibernate-entitymanager" % "4.3.6.Final",
  "org.hibernate" % "hibernate-core" % "4.3.6.Final",
  "org.hibernate" % "hibernate-commons-annotations" % "3.2.0.Final",
  "org.apache.directory.api" % "api-all" % "1.0.0-M20",
  "commons-io" % "commons-io" % "2.4",
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0",
  "org.elasticsearch" % "elasticsearch" % "1.4.1",
  "eu.medsea.mimeutil" % "mime-util" % "2.1.3"
)

lazy val root = (project in file(".")).enablePlugins(PlayJava)

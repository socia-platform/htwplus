name := "htwplus"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaCore,
  javaWs,
  //"com.typesafe.play" %% "play-jdbc" % "2.2.3" exclude("com.jolbox", "bonecp"),
  //"com.jolbox" % "bonecp" % "0.8.0.RELEASE",
  //javaJpa.exclude("org.hibernate.javax.persistence", "hibernate-jpa-2.0-api"),
  javaJpa,
  cache,
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.hibernate" % "hibernate-entitymanager" % "4.3.6.Final",
  "org.hibernate" % "hibernate-core" % "4.3.6.Final",
  "org.hibernate" % "hibernate-commons-annotations" % "3.2.0.Final",
  "org.apache.directory.api" % "api-all" % "1.0.0-M20",
  "commons-io" % "commons-io" % "2.4",
  "com.typesafe.play" %% "play-mailer" % "3.0.0",
  "org.elasticsearch" % "elasticsearch" % "1.4.1",
  "eu.medsea.mimeutil" % "mime-util" % "2.1.3",
  "org.imgscalr" % "imgscalr-lib" % "4.2",
  "joda-time" % "joda-time" % "2.7"
)

libraryDependencies += evolutions

lazy val root = (project in file(".")).enablePlugins(PlayJava)

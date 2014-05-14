name := "htwplus"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-jdbc" % "2.2.3" exclude("com.jolbox", "bonecp"),
  "com.jolbox" % "bonecp" % "0.7.1.RELEASE",
  javaJpa.exclude("org.hibernate.javax.persistence", "hibernate-jpa-2.0-api"),
  cache,
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "org.hibernate" % "hibernate-entitymanager" % "4.3.5.Final",
  "org.hibernate" % "hibernate-search" % "4.5.0.Final",
  "org.hibernate" % "hibernate-core" % "4.3.5.Final",
  "org.hibernate" % "hibernate-commons-annotations" % "3.2.0.Final",
  "org.apache.directory.api" % "api-all" % "1.0.0-M20",
  "commons-io" % "commons-io" % "2.4"
)     

play.Project.playJavaSettings

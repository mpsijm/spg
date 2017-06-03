name := "org.metaborg.spg.cmd"

version := "2.2.1"

scalaVersion := "2.11.8"

resolvers += Resolver.mavenLocal

resolvers += "Metaborg Release Repository" at "http://artifacts.metaborg.org/content/repositories/releases/"

resolvers += "Metaborg Snapshot Repository" at "http://artifacts.metaborg.org/content/repositories/snapshots/"

// Scala wrapper for slf4j and a logging backend
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
libraryDependencies += "ch.qos.logback" %  "logback-classic" % "1.1.7"

// Scala wrapper for Guice
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.1.0"

// CLI
libraryDependencies += "org.backuity.clist" %% "clist-core" % "3.2.2"
libraryDependencies += "org.backuity.clist" %% "clist-macros" % "3.2.2" % "provided"

// Spoofax
libraryDependencies += "org.metaborg" % "org.metaborg.spoofax.core" % "2.2.1"

// SPG
libraryDependencies += "org.metaborg" % "org.metaborg.spg.core" % "2.2.1"

import AssemblyKeys._
import NativePackagerKeys._

assemblySettings

ReleaseSettings.defaults

name := "openie"

organization := "edu.washington.cs.knowitall.openie"

crossScalaVersions := Seq("2.10.2")

scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head }

resolvers += "Sonatype SNAPSHOTS" at "https://oss.sonatype.org/content/repositories/snapshots/"

val clearGroup = "com.clearnlp"
val clearVersion = "2.0.2"

val breezeVersion = "0.2"
val breezeLearn = "org.scalanlp" %% "breeze-learn" % breezeVersion exclude("com.codecommit", "anti-xml_2.9.1") cross CrossVersion.binaryMapped {
  case "2.9.3" => "2.9.2"
  case "2.10.2" => "2.10"
  case x => x
}
val breezeProcess = "org.scalanlp" %% "breeze-process" % breezeVersion exclude("com.codecommit", "anti-xml_2.9.1") cross CrossVersion.binaryMapped {
  case "2.9.3" => "2.9.2"
  case "2.10.2" => "2.10"
  case x => x
}

val scopt = "com.github.scopt" % "scopt" % "2.1.0" cross CrossVersion.binaryMapped {
  case "2.9.3" => "2.9.2"
  case "2.10.2" => "2.10"
  case x => x
}

val unfilteredFilter = "net.databinder" %% "unfiltered-filter" % "0.7.0"
val unfilteredJetty = "net.databinder" %% "unfiltered-jetty" % "0.7.0"

libraryDependencies ++= Seq(
  clearGroup % "clearnlp-dictionary" % "1.0",
  clearGroup % "clearnlp-general-en-pos" % "1.1",
  clearGroup % "clearnlp-general-en-dep" % "1.2",
  clearGroup % "clearnlp-general-en-srl" % "1.1",
  "org.scalaz" %% "scalaz-core" % "7.0.3" cross CrossVersion.binaryMapped {
      case "2.9.3" => "2.9.2"
      case "2.10.2" => "2.10"
      case x => x
    },
  "edu.washington.cs.knowitall" % "reverb-core" % "1.4.3",
  unfilteredFilter, unfilteredJetty,
  scopt,
  breezeProcess,
  breezeLearn,
  "edu.washington.cs.knowitall" %% "openregex-scala" % "1.1.2",
  "edu.washington.cs.knowitall" % "morpha-stemmer" % "1.0.5",
  "org.apache.opennlp" % "opennlp-tools" % "1.5.3" exclude("net.sf.jwordnet", "jwnl"),
  "com.clearnlp" % "clearnlp" % "2.0.2" ,
  "edu.washington.cs.knowitall" % "opennlp-tokenize-models" % "1.5",
  "edu.washington.cs.knowitall" % "opennlp-postag-models" % "1.5",
  "edu.washington.cs.knowitall" % "opennlp-chunk-models" % "1.5",
  // for remote components
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
  // resource management
  "com.jsuereth" %% "scala-arm" % "1.3",
  // logging
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "ch.qos.logback" % "logback-core" % "1.0.13",
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "org.scalatest" % "scalatest_2.10" % "2.0.RC1" % "test")

mainClass in assembly := Some("edu.knowitall.openie.OpenIECli")

scalacOptions ++= Seq("-unchecked", "-deprecation")

// custom options for high memory usage

javaOptions += "-Xmx4G"

javaOptions += "-XX:+UseConcMarkSweepGC"

fork in run := true

fork in Test := true

connectInput in run := true // forward stdin/out to fork

licenses := Seq("Open IE Software License Agreement" -> url("https://raw.github.com/knowitall/openie/master/LICENSE"))

homepage := Some(url("https://github.com/knowitall/openie"))

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <scm>
    <url>https://github.com/knowitall/openie</url>
    <connection>scm:git://github.com/knowitall/openie.git</connection>
    <developerConnection>scm:git:git@github.com:knowitall/openie.git</developerConnection>
    <tag>HEAD</tag>
  </scm>
  <developers>
   <developer>
      <name>Michael Schmitz</name>
    </developer>
    <developer>
      <name>Bhadra Mani</name>
    </developer>
  </developers>)

packagerSettings

packageArchetype.java_application

mappings in Universal ++= Seq(
  file("README.md") -> "README.md",
  file("LICENSE") -> "LICENSE"
)

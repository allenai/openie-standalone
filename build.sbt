name := "openie"

organization := "edu.washington.cs.knowitall.openie"

crossScalaVersions := Seq("2.11.8")

scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head }

resolvers += "Sonatype SNAPSHOTS" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "ch.qos.logback" % "logback-core" % "1.0.13",
  "com.clearnlp" % "clearnlp" % "2.0.2" ,
  "com.clearnlp" % "clearnlp-dictionary" % "1.0",
  "com.clearnlp" % "clearnlp-general-en-dep" % "1.2",
  "com.clearnlp" % "clearnlp-general-en-pos" % "1.1",
  "com.clearnlp" % "clearnlp-general-en-srl" % "1.1",
  "com.github.scopt" %% "scopt" % "3.4.0",
  "edu.washington.cs.knowitall" % "morpha-stemmer" % "1.0.5",
  "edu.washington.cs.knowitall" % "opennlp-chunk-models" % "1.5",
  "edu.washington.cs.knowitall" % "opennlp-postag-models" % "1.5",
  "edu.washington.cs.knowitall" % "opennlp-tokenize-models" % "1.5",
  "edu.washington.cs.knowitall" % "reverb-core" % "1.4.3",
  "net.databinder" %% "unfiltered-filter" % "0.7.1",
  "net.databinder" %% "unfiltered-jetty" % "0.7.1",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
  "org.allenai.openregex" %% "openregex-scala" % "1.1.3",
  "org.apache.opennlp" % "opennlp-tools" % "1.5.3" exclude("net.sf.jwordnet", "jwnl"),
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.scalaz" %% "scalaz-core" % "7.0.9",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.specs2" % "specs2_2.11" % "2.3.13" % "test"
)

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


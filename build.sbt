import org.allenai.plugins.CoreDependencies

lazy val buildSettings = Seq(
  organization := "org.allenai.openie",
  crossScalaVersions := Seq(CoreDependencies.defaultScalaVersion),
  scalaVersion <<= crossScalaVersions { (vs: Seq[String]) => vs.head },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  licenses += ("Open IE Software License Agreement", url("https://raw.githubusercontent.com/allenai/openie-standalone/master/LICENSE")),
  homepage := Some(url("https://github.com/allenai/openie-standalone")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/allenai/openie-standalone"),
    "https://github.com/allenai/openie-standalone.git")),
  pomExtra := (
    <developers>
      <developer>
        <name>Michael Schmitz</name>
      </developer>
      <developer>
        <name>Bhadra Mani</name>
      </developer>
      <developer>
        <id>allenai-dev-role</id>
        <name>Allen Institute for Artificial Intelligence</name>
        <email>dev-role@allenai.org</email>
      </developer>
    </developers>),
  bintrayPackage := s"${organization.value}:${name.value}_${scalaBinaryVersion.value}",

  // Bintray supports specific OSS licenses:
  //
  //     https://bintray.com/docs/api/#_footnote_1
  //
  // OpenIE's license is unsupported by Bintray, which means it cannot be
  // published there publicly. Here we ask Bintray to bypass the license
  // restriction check during publication because we intend to publish
  // privately.
  bintrayOmitLicense := true,
  bintrayRepository := "private"
)

lazy val openie = Project(id = "openie", base = file("."))
  .settings(buildSettings)
  .enablePlugins(LibraryPlugin)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "ch.qos.logback" % "logback-core" % "1.0.13",
  "com.clearnlp" % "clearnlp" % "2.0.2",
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
  "org.scalaz" %% "scalaz-core" % "7.0.9",
  "org.slf4j" % "slf4j-api" % "1.7.5",

  "nl.jqno.equalsverifier" % "equalsverifier" % "2.1" % "test",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.specs2" % "specs2_2.11" % "2.3.13" % "test"
)

scalacOptions ++= Seq("-unchecked", "-deprecation")

javaOptions += "-Xmx4G"

javaOptions += "-XX:+UseConcMarkSweepGC"

fork in run := true

fork in Test := true

// forward stdin/out to fork, so the OpenIE CLI can be run in sbt.
connectInput in run := true

// The style warning "Line is more than 100 characters long" appears in
// hundreds of places and is not very important in this codebase at the moment.
StylePlugin.enableLineLimit := false

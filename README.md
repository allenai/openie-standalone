Open IE [![Build Status](https://semaphoreci.com/api/v1/allenai/openie-standalone/branches/master/badge.svg)](https://semaphoreci.com/allenai/openie-standalone)
======

This project contains the principal Open Information Extraction (Open IE)
system from the University of Washington (UW).  An Open IE system runs over
sentences and creates extractions that represent relations in text.  For
example, consider the following sentence.

    The U.S. president Barack Obama gave his speech on Tuesday to thousands of people.

There are many binary relations in this sentence that can be expressed as a
triple `(A, B, C)` where `A` and `B` are arguments, and `C` is the relation
between those arguments.  Since Open IE is not aligned with an ontology, the
relation is a phrase of text.  Here is a possible list of the binary relations
in the above sentence:

    (Barack Obama, is the president of, the U.S.)
    (Barack Obama, gave, his speech)
    (Barack Obama, gave his speech, on Tuesday)
    (Barack Obama, gave his speech, to thousands of people)

The first extraction in the above list is a "noun-mediated extraction", because
the extraction has a relation phrase is described by the noun "president".  The
other extractions are very similar.  In fact, they can be represented more
informatively as an n-ary extraction.  An n-ary extraction can have 0 or more
secondary arguments.  Here is a possible list of the n-ary relations in the
sentence:

    (Barack Obama, is the president of, the U.S.)
    (Barack Obama, gave, [his speech, on Tuesday, to thousands of people])

Extractions can include more than just the arguments and relation as well.  For
example, we might be interested in whether the extraction is a negative
assertion or a positive assertion, or if it is conditional in some way.
Consider the following sentence:

    Some people say Barack Obama was born in Kenya.

We would not want to extract that `(Barack Obama, was born, in Kenya)` alone
because this is not true.  However, if we have the condition as well, we can
have a correct extraction.

    Some people say:(Barack Obama, was born in, Kenya)

To see an example of Open IE being used, please visit http://openie.cs.washington.edu/.

## Notifications

* June 2016

This repository is a modification of https://github.com/knowitall/openie
at 9dcbf4b0a4fd088d780a3f4480ce9ce811295f30 to run on Scala 2.11.

This project currently has many dependencies that would need to migrate to Scala 2.11.
To make the migration and future maintenence easier, @schmmd copied in the dependent
libraries:

  * [Common-scala](https://github.com/knowitall/common-scala)
  * [NLP Tools](https://github.com/knowitall/nlptools)
  * [ChunkedExtractor](https://github.com/knowitall/chunkedextractor)
  * [Srlie](https://github.com/knowitall/srlie)

In addition, some code has been removed to ease migration to Scala 2.11:

  * https://github.com/allenai/openie-standalone/commit/6b4c22ed6a7779c95042e103d4e4975167b7bf55

Finally, `ClearSrl.scala` was adjusted to produce consistent behavior in Scala 2.11:

  * https://github.com/allenai/openie-standalone/commit/bb5cf227448d2bf077146899c537d72fd8a228f0

* [01/15/2016] The version 4.2.0 is released ([release notes](https://github.com/knowitall/openie/blob/master/release/release_notes.md)).

## Google Group

* [knowitall_openie](https://groups.google.com/forum/#!forum/knowitall_openie)

## Research

Open IE 4.x is the successor to [Ollie](http://www.github.com/knowitall/ollie).
Whereas Ollie used bootstrapped dependency parse paths to extract relations
(see [Open Language Learning for Information Extraction](https://homes.cs.washington.edu/~mausam/papers/emnlp12a.pdf)),
Open IE 4.x uses similar argument and relation expansion heuristics to create
Open IE extractions from SRL frames.  Open IE 4.x also extends the defintion of
Open IE extractions to include n-ary extractions (extractions with 0 or more arguments 2s).

## Buiding

`openie` uses java-7-openjdk & the [sbt build system](http://www.scala-sbt.org/), so downloading
dependencies and compiling is simple.  Just run:

    sbt compile

## Memory requirements

`openie` requires substantial memory.  `sbt` is configured to use these options by default:

    -Xmx4G -XX:+UseConcMarkSweepGC

## Usage

### Command Line Interface

Check out this project and use `sbt` to run the CLI:

    % git clone git@github.com:allenai/openie-standalone.git
    % cd openie-standalone
    % sbt "runMain edu.knowitall.openie.OpenIECli"
    ...
    [info] * * * * * * * * * * * * *
    [info] * OpenIE 4.1.x is ready *
    [info] * * * * * * * * * * * * *

`openie` takes a number of command line arguments.  To see them all run
`sbt "runMain edu.knowitall.openie.OpenIECli --usage"`. Of particular interest are
`--ignore-errors` which continues running even if an exception is encountered, `--binary` which gives the binary(triples) output and `--split` which splits the input document text into sentences.

There are two output formats in the CLI: a simple format made for ease of reading and a
columnated format used for machine processing.  The format can be specified
with either `--format simple` or `--format column`.  The simple format is
chosen by default.

### Code interface

You can depend on the OpenIE library in your SBT-based application instead of using the CLI. The minimal declaration in your `build.sbt` file is:

```scala
resolvers += Resolver.jcenterRepo
libraryDependencies += "org.allenai.openie" %% "openie" % "4.2.6"
```

#### Hello World example

To illustrate how OpenIE can be done used in your application, here is a *Hello World* application.

Make a new directory and create three files:

File 1: `build.properties`:

```
sbt.version=0.13.11
```

File 2: `build.sbt`:

```
scalaVersion := "2.11.8"
resolvers += Resolver.jcenterRepo
libraryDependencies += "org.allenai.openie" %% "openie" % "4.2.6"
```

File 3: `OpenIEHelloWorld.scala`:

```scala
import edu.knowitall.openie.OpenIE
import edu.knowitall.openie.OpenIECli.{ColumnFormat, SimpleFormat}
import java.io.{PrintWriter, StringWriter}

object OpenIEHelloWorld {
    val openie = new OpenIE
    val sentence = "U.S. President Obama gave a speech"
    val instances = openie.extract(sentence)

    def main(args: Array[String]): Unit = {
        println("Hello, OpenIE world.")
        exampleUsage1()
        exampleUsage2()
        exampleUsage3()
    }

    def exampleUsage1() : Unit = {
        println("Example Usage 1:")
        println("")
        var s = new StringBuilder()
        for (instance <- instances) {
          s.append("Instance: " + instance.toString() + "\n")
        }
        println(s.toString())
    }

    def exampleUsage2() : Unit = {
        println("Example Usage 2:")
        println("")
        val sw = new StringWriter()
        SimpleFormat.print(new PrintWriter(sw), sentence, instances)
        println(sw.toString())
    }

    def exampleUsage3() : Unit = {
        println("Example Usage 3:")
        println("")
        val sw = new StringWriter()
        ColumnFormat.print(new PrintWriter(sw), sentence, instances)
        println(sw.toString())
    }

}
```

In this example, `openie.extract(...)` is used. It returns a sequence of `Instance` objects, which are containers for extractions and confidences. Each extraction has various fields you can access manually. These fields are used directly (`.toString`), with `SimpleFormat` and with `ColumnFormat`.

Finally, run the application with `sbt -J-Xmx4G run` to see output like this:

```
Hello, OpenIE world.
Example Usage 1:

Instance: 0.93 (U.S. President Obama; gave; a speech)
Instance: 0.88 (Obama; [is] President [of]; United States)

Example Usage 2:

U.S. President Obama gave a speech
0.93 (U.S. President Obama; gave; a speech)
0.88 (Obama; [is] President [of]; United States)


Example Usage 3:

0.9329286852051247     		SimpleArgument(U.S. President Obama,List([0, 20)))     	Relation(gave,List([21, 25)))  	SimpleArgument(a speech,List([26, 34)))	U.S. President Obama gave a speech
0.8847999636040884     		SimpleArgument(Obama,List([15, 20)))   	Relation([is] President [of],List([5, 14)))    	SimpleArgument(United States,List([0, 4)))     	U.S. President Obama gave a speech
```

## Contributors
* Michael Schmitz (http://www.schmitztech.com/)
* Harinder Pal (http://www.cse.iitd.ac.in/~mcs142123/)
* Bhadra Mani
* Michal Guerquin

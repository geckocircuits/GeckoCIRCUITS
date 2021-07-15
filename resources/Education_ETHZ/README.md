# GeckoCircuits

## What is it?

This is a JDK13 runnable port of [GeckoCircuits](https://sourceforge.net/projects/geckocircuits/).
The main goal was to enable HiDPI monitors to display GeckoCircuits in reasonable size.

The original Readme can be found [here](README.txt)

All the source is from the original author. We just modified it such that it runs with JDK13 and also set up a more or less sane Maven project.

Technokrat GmbH takes no credit whatsoever on the original codebase.

## Prerequisites

To build, you need maven 3 installed and a more recent JDK. Everything from JDK9 to JDK13 should work.

## Building

Then run

```
mvn package
mvn package assembly:single
```

This should build the `target/gecko-1.0-jar-with-dependencies.jar`.
It should have a proper class path set and includes all the dependency libs.

## Running

Run it with

```java -jar target/gecko-1.0-jar-with-dependencies.jar```

To run on HiDPI screens, use

```java -Dsun.java2d.uiScale=2 -jar gecko-1.0-jar-with-dependencies.jar```

## Tests

As you might have recognized during the build, 11 tests were skipped. Those were excluded as the codebase is hard to read and the tests only seem to fail because of some expectations how the environment should luck which is not given outside Netbeans.

Feel free to fix those tests.
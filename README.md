# GeckoCircuits

This is the source code package of the software GeckoCIRCUITS. GeckoCIRCUITS is a fast circuit simulator which is optimized for applications in power electronics.


## What is GeckoCIRCUITS?
See the [FAQ](FAQ.md) section

## What is this repo?

This is the "official" new GeckoCIRCUITS Github repository, created and maintained by one of the original software authors.
GeckoCIRCUITS was hosted on Sourceforge before [GeckoCircuits](https://sourceforge.net/projects/geckocircuits/).
My friends at Technokrat [https://github.com/technokrat/gecko] did do some improvements (e.g. support of HiDPI Monitors) to the software, this changes are included in this repo, thanks for this contribution!

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

```java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar```

To run on HiDPI screens, use

```java -Xmx3G -Dpolyglot.js.nashorn-compat=true -Dsun.java2d.uiScale=2 -jar gecko-1.0-jar-with-dependencies.jar```

## Tests

As you might have recognized during the build, 11 tests were skipped. Those were excluded as the codebase is hard to read and the tests only seem to fail because of some expectations how the environment should luck which is not given outside Netbeans.

Feel free to fix those tests.

If you would like to simply run the program, and without the intention to change, inspect
the sourcecode or to compile the program from scratch, you should probably download
the binary package, available at www.gecko-simulations.com instead of this sourcecode
package.

This software is published under the GNU General Public License Version 3 (GPLv3), originally written by Andreas Müsing, Andrija Stupar and Uwe Drofenik.

You are free to use any IDE for compiling the sources. However we recommend you to use the Netbeans IDE, since the project with all settings an necessary files is already done in Netbeans.


You can redistribute it and/or modify it under the terms of the GNU General Public License version 3 as published by the Free Software Foundation. For the terms of this license, see licenses/gpl_v3.txt or http://www.gnu.org/licenses/ .

For a commercial usage/redistribution, please contact Gecko-Research GmbH to obtain a commercial license.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses/ .

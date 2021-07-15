# FAQ 
(transcription of http://www.gecko-research.com/about-us.html )
## What was your motivation to write another simulator?

In fact, GeckoCIRCUITS is just part of an integrated package of tools we will provide for power electronics simulation. The combined platform allows very fast and highly accurate multi-domain simulation. Within the Power Electronic Systems Laboratory (PES) at the ETH Zurich we experienced a need for a tightly integrated tool with a convenient user interface. Our goal was to create a multi-domain simulation tool to reduce development time and increase product quality.

Our design goals were:

- Thermo-electrical simulation made easy, e.g. transient junction temperatures, or short-term overload temperatures.
- To integrate features like the EMI-Testreceiver for EMI filter design. Sometimes new features are needed to solve a design problem quickly. With GeckoCIRCUITS this now can be done.
- Testing new models and algorithms. Most of the researchers of the PES, ETH Zurich, also build hardware. So we have a direct access to testing and verifying our theories.
- To establish an easy-to-learn tool for the PhD and MSc students taking power electronics courses at the ETH Zurich.
- To get a very fast circuit simulator with an open and easy-to-use Java-Interface.
With Java, there are no problems with a partly inhomogeneous IT environment (Windows, Unix, Solaris).

## What does "Gecko" mean?

We spend some time thinking about a good company name. We didn't want to have another acronym with "sim" or "soft" included, and/or with a diode in the logo. Concerning the logo we had a longer discussion about frogs vs. chameleons, and finally we agreed on the gecko. Geckos are flexible, quick, useful and have some unique and amazing abilities ... just like our software.

## The simulator is written in Java. Isn't Java too slow for this task?

When we wrote iPES in 2001, Java was much slower than now. Back then, we had to optimize the speed of the simulation code in order to make the numerical simulations work in a web browser. With this knowledge "how to make Java really fast" we developed the algorithms of GeckoCIRCUITS. Since then the calculation speed of the Java Virtual Machine has improved significantly. Today, GeckoCIRCUITS is extremely fast. It would not be even faster if written in C++, especially when you combine the circuit simulation with thermal simulation. Perform your own benchmarks with our free trial version (trial@gecko-research.com)!

## We are a small company with limited resources. We would like to integrate simulation into our design process but ...

Gecko-software was designed to solve this problem: It is very easy to learn and use, and performs the hard work of calculating losses and junction temperatures, building thermal models, optimizing heat sink structures, or calculating EMI effects without much user-interaction. One can become familiar with the software and the features within a short time, and will be able to use it even on a part-time base. We think it is the perfect way to get multi-domain aspects into your power electronic design.

## Who is behind "Gecko-Research"?

Gecko-Research was founded by Uwe Drofenik, Andreas Müsing, Johann W. Kolar and Beat Seiler in 2008 as an ETH Zurich Spin-Off. Gecko software is based on research results of the Multi-Domain Simulation and Optimization Group of the PES, ETH Zurich. The software is widely used in research projects at PES aiming for advanced power electronics concepts and finally hardware prototypes.

## My background is electrical engineering. But I would like to get knowledge in thermal design. How much effort is this?

Some people say it needs at least 1000 hours to become an expert in any field, and they are probably right. Thermal design is not part of the typical education in electrical engineering but it is becoming more and more important, especially in power electronics. Therefore, getting experience in thermal design would be an excellent extension to your expertise. You can get a quick and easy start with one of our free reports. Gain experience with GeckoCIRCUITS' built-in loss calculation and thermal design tools. It's a step-by-step process but it will be very rewarding.

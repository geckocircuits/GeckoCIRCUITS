================================================
================================================
Gecko PES Swiss Rectifier Optimizer (March 2015)
================================================
================================================

Thank you for downloading the Gecko Swiss Rectifier Optimizer! This application allows you to design an efficiency-optimized three-phase PFC rectifier.

It is completely free to use, indefinetely. The purpose of it is two-fold:

1) To demonstrate how GeckoCIRCUITS and GeckoMAGNETICS can be used to build further applications on top of them for various power electronics applications,
i.e. in this case to complete the full optimization of an entire converter design.

2) To showcase features which will become available in future Gecko-Simulations' products.

Please bear in mind that this is a DEMONSTRATOR program. NO warranties of any kind are given.

====================
Program requirements
====================

The Optimizer requires Java (Version 7 or newer) to be installed on your system. If Java is not installed on your system,
please install it before trying to run this application. See more information at: http://www.java.com

If you have the proper version of Java installed on your computer, the Optimizer should run on any operating system. It has been tested
on Windows and Linux machines, but should work with any other OS as well (e.g. MacOS X, BSD, Solaris, etc.).

====================
Starting the program
====================

If you use a Windows or Linux operating system, start the program by double-clicking on StartOptimizer.jar.

If this does not work on your computer, open a command prompt or terminal window, go to the directory where this application is located,
and type:

java -jar StartOptimizer.jar

and the program should start, and sufficient memory to it will automatically be allocated.


If you are using another kind of operating system (e.g. MacOS), using StartOptimizer.jar will NOT work. In this case you must start the program manually.

Double-clicking on PESOptimizerSwissRect.jar should start it, but this is NOT recommended, since you are not guaranteed that the program will be allocated sufficient memory.

Therefore, in this case, in a command prompt or terminal window, go to the directory where this application is located,
and type:

java - Xmx2048m -XX:-UseGCOverheadLimit -jar PESOptimizerSwissRect.jar

The above command allocated 2 GB (2048 MB) of memory for the Optimizer. The larger the design space you want the converter optimization to cover,
the more memory is needed. Therefore, it is suggested to make the number after "Xmx" above as high as possible on your system. 

For example, if you have 4 GB or RAM on your computer, allocate at least 3 GB using "-Xmx3072m" in the command above.

===================================
Quick Overview of Using the Program
===================================

If using the default circuit simulation model, no changes need to be made in the first tab (Model Setup). Just click on Apply Names and Next.

Click on Start GeckoCIRCUITS, GeckoCIRCUITS should open and open the Swiss Rectifier model (SwissRect.ipes).

WARNING: Windows might ask for the administrative password to allow Java network access. Allow it, both times, otherwise the application will not work.

The rest of the process is mostly self-explanatory. Be sure to click on Apply Names, Apply Parameters, Apply Settings, at each step, before continuing, especially (!) if enabling the EMI option which is set up in a separate window.

Note the thermal properties of the inductor (also separate window) - default is natural convection, but default settings make the inductor very very hot. Try with forced, 30 m/s, for better results with the default settings.

It is NOT recommended to simply take the default settings and click 'Start Optimization' - it will take a not so short time (half an hour at least). Try evaluating upper and lower bounds first (single run), to get a feeling for the program, then try some very constrained optimizations (e.g. 5-6 different converters in total).

During calculations, an output window shows up showing the progress. Once done, click on the Results Visualization tab and click the "refresh" button - results will appear in graphical form. All the raw data (current stresses, DM EMI filter component suggestions, inductor losses by type, etc.) is stored in the specified output file, and also, remains in the output window until you close it (a separate window is opened for each run).

In the graphs, you can zoom (right click, menu or left click, drag and make rectangle around area to zoom).

BEFORE USING THE PROGRAM, please go through the short introductory presentation included with this application (QuickIntro.pdf or QuickIntro.pptx).

Report any bugs, errors, or general feedback to to andrija.stupar@gecko-simulations.com


====================================================
Reproducing results from the sample output directory
====================================================

---------
EXAMPLE 1
---------

Leave all options as given in the default setup, EXCEPT:
Converter Design Parameters -> Output Inductor -> View/Set Thermal Properties
Set convection to FORCED,
Air speed to 30 m/s,
and air flow to FRONTBACK,

and click "Multiple Selection" for Cores, select ELP64 and E58/11/38,

and click "Solid Wire" (disable "Fill Factor" option),

and click "Multiple Selection" for Wires, and select AWG10 and AWG11,

and select "Exhaustive Optimization" under the Optimization Settings tab.


The optimization run takes about 40 mins on an Intel Core i7-2620M 2.7GHz CPU with 4 GB RAM running 64bit Windows 7 Enterprise.


---------
EXAMPLE 2
---------

Leave all options as given in the default setup, EXCEPT:
Converter Design Parameters -> Output Inductor -> View/Set Thermal Properties
Set convection to FORCED,
Air speed to 5 m/s,
air flow to FRONTBACK,
and core orientation to HORIZONTAL,

and click "Multiple Selection" for Cores, select E30, E34, E42, D43/10/28, E55, E58/11/38 and and ELP64,

and set "Stacked cores" to 1 to 4

and select the "Filling Factor" option and set it from 0.3 to 0.6 by 0.1 (NOT fixed)

and select "3C81" as the core material,

and select "Genetic algorithm" under the Optimization Settings tab.


The optimization run takes about 2 hrs on an Intel Core i7-2620M 2.7GHz CPU with 4 GB RAM running 64bit Windows 7 Enterprise.
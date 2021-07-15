# AC/AC-Conversion for Highly Compact Drives - What Options Do I Have?
## PART I: An Overview of AC/AC-Converter Topologies

### Overview – Classification of Three-Phase AC/AC Converters
 
For operating a Permanent Magnet Synchronous Machine (PMSM), which allows a highly compact design, you have to supply three-phase voltage with controllable output frequency and controllable voltage amplitude. There are many different alternatives for the AC/AC converter. It is very helpful to look at the systematic classification shown in Fig. 1 which is based on a discussion in [25]. In this report we will have a look at the converters in the green boxes. For details of all converter systems see the references.

Fig. 1: Classification of Three-Phase AC/AC Converters.
 


### The Classic Solution – AC/AC Conversion Employing a DC-Link

In order to control the speed of a Permanent Magnet Synchronous Machine (PMSM) one must be able to control voltage amplitude and frequency of the three-phase voltage system, which connects to the PMSM.
 
A standard solution for AC/AC conversion with
 
•      Controllable output voltage amplitude
•      Controllable output frequency
•      Approximately sinusoidal input currents
•      Bidirectional power flow
 
is the coupling of two inverters via a DC-link. The topology employing a capacitor, which gives defined voltage in the DC link, is shown in Fig. 2. The PMSM is modelled by three inductors and three voltage sources connected to nodes A, B, C at the output side.
 
 
 
 
Fig. 2: Topology of the three-phase AC/AC converter with voltage DC-link.
 
 
 
The input side converter could be alternatively realized as simple diode bridge, but the input current would contain significant low-frequency harmonics then. The diode bridge could not feed back braking energy in to the mains. Therefore, a braking resistor in the DC link would be needed. Alternatively, a thyristor bridge at the input side could feed back braking energy, but would still suffer from significant low-frequency input current harmonics, especially during inverter operation.

Instead of defining a voltage in the DC link, one could also define the current in the DC link via DC link inductor as shown in Fig. 3. The converter system in Fig. 3 needs small capacitive filters at input- and output side for decoupling the inductors.
 
 
 
 
Fig. 3: Topology of the three-phase AC/AC converter with current DC-link.
 
 
The big advantage of the DC link is the decoupling (at least, to a large extent) of the control tasks of the input-side and the output-side converter.
 
A disadvantage of such converter systems is the DC-link energy storage element which shows typically a relatively large physical volume. Furthermore, in case an electrolytic capacitor is used in the topology of Fig. 2, this bulky passive component would reduce the system lifetime because its reliability is relatively low in comparison with the other components in the power circuit.
 
### How the Matrix Converter Helps You to Get Rid of Passive Components in the DC-Link
 
Matrix Converters achieve three-phase AC/AC conversion without any intermediate energy storage element. This has the potential to increase power density (output power per converter volume) significantly. Furthermore, omitting the electrolytic capacitor in the DC-link will improve the system reliability.

The topology of the Conventional Direct Matrix Converter is shown in Fig. 4. The main idea is to be able to connect each input phase directly to each output phase at any time in order to put together a three-phase output voltage system as needed for the variable-speed drive.
 
A disadvantage is the complicated multi-step current commutation scheme where the current control always has to make sure that free wheeling can occur. A large number of states must be considered, and signs of currents and voltages have to be taken into account when defining the switching pattern. A current control error will result in immediate destruction of power switches. Learn more in [26] or visit the free Java-based Matrix Commutation Animation of iPES (Interactive Power Electronics Seminar at www.ipes.ethz.ch/).
 
Matrix Converters are often seen as a future concept for variable speed drives technology, but despite intensive research over the decades they have until now only achieved low industrial penetration. As mentioned before, the reason for this might be the higher complexity in modulation and analysis effort [25].
 
The decision if to select a DC-link topology or a matrix converter is dependent on the switching frequency and on the semiconductor technology. For switching frequencies above 30kHz matrix converters become competitive.

### Sparse Matrix Converter – Same Functionality But Reduced Semiconductor Count Plus Safe Commutation
 
The Sparse Matrix Converter is a new kind of AC/AC converter with a reduced number of components and a modulation scheme of low complexity and realization effort. Sparse Matrix Converters avoid the multi-step commutation procedure of the conventional matrix converter which could impair the system reliability for operation in industrial environment.
 
The characteristics of the Sparse Matrix Converter are
 
•      Quasi-Direct AC-AC conversion with no DC link energy storage elements
•      Sinusoidal input current in phase with mains voltage
•      Zero DC link current commutation scheme resulting in lower modulation complexity and very high reliability
•      Low complexity of power circuit / power modules available
•      Ultra-Sparse Matrix Converter, does show very low realization effort, in case unidirectional power flow can be accepted (admissible displacement of 30° the input current fundamental and input voltage, as well as for the output voltage fundamental and output current), accordingly, a possible application area would be variable speed PSM drives of low dynamics.

#### Sparse Matrix Converter (SMC)
 
The Sparse Matrix Converter topology is characterized by 15 Transistors, 18 Diodes, and 7 Isolated Driver Potentials.

Fig. 5: Topology of the Sparse Matrix Converter.
 
Compared to the Direct Matrix Converter, this topology provides identical functionality, but with a reduced number of power switches and the option of employing an improved zero DC link current commutation scheme, which provides lower control complexity and higher safety and reliability.
 
 
#### Very Sparse Matrix Converter (VSPM)
 
Characteristics of the Very Sparse Matrix Converter topology are 12 Transistors, 30 Diodes, and 10 Isolated Driver Potentials. It shows identical functionality compared to the Direct Matrix Converter and/or the Sparse Matrix Converter.
 
Compared to the Sparse Matrix Converter there is a smaller number of transistors, and higher conduction losses due to the increased number of diodes in the conduction paths.

Fig. 6: Topology of the Very Sparse Matrix Converter.
 
 
#### Ultra Sparse Matrix Converter

Fig. 7: Topology of the Ultra Sparse Matrix Converter.
 
 
The Ultra Sparse Matrix Converter topology has got 9 Transistors, 18 Diodes, and 7 Isolated Driver Potentials.
 
As mentioned before, the significant limitation of this converter topology as compared to the Sparse Matrix Converter is the restriction of its maximal phase displacement between load-side voltage and input current to ± 30°. The reason is that the input stage of this converter is unidirectional. Possible applications would be PMSM (small phase displacement) with no energy-feedback into the mains.
 
 
 
### Finally a Simulation of the Sparse Matrix Converter
 
Let’s perform a numerical simulation of the operation of the Sparse Matrix Converter (SMC) which is shown in Fig. 5.

Fig. 8: Java-Animation of the Sparse Matrix Converter at www.ipes.ethz.ch.

We will employ our new simulator GeckoCIRCUITS which gives the following benefits: 
 
•      Easy to learn and use
•      Very fast and numerically stable
•      Multi-domain: Electric - Thermal - EMI
•      Easy & fast calculation of transient junction temperatures
•      Free online version, no installation required
 
For the family of the Sparse Matrix Converters, we employ the Zero DC Link Current Commutation which is described in detail in [19]. The scheme makes the commutation safe and simple compared to the conventional multi-step commutation scheme. 
 
One has to define the relevant voltage sector pairs at the input and output side. There are 12 different voltage sectors over one mains period for each side. Fig.8 shows voltage sector 2 for the input side and voltage sector 11 for the output side as yellow shaded areas. The position of the red vertical time slider defines the relevant pair of sectors.
 
Dependent on the sectors, the power switches have to be switched in certain sequences. This is shown in Fig. 8, bottom diagram, right-hand side. The individual on- and off-times are calculated from equations which are dependent on the relevant sector pair. All information including the equations is given in detail in [19] and/or [21]. All equations are fully implemented in the Java-Applet of Fig. 8. We use the applet to debug simulation and hardware prototypes step by step.
 
Obviously, there waits a lot of hard work to implement a current controller with so many different states to be defined from measured voltages, and such a large number of state-dependent equations and different switching sequences.
 
That’s why we will employ the powerful JAVA-Block of our simulator GeckoCIRCUITS. First let’s study a simple example to understand the JAVA-Block located in GeckoCIRCUITS’ Tab “Special”:

Fig. 9: JAVA-Block in GeckoCIRCUITS.


Fig. 10: Put the File “tools.jar” Into to Folder jre1.6/lib/ext/ of Your Actual Java Runtime Environment to make the JAVA-Block work.
 
 
 
The JAVA-Block allows to directly write Java-Code which is compiled before begin of the simulation. This Java-Code is then executed within each numerical time step, processing input variables and generating output variables.
 
The JAVA-Block uses the Java-compiler available in Sun’s library “tools.jar” which you have to install in order to make the JAVA-Block work. If “tools.jar” is not yet available on your computer and you double-click the JAVA-Block, you will see a warning dialog which tells you where to get the file “tools.jar” from, and into what folder you should put it. You can download “tools.jar” for free embedded in Sun’s JDK 1.6.
 
In case of problems and/or difficulties please contact contact@gecko-research.com.
 
The JAVA-Block allows in a very simple way the implementation of highly complex functions, and/or creating control blocks one would need but are not included in the control library yet.
 
 
 
#### How to Use the JAVA-Block
 

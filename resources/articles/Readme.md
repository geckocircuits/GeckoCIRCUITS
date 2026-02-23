
# GeckoCircuits Power Electronics Newsletter Archive

## The Swiss Rectifier (July 5, 2012)

In this newsletter, we present a new rectifier converter topology, the Swiss Rectifier. What was the motivation for the new topology? The easiest approach to three-phase mains rectification is a simple diode bridge. However, a diode bridge has a low power factor and introduces unwanted low frequency current harmonics into the grid. Additionally, many applications require a controlled output voltage. Therefore, Power Factor Correction (PFC) rectifiers are typically used, e.g. a 3-phase six-switch buck-type PFC rectifier. The new Swiss Rectifier employs active third harmonic current injection and allows to achieve efficiencies superior to that of conventional buck-type systems.

## Inverter Operating Limits Optimization with GeckoSCRIPT (October 18, 2011)

In this newsletter, we show how to optimize the limits of operation of a neutral point clamped (NPC) inverter which can operate at different switching frequencies and must be able to generate a range of output voltage frequencies. The selected IGBT module has a maximum allowable junction temperarature of 130°C. We would like to determine what is the maximum output current the NPC inverter can supply for a particular combination of switching and output frequency without exceeding the maximum junction temperature?

## How to Calculate Transient Semiconductor Temperatures (Thermal Model of Semiconductor) - Part III (July 17, 2011)

In this little example we want to find out how short-term overload effects junction temperatures. How does the junction temperature of the IGBT of a boost converter rise if there is a significant increase of the output power for a duration of 10ms vs. a duration of 1ms? ...

## How to Calculate Transient Semiconductor Temperatures (Heat Sink Optimization) - Part II (Oct. 26, 2010)

Let's have a look at heat sink optimization. If you want to optimize a converter for power density (output power in relation to converter volume), you must keep the heat sink small because often the heat sink contributes significantly to the converter volume ...

## How to Calculate Transient Semiconductor Losses in a Simple Way - Part I (Sept. 21, 2010)

If junction temperature is too high, the power semiconductor will be destroyed. A good design of the heat sink will make sure that converter lifetime can be guaranteed. Thermal design is essential for building reliable products ...

## High-Speed Motor: One Million Revolutions per Minute (August 2, 2010)

In 2008 researchers at ETH Zurich have broken the barrier of 1,000,000 rpm which is the highest rotational speed ever achieved by an electric drive. The higher the rotational speed the smaller the drive. The prototype drive system generated an output of 100 watts and was barely bigger than a matchbox ...

## 200W PFC of Only 1mm Thickness (May 10, 2010)

Soft switching employs parasitic inductances and capacitors to reduce switching losses. Ideally, you get a huge benefit with intelligent control without the need to add components. Why not using the internal output capacity of a power MOSFET for elimination of switching losses? ...

## Loss of One Phase of a Three-Phase Rectifier (March 22, 2010)

What happens if one phase of a three-phase rectifier is lost? Will the rectifier keep working in single-phase mode with sinusoidal phase current? Of course it depends on the converter topology and the control. Researcher from the Power Electronic Systems Lab., ETH Zurich, worked on such a feature and the control some time ago, and implemented this in the three-phase AC/DC Vienna Rectifier ....

## What Simulation Speed Do We Need in Power Electronics? (Feb. 23, 2010)

When we built the first prototype of a 10kW Three-Phase AC/DC Vienna Rectifier in 1994, we started with circuit simulations to make sure the new topology would really work. We used PSpice, and simulating one mains period of 20ms took more than 12 hours ...

## 3kW Single Phase PFC with 99.2% Efficiency (Jan. 15, 2010)

... But now the idea was to maximize efficiency, and the research group around J. W. Kolar and J. Biela realized a single-phase PFC prototype with 99.2% efficiency (measured via calorimeter). Details of this optimization will soon be published ...

## Simulating a 5kW Aircraft Power System (Nov. 20, 2009)

... The rectifier in this example is an interesting topology for aircraft power systems with typical input voltage [98V ... 132V] and a mains frequency [400Hz ... 800Hz]. It's an amazing converter with no active switches (no control electronics) but approximately sinusoidal input currents. ...

## Text and Comments in the Worksheet (Oct. 29, 2009)

One missing feature is now available in GeckoCIRCUITS: It is the ability to write text comments directly into the worksheet. Have a look at the GeckoCIRCUITS online version ...

## Sunday morning meeting in the laboratory (Sept. 8, 2009)

... Now we have implemented blocks to make motor control task easy in GeckoCIRCUITS. Check out the latest example below (we set the new PMSM-example as default). Prof. Nishida employs GeckoCIRCUITS now ...

## Debugging a Complex Converter Topology (Aug. 26, 2009)

With converter models of high complexity it is always very important to be able to reliably check where all connections are and if components are really connected. ...

## How to simulate non-linear inductors (Aug. 10, 2009)

In two recent PhD projects at the Power Electronic Systems Lab., ETH Zurich, it became necessary to model non-linear capacitors and inductors. We spent the last week to implement such non-linear models into GeckoCIRCUITS ...

## How do you teach power electronics? (July 31, 2009)

Maybe you have to teach colleagues or customers how power electronics works, maybe you supervise students or make your money as electronics consultant. Or you need to remember how a certain topology works. Based on our own experience we developed years ago the free online-course iPES ...

# Free Reports: Power Electronics Simulation and Application

To learn a few tricks how to speed up work with GeckoCIRCUITS, just go through our free reports! The reports are also packed with up-to-date knowledge of power electronics. More content will be added!
Important Information:
You can simulate most of the examples shown in the reports online! Just go to the Online-Version of GeckoCIRCUITS (Java-Applet). Or contact us for a free trial version of GeckoCIRCUITS plus the related examples!

## AC/AC-Conversion for Highly Compact Drives - What Options Do I Have

For operating a Permanent Magnet Synchronous Machine (PMSM), which allows a highly compact design, you have to supply three-phase voltage with controllable output frequency and controllable voltage amplitude. There are many different alternatives for the AC/AC converter. Here you will learn all options.

### Part I - An Overview of AC/AC-Converter Topologies

## How to Design a 10kW Three-Phase AC/DC Interface Step by Step
You need a rectifier with sinusoidal input currents (power factor correction) and controlled DC-voltage at the output side? In this report you will learn how to compare the well-known Bidirectional 3-Phase AC/DC PWM Converter with Impressed Output Voltage (VSR) with a Vienna Rectifier employing a simple but effective strategy.

### Part I - How Can I Compare Topologies?
### Part II - Semiconductor Loss Calculation Demystified
### Part III - Do You Know the Junction Temperatures of Your Design?

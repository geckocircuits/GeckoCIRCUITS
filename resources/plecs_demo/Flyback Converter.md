# Flyback Converter

Overview
Electrical model
Simulation

## Overview
This example demonstrates an unregulated single-output ﬂyback converter.
pict 
Figure 1: Flyback converter

## Electrical model

A ﬂyback converter supplies a constant DC output voltage from either a rectiﬁed AC or DC source. A transformer provides isolation between the input and output sides. Rather than act as a classical transformer where currents ﬂow through all coupled windings at the same time, the ﬂyback transformer is essentially a coupled inductor. It stores energy via magnetic ﬂux in its primary side winding while the switch is conducting. This energy is transferred to the secondary side winding (and load) once the switch is opened.
The topology is based on a buck-boost converter where the buck-boost shared inductor is replaced by the coupled inductor transformer. The operating conditions of a ﬂyback converter are therefore similar to a buck-boost converter, but the transfer function is inﬂuenced not only by the duty cycle but also by the winding turns ratio and parasitic elements.
This example models an ideal DC/DC ﬂyback converter without any parasitic elements and with a duty cycle of 0.5, which provides a unity gain output voltage with the buck-boost converter. With this conﬁguration, the output voltage can be manipulated solely by the choice of winding turns ratio.

## Simulation

Run the simulation with the model as provided and verify that with a DC input of 12 V and a 12:5 transformer winding ratio, the output voltage is 5 V. Then adjust the windings ratio and observe the inﬂuence on the output voltage.
More advanced examples of a ﬂyback converter are given in the demo models Flyback Converter with Analog Controls. and Flyback Converter with Magnetics.
# Buck-Boost Converters

Overview
Electrical model
Simulation

# Overview
This demonstration shows an inverting and a non-inverting buck-boost converter, both unregulated.

![](img/buckboost_fig1.png)
*Figure 1: Buck-boost Converter*

# Electrical model
The buck-boost is a DC/DC converter which can be conﬁgured to produce an output voltage either lower or higher than the input voltage. Two diﬀerent topologies exist: one has an inverting output where the polarity is the opposite the input, and the second maintains the same polarity at the output as at the input.
The inverting buck-boost converter has an ideal transfer function of:
$$
\frac{Vout}{Vin}=\frac{-D}{1-D}
$$

where D is the duty cycle.
This means that a duty cycle value of 0.5 will create a unity gain of 1 with opposite polarity, and values higher or lower than 0.5 will step-up or step-down the output voltage, respectively.
The second topology is essentially a buck converter followed by a boost converter, where a single inductor is shared by both and connects the two in series. The ideal transfer function is the same, except that the non-inverting buck-boost converter will produce an output polarity consistent with that of the input.

# Simulation
The ﬁrst Scope shows the output voltage and the source current. The second Scope shows the temperatures of the semiconductor junctions and case of the combined chopper package. The electrical values reach equilibrium very early on in the simulation while the temperature takes much longer to approach steady-state conditions.

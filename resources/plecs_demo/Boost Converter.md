# Boost Converter

Overview
Model
Simulation
Conclusion

## Overview
This demonstration shows an unregulated boost converter with a resistive load.

## Model
![](img/boost_fig1.png)
 
*Figure 1: Boost converter*

A boost converter is also known as a “step-up” converter since it converts an input DC voltage to a higher DC voltage at its output. It requires at least two semiconductor switches, with a diode and transistor providing the most simple arrangement. An inductor is used to store energy in the form of current at the input, and a capacitor is used as a ﬁltering component to minimize the voltage ripple at the load.
In this model, the load is represented as a resistor and a MOSFET is used to control the current ﬂow through the diode to charge the capacitor. The MOSFET is driven by a ﬁxed duty cycle, with a default value of 0.3, and a switching frequency of 5,000 Hz. With this conﬁguration, the converter operates in continuous conduction mode (CCM), meaning the current in the inductor does not fully discharge when the switch is oﬀ.
The equation for the ideal transfer function of a boost converter operating in CCM is:

$$
\frac{Vout}{Vin}=\frac{1}{1-D}
$$

where D is the duty cycle.
The opposite of CCM is discontinuous conduction mode (DCM), where the current remains at zero at the end of each switching cycle. DCM operation is often a strong candidate for converters that need to operate over a wide range of loads where maintaining a positive inductor current at all times would require either a very high switching frequency (more losses) or a large inductor (more cost). Another example candidate for DCM is a converter that only needs to convert low power levels, as the losses due to the ripple current may not signiﬁcantly inﬂuence the eﬃciency.
DCM has an advantage from a controls perspective in addition to reduced switching losses. Another advantage of DCM operation is that such a design may require cheaper, uni-directional switches only, rather than a FET or IGBT with integrated anti-parallel diode, which could be more expensive. Note that the transfer function of a boost converter operating in DCM is dependent on not only D  , but also L  , Vin  , fsw  , and Iout  , and therefore may be more diﬃcult to regulate.

## Simulation
Run the simulation with the model as provided to view the signals and verify that the load is

$$
Vin\frac{1}{1-D}=10V\frac{1}{1-0.3}=14V
$$

Then change the switching frequency to 3,000 Hz to enter DCM. Notice that while the average output voltage remains the same, the output voltage ripple increases. Generally, such a converter would be designed to minimize output voltage ripple to within a certain limit.
To observe how switching frequency inﬂuences output voltage ripple, increase the value to 50,000 Hz. The inductor current now has less time to charge and discharge per cycle so the ripple magnitude is reduced. Higher switching speeds do create more switching losses, however, so a tradeoﬀ between ripple and eﬃciency must always be considered.

## Conclusion
This model discusses the operation of an open-loop boost converter and suggests ways to interact with the circuit.

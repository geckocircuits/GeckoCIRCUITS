Continuous PID Controller
Purpose
Implementation of a continuous-time controller (P, I, PI, PD or PID)

Library
Control / Continuous

Description
pict
This block implements a highly configurable continuous-time controller with two different anti-windup mechanisms, see subsection Anti-windup methods. The output signal is a weighted sum of at maximum three types of control actions: proportional action, integral action and derivative action. A practical implementation of the derivative action needs an additional first-order low-pass filter. The selection of the filter time constant Kf   is a trade-off between filtering noise and avoiding interactions with the dominant PID controller dynamics. This leads to the transfer function below:

         U (s)        K      K
CPID(s) =---- = Kp + --i+ -1-ds--.
         E(s)         s   Kfs + 1

pict 
Implementation of a PID controller in parallel form

Anti-windup Methods
Set-point changes together with actuator saturation limits can lead to the integrator windup effect and degraded controller performance. To avoid this phenomenon, this component implements the following two mechanisms.

Back-Calculation
pict 
Anti-windup scheme with back-calculation

The Back-calculation method changes the integral action when the controller output is in saturation. The integral term is reduced/increased when the controller output is higher/lower than the upper/lower saturation limit. This is done by feeding back the difference between the saturated and the unsaturated controller output. The value of the back-calculation gain Kbc   determines the rate at which the integrator is reset and is therefore crucial for performance of the anti-windup mechanism. A common choice for this back-calculation gain is:
      ∘ ----
        Kd-
Kbc =   Ki .

However, this rule can only be applied to full PID controllers. In the case of a PI controller, where Kd = 0  , it is suggested that

Kbc = Kp-.
      Ki

Conditional Integration
The Conditional integration method stops the integration when the controller output saturates and the control error e   and the control variable u   have the same sign. This means, the integral action is still applied if it helps to push the controller output out of saturation.

pict 
Anti-windup scheme with conditional integration

Anti-Windup with External Saturation
If the saturation is placed externally to the PID controller block, i.e. Saturation is set to external, one can still use the above described anti-windup methods. To feedback the saturated output of the external Saturation block an additional subsystem port u∗  is made visible if the Saturation parameter is set to external and the Anti-windup method parameters is set to an option other than none, see the figure below.

pict 
Anti-windup scheme with external saturation

Parameters
Basic
Controller type
Specifies the controller type. The controller can be of type P, I, PI, PD or PID.
Parameter source
Specifies whether the controller parameters are provided via the mask parameters (internal) or via input signals (external).
Proportional gain Kp
The proportional gain of the controller. This parameter is shown only if the Controller type parameter is set to P, PI, PD or PID and the Parameter source parameter is set to internal.
Integral gain Ki
The integral gain of the controller. This parameter is shown only if the Controller type parameter is set to I, PI or PID and the Parameter source parameter is set to internal.
Derivative gain Kd
The derivative gain of the controller. This parameter is shown only if the Controller type parameter is set to PD or PID and the Parameter source parameter is set to internal.
Derivative filter coefficient Kf
The filter coefficient which specifies the pole location of the first-order filter in the derivative term. This parameter is shown only if the Controller type parameter is set to PD or PID and the Parameter source parameter is set to internal.
External reset
The behavior of the external reset input. The values rising, falling and either cause a reset of the integrator on the rising, falling or both edges of the reset signal. A rising edge is detected when the signal changes from 0   to a positive value, a falling edge is detected when the signal changes from a positive value to 0  . If level is chosen, the output signal keeps the initial value while the reset input is not 0  . Only the integrator in the integral action is reset.
Initial condition source
Specifies wheter the initial condition is provided via the Initial condition parameter (internal) or via an input signal (external).
Initial condition
The initial condition of the integrator in the integral action. The value may be a scalar or a vector corresponding to the implicit width of the component. This parameter is shown only if the Initial condition source parameter is set to internal.
Anti-Windup
Saturation
Specifies if the internally placed saturation (internal) is used or if the user wants to place the saturation externally (external) to the PID Controller block. If external is selected, the internal Saturation block is not active.
Saturation limits
Specifies whether the saturation limits are provided via the mask parameters (constant) or via input signals (variable).
Upper saturation limit
An upper limit for the output signal. If the value is inf the output signal is unlimited. If input and output are vectorized signals a vector can be used. The number of elements in the vector must match the number of input signals. This parameter is shown only if the Saturation parameter is set to internal and the Saturation limits parameter is set to constant.
Lower saturation limit
A lower limit for the output signal. If the value is -inf the output signal is unlimited. If input and output are vectorized signals a vector can be used. The number of elements in the vector must match the number of input signals. This parameter is shown only if the Saturation parameter is set to internal and the Saturation limits parameter is set to constant.
Anti-Windup method
Specifies the method to avoid windup of the integral action. See Anti-windup methods above.
Back-calculation gain
The gain of the back-calculation anti-windup method. This parameter is shown only of the Anti-windup method parameter is set to Back-calculation.
Probe Signals
Proportional action
Proportion of the proportional action of the controller output signal.
Integral action
Proportion of the integral action of the controller output signal.
Derivative action
Proportion of the derivative action of the controller output signal.
Controller output before saturation
The input signal of the saturation block.
Controller output after saturation
The output signal of the saturation block.
References
A.  Visioli, "Practical PID Control - Advances in industrial control", Springer-Verlag, 2006.
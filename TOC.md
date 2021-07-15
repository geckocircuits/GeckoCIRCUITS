# GeckoCircuits

## What is it?
it a simulator for power electronic circuit, designed by power electronic engineer :)


## How do I run it?
use ./bin_jdk8/run_gecko_with_local_jdk.bat if you want to use the local jdk1.8.0_261
use ./bin_jdk8/run_gecko.bat if you want to use the java version installed in your machine

## How to get started?
There are many example, but really useful are the PDF tutorials:
- [Beginner tutorial](/GeckoCIRCUITStutorials/GeckoCIRCUITS_beginners_tutorial.pdf)
- [Basic tutorial](/GeckoCIRCUITStutorials/GeckoCIRCUITS_tutorial_basics_EN.pdf)

## Examples
### DC/DC topologies
- Buck converter
  - [buck with ideal switch](./resources/Education_ETHZ\ex_1.ipes)
  - ".\resources\education_www.ipes.ethz.ch\buck_simple.ipes"
  - [high side buck with ideal switch](./resources/Education_ETHZ/LESI_R3.ipes)
  - [Quasi resonant buck converter](Education_ETHZ\LESI_R5.ipes)
  - 
- Boost converter
  
- Buck/boost converter
  - [buck-boost simple](".\resources\education_www.ipes.ethz.ch\buckBoost_simple.ipes")
  - [buck-boost with open-loop control and ideal switch](./resources/Topologies/BuckBoost_const_dutyCycle.ipes)
  - [buck-boost with open-loop control with thermal model](./resources/Topologies/BuckBoost_thermal.ipes)

- [Cuk converter](".\resources\education_www.ipes.ethz.ch\cuk_simple.ipes")
- [Sepic converter](".\resources\education_www.ipes.ethz.ch\sepic_simple.ipes")

- [Flyback](): #TODO

- [Flybuck](): #TODO

- [Half bridge](): #TODO

- Full bridge
  - [Unidirectional full bridge with half wave rectifier](./resources/Education_ETHZ\ex_2.ipes)

### AC/DC topologies ()
- [Flyback AC charger Vin=120Vac Vout=100Vdc](./resources/Education_ETHZ\ex_3_pwm.ipes)
- Vienna rectifier
- Swiss rectifier
  - 
- Three phase Six Switches (VSR) rectifier
  - [Three phase VSR rectifier 250kW](./resources/Topologies/three-phase_VSR_simpleControl_250kW.ipes)
  - [Three phase VSR rectifier 10kW with thermal model](./resources/Topologies/ThreePhase-VSR_10kW_thermal.ipes)
  - [Single phase boost control with current control](".\resources\education_www.ipes.ethz.ch\boostPFC_currentControl.ipes")
  - [Single phase boost control with voltage control](".\resources\education_www.ipes.ethz.ch\boostPFC.ipes")

### DC/AC topologies
- [Three phase Inverter](Education_ETHZ\LESI_R7.ipes)
- [Single phase full bridge inverter with hysteretic control mode](".\resources\education_www.ipes.ethz.ch\singlePhase_PWM_converter.ipes")
- 
### AC/AC topologies
- [Three phase Ultra Sparse Matrix Converter](./UltraSparseMatrixConverter.ipes)
- [Three phase Sparse Matrix Converter with thermal model](./three-phase_ACAC_sparsematrixConverter_junction_temperature.ipes)

### Various
- [Gate driver supply with voltage doubler and NE555](Education_ETHZ\LESI_R2.ipes)
- [Series resonant converter with phasor diagram](Education_ETHZ\LESI_R6.ipes)


### Rectifiers
- [Single phase diode rectifier with snubber](".\resources\education_www.ipes.ethz.ch\diode_RL_singlePH_trafo.ipes")
- [Three phase diodes rectifier with snubber](Education_ETHZ\LESI_R4.ipes)
- [Thyristor rectifier with active turnoff?](Education_ETHZ\LESI_R7.ipes)
- [2phaseDiodeBridge_AC-Inductor.ipes](".\resources\education_www.ipes.ethz.ch\2phaseDiodeBridge_AC-Inductor.ipes")
- [2phaseDiodeBridge_DC-Inductor.ipes](".\resources\education_www.ipes.ethz.ch\2phaseDiodeBridge_DC-Inductor.ipes")
- [thyristor_commutation_3ph_trafo.ipes](".\resources\education_www.ipes.ethz.ch\thyristor_commutation_3ph_trafo.ipes")
- [thyristor_freeWheelingDiode.ipes](".\resources\education_www.ipes.ethz.ch\thyristor_freeWheelingDiode.ipes")
- [thyristor_interface_trafo.ipes](".\resources\education_www.ipes.ethz.ch\thyristor_interface_trafo.ipes")
- [thyristor_Jakopovic.ipes](".\resources\education_www.ipes.ethz.ch\thyristor_Jakopovic.ipes")
- [thyristor_lossOfCommutation.ipes](".\resources\education_www.ipes.ethz.ch\thyristor_lossOfCommutation.ipes")
- [thyristor_RL_2phBridge.ipes](".\resources\education_www.ipes.ethz.ch\thyristor_RL_2phBridge.ipes")
- [thyristor_RL_3ph_trafo.ipes](".\resources\education_www.ipes.ethz.ch\thyristor_RL_3ph_trafo.ipes")
- [thyristor_RL_3phBridge.ipes](".\resources\education_www.ipes.ethz.ch\thyristor_RL_3phBridge.ipes")
- [thyristor_RL_single.ipes](".\resources\education_www.ipes.ethz.ch\thyristor_RL_single.ipes")
- [thyristor_RL_singlePh_trafo.ipes](".\resources\education_www.ipes.ethz.ch\thyristor_RL_singlePh_trafo.ipes")

## How can I contribute?
I dont know anything is welcome, any discussion is always helpful :)

## Possible improvements
Designer for power electronic that run in top of Gecko
Open new file in additional windows
Improve gui 
add subblock
add webserver xrpc with binary file
add CISPR25 block 
add EMI filter designer
magnetics designer based on AI?? :)


# Nov. 20, 2009 - Simulating a 5kW Aircraft Power System

Do you work with transformers that have more than two windings? We put an example online, see screenshot and online simulation below, where a 3-three phase interphase transformer is employed.
The rectifier in this example is an interesting topology for aircraft power systems with typical input voltage [98V ... 132V] and a mains frequency [400Hz ... 800Hz]. It's an amazing converter with no active switches (no control electronics) but approximately sinusoidal input currents. You can find more detailed information in this paper http://www.pes.ee.ethz.ch/uploads/tx_ethpublications/gong_APEC04.pdf.

 ![](img/20091120_fig1.png)
 
*Fig. 1: 5kW Aircraft Power System*
 
One can build all types of transformers in GeckoCIRCUITS with the element 'Magnetic Coupling k'. If you have three (or more) windings on a core - like in the example - you need to define one coupling for each inductor combination. This is flexible but often inconvenient. We plan to add a transformer library to GeckoCIRCUITS in a next release.

 ![](img/20091120_fig2.png)
 
*Fig. 2: Waveforms*
 

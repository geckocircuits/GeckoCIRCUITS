# Sept. 8, 2009 - Sunday morning meeting in the laboratory
When a group of Japanese researchers came to Europe to attend the EPE 2009 in Spain, they made a short stop at our power electronics lab (PES) at the ETH Zurich.
Schedule was tight, and we decided to have a breakfast meeting on Sunday morning at 6:00 right in my office. My family still sleeps at this time, so they didn't complain, and our Japanese guests still had their internal clocks set to Japanese time. It was dark outside, I had organized fresh "Gipfli", which is a Swiss kind of pastry, and we had access to the lab's powerful coffee machine.
In the past Prof. Nishida (Chiba University) was in the situation that it was very difficult to find master course students for power electronics (he worked at the Nihon University on the countryside that time, a very beautiful place). Then, in 2007, he started the project "Electric Vehicle": His students had to build a simple electric car with some minor help from industry. This was supposed to be a "fun project" for undergraduates based on well-known concepts, and it worked well.
- The vehicle quickly became a big attraction on the campus
- The project received coverage in newspaper
- The number of students joining his lab increased significantly
And what kind of electrical machine and control did they use? "The students did a great job, especially on project management", Prof. Nishida told us while drinking his second cup of coffee, "and to simplify things, we employed a DC motor with a DC-DC converter. But the next step might be to employ a synchronous machine with dq-control. It will bring benefits like smaller volume but will be much more complicated."

[dq_control_psms](img/20090908_fig1.png)

Now we have implemented blocks to make motor control task easy in GeckoCIRCUITS. Check out the latest example below (we set the new PMSM-example as default). Prof. Nishida employs GeckoCIRCUITS now in his projects especially because simulation is so fast and the software is very simple in usage.
 
Simulation file [](ipes_files/dq_control_psms.ipes)
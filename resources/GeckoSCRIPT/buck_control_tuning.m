Kp = 2.2;
Ki = 0;
Kd = 0;
finished = 0;
Ku = 1;
Tu = 0;

%set LC filter values
 setParameter('C.1','C',100e-9);
 setParameter('L.1','L',300e-6);
 %set switching frequency
 setParameter('SIGNAL.2','f',200e3);
 %set output reference voltage
 setParameter('CONST.1','const',6.0);
 
 %set proportional gain to 1, all other gains to zero
 setParameter('PI.1','r0',Kp);
 setParameter('PI.1','a1',Ki);
 setParameter('PD.1','a1',Kd);
 
 dt = 100e-9;
 tEnd = 40e-3;
 simulatedTime = 0;
 initSimulation(dt,tEnd);
 
  %in a loop, increase proportional gain by 0.1 until output voltage oscillates at a constant frequency and amplitude
 while (finished == 0)
     %run the simulation
 	 fprintf(1,'Running simulation with Kp = %f\n',Kp);
 	 simulateTime(2e-3);
     simulatedTime = simulatedTime + 2e-3;
 	 %now check through output of Java block in model, whether amplitude and period are constant
 	 amplitude_constant = getOutput('JAVA_FUNCTION.5','1');
 	 period_constant = getOutput('JAVA_FUNCTION.5','3');
 	 if (amplitude_constant == 1 && period_constant == 1) %constant oscillation, can calculate PID parameters
 	 
 	 	 fprintf(1,'Loop output (Vout) oscillating at constant amplitude and frequency.\n');
 	 	 Ku = getParameter('PI.1','r0'); %get ultimate gain
 	 	 Tu = getOutput('JAVA_FUNCTION.5','2'); %get ultimate period
 	 	 fprintf(1,'Ultimate gain is Ku = %f\n',Ku);
 	 	 fprintf(1,'Ultimate period is Tu = %f\n',Tu);
 	 	 finished = 1;
     else %not constant oscillation, increase Kp by 0.1, go on
 	 
 	 	 Kp = Kp + 0.1;
 	 	 fprintf(1,'Loop not oscillating at constant amplitude and frequency.\n');
 	 	 setParameter('PI.1','r0',Kp);
     end
 end
 
 if (simulatedTime ~= tEnd)
     simulateTime(tEnd - simulatedTime)
 end
 
 endSimulation();
 
 %now calculate and set PID coefficients according to Ziegler-Nichols method
 Kp = 0.6*Ku;
 Ki = 2*Kp/Tu;
 Kd = Kp*Tu/8;
 
 setParameter('PI.1','r0',Kp);
 setParameter('PI.1','a1',Ki);
 setParameter('PD.1','a1',Kd);
 
 %now run simulation again to see results
 runSimulation();
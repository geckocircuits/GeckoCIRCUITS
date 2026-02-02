%Script for GeckoCIRCUITS simulation determining maximum RMS current for
%NPC inverter output stage for a given switching frequency and output
%voltage frequency

%import package with functions for control of GeckoCIRCUITS
%import('gecko.GeckoExternal.*');

%start the program
%startGui();

%load the file with the model
%openFile('outputstagemodel.ipes');

% Output frequency to sweep over (Hz)
f_mn = [10 25 50 65];

%Switching frequency to sweep over (Hz)
f_sk = [10 20 50 75 100]*1e3;

%Minimum L value (H) for particular switching frequency
Lmin = [468 234 94 63 47]*1e-6;

%Minimum C value (F) for particular switching frequency
Cmin = [28 15 7 5 5]*1e-6;

%Maximum junction temperature (C)
T_j_max = 130;

%Maximal output current (A rms)
I_max = zeros(length(f_mn),length(f_sk));

%now in a loop, sweep over all switching frequencies, and for each over all
%output frequencies
for j = 1:length(f_sk)
    
    %set switching frequency
    fs = f_sk(j);
    setParameter('CONST.1','const',fs);
    %set LC filter
    L = Lmin(j);
    C = Cmin(j);
    setParameter('L.1','L',L);
    setParameter('C.1','C',C);
    
    %now sweep over all output frequencies at this switching frequency
    for i = 1:length(f_mn)
        
        %set output frequency - both in carrier signal generator and output
        %current source
        fout = f_mn(i);
        setParameter('SIGNAL.1','f',fout);
        setParameter('I.1','f',fout);
        %set simulation time so that Tj settles to steady state -> pick 20
        %output cycles
        set_Tend(20*(1/fout));
        %now start simulating at one output current, and increase until
        %simulated junction temperature hits Tjmax
        Tj_simulated = 0;
        Iout_rms = 0;
        while (Tj_simulated < T_j_max)
            Iout_rms = Iout_rms + 5;
            %set output current
            Iout_max = sqrt(2)*Iout_rms;
            setParameter('I.1','iMAX',Iout_rms);
            %run the simulation in GeckoCIRCUITS
            runSimulation();
            %fetch output of Java block which records maximum Tj
            Tj_simulated = getOutput('JAVA_FUNCTION.3','0');     
        end
        %record maxium RMS output current
        I_max(i,j) = Iout_rms;
        
    end
    
end

figure(1)
plot(f_mn, I_max(:,1), f_mn, I_max(:,2), f_mn, I_max(:,3), f_mn, I_max(:,4), f_mn, I_max(:,5));
xlabel('Output frequency [Hz]');
ylabel('Max. rms current [A]');
legend('f_s = 10kHz', 'f_s = 20kHz', 'f_s = 50kHz', 'f_s = 75kHz', 'f_s = 100kHz');
axis([0 65 0 300]);
grid on;
box on;

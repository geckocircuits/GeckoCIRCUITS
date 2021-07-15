%load the design file
geckom.loadDesignFile('C:/tutorial/TutorialDesign2.gmd');
%create array to store results
Loads = [20; 50; 80; 100];
LoadsR = [20; 8; 5; 4];
LossesByLoad = zeros(4);
%load the circuit simulation file
geckom.loadCircuitsSimulationFile('C:/tutorial/tutorialBuck.ipes')

%now execute different load levels in loop
for n = 1:4
    %get current R value
    R = LoadsR(n);
    %send to GeckoCIRCUITS
    geckoc.setParameter('R.1','R',R);
    %set simulation parameters
    geckom.setCircuitsSimulationParameters(15,'L.1','VL','IL',1e-9,1e-3,200e3,200e4)
    %set waveform type
    geckom.setWaveformType('PIECEWISE_LINEAR');
    %set cooling
    geckom.setCooling(true,false,'Top-Down',45,'bottom');
    %run simulations
    geckom.evaluateComponent();
    %extract losses
    LossesByLoad(n) = geckom.getTotalLosses();
end

%plot results
plot(Loads,LossesByLoad);
title('Inductor Losses vs. Converter Output Load');
xlabel('Load Part (%)');
ylabel('Total Losses (W)');
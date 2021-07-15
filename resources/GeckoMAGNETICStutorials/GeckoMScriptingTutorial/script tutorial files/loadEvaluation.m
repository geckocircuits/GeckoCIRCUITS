%load the design file
geckom.loadDesignFile('C:/tutorial/TutorialDesign2.gmd');
%create array to store results
Loads = zeros(4);
LossesByLoad = zeros(4);
%load first waveform file
geckom.loadWaveformFile('C:/tutorial/TutorialWaveform20pctLoad.gmw');
%set cooling
geckom.setCooling(true,false,'Top-Down',45,'bottom');
%simulate
geckom.evaluateComponent();
%store results
Loads(1) = 20;
LossesByLoad(1) = geckom.getTotalLosses();
%load second waveform file
geckom.loadWaveformFile('C:/tutorial/TutorialWaveform50pctLoad.gmw');
%set cooling
geckom.setCooling(true,false,'Top-Down',45,'bottom');
%simulate
geckom.evaluateComponent();
%store results
Loads(2) = 50;
LossesByLoad(2) = geckom.getTotalLosses();
%load third waveform file
geckom.loadWaveformFile('C:/tutorial/TutorialWaveform80pctLoad.gmw');
%set cooling
geckom.setCooling(true,false,'Top-Down',45,'bottom');
%simulate
geckom.evaluateComponent();
%store results
Loads(3) = 80;
LossesByLoad(3) = geckom.getTotalLosses();
%load fourth waveform file
geckom.loadWaveformFile('C:/tutorial/TutorialWaveform100pctLoad.gmw');
%set cooling
geckom.setCooling(true,false,'Top-Down',45,'bottom');
%simulate
geckom.evaluateComponent();
%store results
Loads(4) = 100;
LossesByLoad(4) = geckom.getTotalLosses();
%plot results
plot(Loads,LossesByLoad);
title('Inductor Losses vs. Converter Output Load');
xlabel('Load Part (%)');
ylabel('Total Losses (W)');
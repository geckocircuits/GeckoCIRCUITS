% GECKO2OCTAVE
%
%   Runs the Gecko simulation filename and sets the global parameter_names to parameter_values.
%
%   The inputs to a scope named OUTSCOPE in the simulation are returned in the matrix waveforms.
%   All input terminals of the scope must be labelled.
%
%
function [t, waveforms] = gecko2octave(filename, parameter_names, parameter_values, gecko_path)
  
  javaaddpath(gecko_path)

  gesim = javaObject("gecko.GeckoRemote");
  
  try
    gesim.startGui(43035);
  catch
    
  end
  
  try
    gesim.connectToGecko(43035);
  catch
   
  end
  
  try
    gesim.openFile(filename);
  catch
    gesim.shutdown();
    error('Could not open file.');
  end
  
  try 
    for k = 1:length(parameter_names)
      gesim.setGlobalParameterValue(parameter_names{k},parameter_values(k));
    end
  catch
    gesim.shutdown();
    error('Could not set parameters.');
  end

  try
    gesim.runSimulation();
  catch
    gesim.shutdown();
    error('Could not run simulation.');
  end
  
  try
    Tsim = gesim.getSimulationTime();
    Nw = gesim.getParameter("OUTSCOPE","numberInputTerminals");
    waveforms = [];
    for k = 1:Nw
      labelname = gesim.getInputNodeName("OUTSCOPE",k-1);
      waveforms = [waveforms gesim.getSignalData(labelname,0,Tsim,1)];
    end
  catch
    gesim.shutdown();
    error('Could not read waveforms.');
  end
  
  gesim.shutdown();
  
  waveforms = rot90(waveforms);
  [rows,cols] = size(waveforms);
  t = linspace(0,Tsim,cols);
  waveforms = [t;waveforms];
  
end
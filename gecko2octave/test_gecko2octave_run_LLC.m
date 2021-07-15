gecko_path='../bin_jdk8/GeckoCIRCUITS.jar'
filename = './LLC.ipes'
parameter_names = ''
parameter_names = ''

[t, waveforms] = gecko2octave(filename, parameter_names, parameter_names, gecko_path);
plot(t, waveforms(2, :)) 
plot(t, waveforms(3, :)) 
plot(t, waveforms(4, :)) 
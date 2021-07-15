
% linux 64-bit
%mex s_GeckoCIRCUITS.c /usr/lib/jvm/java-6-sun-1.6.0.24/jre/lib/amd64/server/libjvm.so  -I/usr/lib/jvm/java-6-sun-1.6.0.24/include -I/usr/lib/jvm/java-6-sun-1.6.0.24/include/linux/ -I/usr/local/MATLAB/R2010b/simulink/include/

% linux 32-bit
%mex s_GeckoCIRCUITS.c /usr/local/jdk1.6.0_20/jre/lib/i386/server/libjvm.so -I/usr/local/jdk1.6.0_20/include/ -I/usr/local/jdk1.6.0_20/include/linux/ -I/home/andy/Software/Matlab/simulink/include/


% windows 32-bit
mex s_GeckoCIRCUITS.c C:/Program' Files'/Java/jdk1.6.0_20/lib/jvm.lib  -IC:/Program' Files'/Java/jdk1.6.0_20/include/ -IC:/Program' Files'/Java/jdk1.6.0_20/include/win32/  -IC:/Program' Files'/MatLAB/R2010a/simulink/include/

% windows 64-bit (vmware)
%mex s_GeckoCIRCUITS.c C:/Program' Files'/Java/jdk1.6.0_23/lib/jvm.lib  -IC:/Program' Files'/Java/jdk1.6.0_23/include/ -IC:/Program' Files'/Java/jdk1.6.0_23/include/win32/  -IC:/Program' Files'/MATLAB/R2010b/simulink/include/
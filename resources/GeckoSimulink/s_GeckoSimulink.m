
function setup(block)

openGeckoCIRCUITS()

testexists = evalin('base', 'exist(''GeckoCIRCUITS'')');

if testexists < 1
    return;
end

qq= evalin('base','GeckoCIRCUITS'); 
stopTime = get_param(gcs, 'StopTime');
qq.external_init( str2double(stopTime) ); 
% Register number of ports
block.NumInputPorts  = 1;
block.NumOutputPorts = 1;

% Setup port properties to be inherited or dynamic
block.SetPreCompInpPortInfoToDynamic;
block.SetPreCompOutPortInfoToDynamic;

% Override input port properties
block.InputPort(1).Dimensions        = qq.external_getTerminalNumber_FROM_EXTERNAL();
block.InputPort(1).DatatypeID  = -1; %inherited  0; % double
block.InputPort(1).Complexity  = 'Real';
block.InputPort(1).DirectFeedthrough = false;

% Override output port properties 
block.OutputPort(1).Dimensions       = qq.external_getTerminalNumber_TO_EXTERNAL(); 
block.OutputPort(1).DatatypeID  = 0; % double
block.OutputPort(1).Complexity  = 'Real';

% Register parameters
block.NumDialogPrms     = 0;

% Register sample times
%  [0 offset]            : Continuous sample time
%  [positive_num offset] : Discrete sample time
%
%  [-1, 0]               : Inherited sample time
%  [-2, 0]               : Variable sample time
block.SampleTimes = [qq.external_getdt() 0];

% Specify the block simStateCompliance. The allowed values are:
%    'UnknownSimState', < The default setting; warn and assume
%    DefaultSimState
%    'DefaultSimState', < Same sim state as a built-in block
%    'HasNoSimState',   < No sim state
%    'CustomSimState',  < Has GetSimState and SetSimState methods
%    'DisallowSimState' < Error out when saving or restoring the model sim state
block.SimStateCompliance = 'DefaultSimState';

%% -----------------------------------------------------------------
%% The M-file S-function uses an internal registry for all
%% block methods. You should register all relevant methods
%% (optional and required) as illustrated below. You may choose
%% any suitable name for the methods and implement these methods
%% as local functions within the same file. See comments
%% provided for each function for more information.
%% -----------------------------------------------------------------

%%block.RegBlockMethod('PostPropagationSetup',    @DoPostPropSetup);
%%block.RegBlockMethod('InitializeConditions', @InitializeConditions);
%%block.RegBlockMethod('Start', @Start);
block.RegBlockMethod('Outputs', @Outputs);     % Required
block.RegBlockMethod('Update', @Update);
%%block.RegBlockMethod('Derivatives', @Derivatives);
block.RegBlockMethod('Terminate', @Terminate); % Required

%end setup

function openGeckoCIRCUITS()

testexists = evalin('base', 'exist(''GeckoCIRCUITS'')');
if testexists < 1
    GeckoCIRCUITSPath = get_param(gcb, 'GeckoCIRCUITSPath');
    if exist(GeckoCIRCUITSPath)
        javaaddpath(GeckoCIRCUITSPath)    
        evalin('base','global GeckoCIRCUITS');  
        openString = ['GeckoCIRCUITS = gecko.GeckoSimulink('''  get_param(gcb, 'GeckoModelPath')  ''')']    
        evalin('base',openString);
    end
end


function Update(block)
    qq= evalin('base','GeckoCIRCUITS'); 
    block.OutputPort(1).Data = qq.external_step(block.CurrentTime ,block.InputPort(1).Data);   
%endfunction



function Outputs(block)
    %qq= evalin('base','GeckoCIRCUITS'); 
    %block.OutputPort(1).Data = qq.external_step(block.CurrentTime ,block.InputPort(1).Data);   
%endfunction

%%
%% Terminate:
%%   Functionality    : Called at the end of simulation for cleanup
%%   Required         : Yes
%%   C-MEX counterpart: mdlTerminate
%%
function Terminate(block)
    qq= evalin('base','GeckoCIRCUITS'); 
    qq.external_end(); 
%end Terminate

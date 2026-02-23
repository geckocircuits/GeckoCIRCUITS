@echo off
REM GeckoCIRCUITS Launcher for Windows
REM
REM Usage:
REM   run-gecko.bat                    - Start GeckoCIRCUITS
REM   run-gecko.bat circuit.ipes       - Open a circuit file
REM   run-gecko.bat --hidpi            - Start with HiDPI scaling (4K displays)
REM   run-gecko.bat --hidpi circuit.ipes

setlocal enabledelayedexpansion

REM Script directory
set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."

REM JAR file location
set "JAR_FILE=%PROJECT_DIR%\target\gecko-1.0-jar-with-dependencies.jar"

REM Default JVM options
set "JVM_OPTS=-Xmx3G -Dpolyglot.js.nashorn-compat=true"

REM Parse arguments
set "CIRCUIT_FILE="
set "HIDPI="

:parse_args
if "%~1"=="" goto done_args
if /i "%~1"=="--hidpi" (
    set "HIDPI=1"
    shift
    goto parse_args
)
if /i "%~1"=="-h" (
    goto show_help
)
if /i "%~1"=="--help" (
    goto show_help
)
REM Assume it's a circuit file
set "CIRCUIT_FILE=%~1"
shift
goto parse_args

:done_args

REM Add HiDPI scaling if requested
if defined HIDPI (
    set "JVM_OPTS=%JVM_OPTS% -Dsun.java2d.uiScale=2"
)

REM Check if JAR exists
if not exist "%JAR_FILE%" (
    echo Error: JAR file not found at %JAR_FILE%
    echo.
    echo Please build the project first:
    echo   cd %PROJECT_DIR%
    echo   mvn clean package assembly:single -DskipTests
    exit /b 1
)

REM Check Java installation
where java >nul 2>&1
if errorlevel 1 (
    echo Error: Java not found in PATH
    echo Please install Java 21 or later
    exit /b 1
)

REM Display startup info
echo ============================================
echo GeckoCIRCUITS Launcher
echo ============================================
echo JAR: %JAR_FILE%
if defined HIDPI echo HiDPI: enabled
if defined CIRCUIT_FILE echo Circuit: %CIRCUIT_FILE%
echo.

REM Run GeckoCIRCUITS
if defined CIRCUIT_FILE (
    java %JVM_OPTS% -jar "%JAR_FILE%" "%CIRCUIT_FILE%"
) else (
    java %JVM_OPTS% -jar "%JAR_FILE%"
)
goto :eof

:show_help
echo GeckoCIRCUITS Launcher for Windows
echo.
echo Usage:
echo   run-gecko.bat [options] [circuit.ipes]
echo.
echo Options:
echo   --hidpi     Enable HiDPI scaling for 4K displays
echo   -h, --help  Show this help message
echo.
echo Examples:
echo   run-gecko.bat                        Start GeckoCIRCUITS
echo   run-gecko.bat my_circuit.ipes        Open a circuit file
echo   run-gecko.bat --hidpi                Start with HiDPI scaling
echo   run-gecko.bat --hidpi circuit.ipes   HiDPI with circuit file
exit /b 0

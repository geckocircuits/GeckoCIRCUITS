@echo off
REM Run script for GeckoCIRCUITS

set JAR_FILE=target\gecko-1.0-jar-with-dependencies.jar

if not exist "%JAR_FILE%" (
    echo ===============================================
    echo ERROR: JAR file not found!
    echo Please build the project first using build.bat
    echo ===============================================
    pause
    exit /b 1
)

echo ===============================================
echo Starting GeckoCIRCUITS...
echo ===============================================

java "-Xmx3G" "-Dpolyglot.js.nashorn-compat=true" -jar "%JAR_FILE%" %*

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ===============================================
    echo GeckoCIRCUITS exited with an error.
    echo ===============================================
    pause
)

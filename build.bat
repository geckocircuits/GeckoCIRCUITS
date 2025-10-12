@echo off
REM Build script for GeckoCIRCUITS

echo ===============================================
echo Building GeckoCIRCUITS...
echo ===============================================

mvn clean package assembly:single

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ===============================================
    echo Build successful!
    echo JAR file: target\gecko-1.0-jar-with-dependencies.jar
    echo ===============================================
    echo.
    echo To run: run.bat
) else (
    echo.
    echo ===============================================
    echo Build failed! Please check the error messages above.
    echo ===============================================
)

pause

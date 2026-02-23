@echo off
REM Build GeckoCIRCUITS distribution packages
REM
REM Usage:
REM   _build\build-distributions.bat          Build all distributions
REM   _build\build-distributions.bat windows  Build Windows only
REM   _build\build-distributions.bat examples Build examples only
REM
REM Output: target\

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."

cd /d "%PROJECT_DIR%"

REM Parse arguments
set "TARGETS=%~1"
if "%TARGETS%"=="" set "TARGETS=all"

echo ============================================
echo GeckoCIRCUITS Distribution Builder
echo ============================================
echo.

if "%TARGETS%"=="all" (
    echo Building all distributions...
    call mvn clean package -Pdist-all -DskipTests
) else if "%TARGETS%"=="windows" (
    echo Building Windows distribution...
    call mvn clean package -Pdist-windows -DskipTests
) else if "%TARGETS%"=="linux" (
    echo Building Linux distribution...
    call mvn clean package -Pdist-linux -DskipTests
) else if "%TARGETS%"=="macos" (
    echo Building macOS distribution...
    call mvn clean package -Pdist-macos -DskipTests
) else if "%TARGETS%"=="wsl" (
    echo Building WSL distribution...
    call mvn clean package -Pdist-wsl -DskipTests
) else if "%TARGETS%"=="examples" (
    echo Building Examples distribution...
    call mvn clean package -Pdist-examples -DskipTests
) else (
    echo Unknown target: %TARGETS%
    echo Valid targets: all, windows, linux, macos, wsl, examples
    exit /b 1
)

echo.
echo ============================================
echo Distribution packages created:
echo ============================================
dir target\GeckoCIRCUITS-*.zip 2>nul

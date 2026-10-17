@echo off
rem Builds a Windows installer (.exe; needs the WiX Toolset for jpackage) from the runnable jar.
setlocal
cd /d "%~dp0"
call mvn -B clean package || exit /b 1
jpackage --name CleanCheck --app-version 1.0 --input target --main-jar cleancheck.jar --main-class com.mycompany.cleancheck.Launcher --dest target\installer --win-menu --win-shortcut || exit /b 1
echo Installer written to target\installer

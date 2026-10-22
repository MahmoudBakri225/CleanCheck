@echo off
cd /d "%~dp0.."
call run-gui.bat
exit /b %errorlevel%

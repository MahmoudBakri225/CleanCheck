@echo off
setlocal
cd /d "%~dp0"
if exist target\cleancheck.jar (
  java -jar target\cleancheck.jar gui
  exit /b %errorlevel%
)
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven was not found in PATH.
  echo Install Maven 3.9+ and add it to PATH, then run this file again.
  pause
  exit /b 1
)
call mvn clean javafx:run
pause

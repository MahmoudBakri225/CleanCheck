@echo off
REM Demo launcher — set JAVA_HOME to JDK 26 if needed
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Apache NetBeans\jdk"
cd /d "%~dp0.."
call "%JAVA_HOME%\bin\java.exe" -version
mvn -q javafx:run

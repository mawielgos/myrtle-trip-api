@echo off
setlocal
cd /d "%~dp0.."

set "EVENT_MANAGER_JAVA_HOME=C:\Program Files\Java\jdk-21.0.10"

if not exist "%EVENT_MANAGER_JAVA_HOME%\bin\java.exe" (
  echo ERROR: Golf Event Manager requires Java 21.
  echo Expected JDK: "%EVENT_MANAGER_JAVA_HOME%"
  echo Update EVENT_MANAGER_JAVA_HOME in scripts\test-backend.cmd if Java 21 moves.
  exit /b 1
)

set "JAVA_HOME=%EVENT_MANAGER_JAVA_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Using Java from %JAVA_HOME%
call mvnw.cmd test
exit /b %ERRORLEVEL%

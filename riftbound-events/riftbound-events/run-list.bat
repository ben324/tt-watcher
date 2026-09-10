@echo off
setlocal
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out ^
  src\com\riftbound\events\*.java ^
  src\com\riftbound\events\json\*.java ^
  src\com\riftbound\events\uvs\*.java ^
  src\com\riftbound\events\playriftbound\*.java
if errorlevel 1 exit /b 1
java -cp out com.riftbound.events.ListEvents %*

@echo off
setlocal
cd /d "%~dp0"
set LIB_SRC=..\riftbound-events\riftbound-events\src
if not exist "%LIB_SRC%\com\riftbound\events" (
  echo Expected library sources at %LIB_SRC%
  exit /b 1
)
if not exist out mkdir out
javac -encoding UTF-8 -d out ^
  %LIB_SRC%\com\riftbound\events\*.java ^
  %LIB_SRC%\com\riftbound\events\json\*.java ^
  %LIB_SRC%\com\riftbound\events\uvs\*.java ^
  %LIB_SRC%\com\riftbound\events\playriftbound\*.java ^
  src\com\riftbound\internal\*.java
if errorlevel 1 exit /b 1
java -cp out com.riftbound.internal.InternalMain %*

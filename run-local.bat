@echo off
setlocal
cd /d "%~dp0"

if not exist "riftbound-internal\run-internal.bat" (
  echo Missing riftbound-internal\run-internal.bat
  exit /b 1
)
if not exist "riftbound-api\run-api.bat" (
  echo Missing riftbound-api\run-api.bat
  exit /b 1
)

echo Starting riftbound-internal on http://127.0.0.1:8081
start "riftbound-internal" cmd /k "cd /d "%~dp0riftbound-internal" && run-internal.bat"

echo Waiting for internal to bind...
timeout /t 2 /nobreak >nul

echo Starting riftbound-api on http://127.0.0.1:8080
start "riftbound-api" cmd /k "cd /d "%~dp0riftbound-api" && run-api.bat"

echo.
echo Two windows should stay open:
echo   riftbound-internal  http://127.0.0.1:8081
echo   riftbound-api       http://127.0.0.1:8080
echo.
echo Open http://127.0.0.1:8080/ in a browser.
echo Close those windows to stop the servers.

@echo off
echo Starting Subscription Tracker Frontend...
echo.
cd /d "%~dp0frontend"
if not exist node_modules (
    echo Installing dependencies...
    call npm install
)
call npm run dev
pause

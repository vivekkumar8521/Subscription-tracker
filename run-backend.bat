@echo off
echo Starting Subscription Tracker Backend...
echo.
cd /d "%~dp0backend"
call mvnw.cmd spring-boot:run
pause

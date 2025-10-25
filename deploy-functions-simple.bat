@echo off
echo ========================================
echo    Firebase Functions Deployment
echo ========================================
echo.

echo Step 1: Navigating to functions directory...
cd functions
if %errorlevel% neq 0 (
    echo Error: functions directory not found!
    pause
    exit /b 1
)

echo.
echo Step 2: Deploying functions...
firebase deploy --only functions
if %errorlevel% neq 0 (
    echo Error deploying functions.
    pause
    exit /b 1
)

echo.
echo ========================================
echo    SUCCESS! Functions Deployed!
echo ========================================
echo.
echo Your push notifications are now working!
echo.
echo Next steps:
echo 1. Build and run your Android app
echo 2. Sign in to your app
echo 3. Test by creating a new page in a board
echo 4. Subscribers will get push notifications automatically!
echo.
pause

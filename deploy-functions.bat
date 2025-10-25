@echo off
echo ========================================
echo    Firebase Functions Deployment
echo ========================================
echo.

echo Step 1: Installing Firebase CLI...
npm install -g firebase-tools
if %errorlevel% neq 0 (
    echo Error installing Firebase CLI. Please run as Administrator.
    pause
    exit /b 1
)

echo.
echo Step 2: Please login to Firebase...
firebase login
if %errorlevel% neq 0 (
    echo Error logging in to Firebase.
    pause
    exit /b 1
)

echo.
echo Step 3: Navigating to functions directory...
cd functions
if %errorlevel% neq 0 (
    echo Error: functions directory not found!
    pause
    exit /b 1
)

echo.
echo Step 4: Installing dependencies...
npm install
if %errorlevel% neq 0 (
    echo Error installing dependencies.
    pause
    exit /b 1
)

echo.
echo Step 5: Building functions...
npm run build
if %errorlevel% neq 0 (
    echo Error building functions.
    pause
    exit /b 1
)

echo.
echo Step 6: Deploying functions...
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
echo Test by creating a new page in your app.
echo.
pause




@echo off
echo ========================================
echo    Deploying SECURE Firebase Functions
echo ========================================
echo.

echo Step 1: Backing up current functions...
copy functions\src\index.ts functions\src\index-backup.ts

echo Step 2: Replacing with secure functions...
copy functions\src\index-secure.ts functions\src\index.ts

echo Step 3: Building functions...
cd functions
npm run build

echo Step 4: Deploying secure functions...
firebase deploy --only functions

echo.
echo ========================================
echo    SECURE Functions Deployed!
echo ========================================
echo.
echo Security Benefits:
echo - Zero subscriber data exposure to board owners
echo - No sensitive data in Android app logs
echo - Server-side processing only
echo - Better privacy protection
echo.
pause

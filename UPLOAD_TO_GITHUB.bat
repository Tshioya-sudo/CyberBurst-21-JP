@echo off
setlocal
cd /d "%~dp0"

echo ==========================================
echo   GitHub Upload Assistant for CyberBurst
echo ==========================================
echo.

:: 1. Initialize Git
if not exist ".git" (
    echo [1/5] Initializing Git repository...
    git init
) else (
    echo [1/5] Git repository already initialized.
)

:: 2. Configure User (if needed - basic check)
git config user.email >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [!] Git user not configured.
    set /p GIT_EMAIL="Enter your email for Git: "
    set /p GIT_NAME="Enter your name for Git: "
    git config user.email "%GIT_EMAIL%"
    git config user.name "%GIT_NAME%"
)

:: 3. Add Files
echo.
echo [2/5] Adding files...
git add .
git commit -m "Initial commit of CyberBurst-21-JP"

:: 4. Branch Rename
git branch -M main

:: 5. Remote Setup
echo.
echo [3/5] Setting up Remote...
echo.
echo.
set REPO_URL=https://github.com/Tshioya-sudo/CyberBurst-21-JP.git
echo   Target Repository: %REPO_URL%
echo   Make sure you have created this repository on GitHub!
echo -----------------------------------------------------------

git remote add origin %REPO_URL%
:: If remote already exists, set url
git remote set-url origin %REPO_URL%

:: 6. Push
echo.
echo [4/5] Uploading to GitHub...
git push -u origin main

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Upload Complete!
    echo You can now go to Render.com and deploy this repository.
) else (
    echo.
    echo [ERROR] Upload failed. Please check your URL or internet connection.
    echo If a browser window opened to sign in, please complete the sign in.
)

pause

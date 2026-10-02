@echo off
setlocal

set "LOG=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\build\github_push.log"
set "GIT=C:\Program Files\Git\bin\git.exe"
set "GH=C:\Program Files\GitHub CLI\gh.exe"
set "PROJ=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics"

echo === GitHub Push === > "%LOG%"

:: Verify tools
"%GIT%" --version >> "%LOG%" 2>&1
if errorlevel 1 ( echo Git not found at expected path >> "%LOG%" & exit /b 1 )

"%GH%" --version >> "%LOG%" 2>&1
if errorlevel 1 ( echo GH CLI not found at expected path >> "%LOG%" & exit /b 1 )

:: Check gh auth status
echo --- Auth status --- >> "%LOG%"
"%GH%" auth status >> "%LOG%" 2>&1

echo Done checking >> "%LOG%"
exit /b 0

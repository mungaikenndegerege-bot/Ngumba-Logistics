@echo off
set "GIT=C:\Program Files\Git\bin\git.exe"
set "GH=C:\Program Files\GitHub CLI\gh.exe"
set "LOG=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\build\gh_check.log"

echo === Checking tools === > "%LOG%"
"%GIT%" --version >> "%LOG%" 2>&1
"%GH%" --version >> "%LOG%" 2>&1
echo --- Auth status --- >> "%LOG%"
"%GH%" auth status >> "%LOG%" 2>&1
echo Done >> "%LOG%"
exit /b 0

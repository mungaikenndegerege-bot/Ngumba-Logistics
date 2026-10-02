@echo off
setlocal
set "GIT=C:\Program Files\Git\bin\git.exe"
set "PROJ=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics"
set "TOKEN=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"
set "USER=mungaikenndegerege-bot"
set "REPO=Ngumba-Logistics"
set "LOG=%PROJ%\build\git_push.log"

echo === Git push === > "%LOG%"
cd /d "%PROJ%"

:: Ensure remote uses token
"%GIT%" remote set-url origin "https://%USER%:%TOKEN%@github.com/%USER%/%REPO%.git" >> "%LOG%" 2>&1

:: Stage all changed files
"%GIT%" add . >> "%LOG%" 2>&1
"%GIT%" status >> "%LOG%" 2>&1

:: Commit
"%GIT%" commit -m "feat: add Railway deploy config, PORT and DATA_DIR env var support" >> "%LOG%" 2>&1

:: Push
"%GIT%" push origin main >> "%LOG%" 2>&1
if errorlevel 1 (
    echo PUSH FAILED >> "%LOG%"
) else (
    echo PUSH SUCCESS >> "%LOG%"
)

:: Strip token from remote URL after push
"%GIT%" remote set-url origin "https://github.com/%USER%/%REPO%.git" >> "%LOG%" 2>&1
echo Remote URL cleaned >> "%LOG%"
echo === DONE === >> "%LOG%"
exit /b 0

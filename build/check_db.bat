@echo off
setlocal
set "LOG=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\build\db_check.log"
set "JAVA=C:\Program Files\Java\jdk1.8.0_202\bin\java.exe"
set "JAR=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\dist\NgumbaWeb.jar"

:: Check what's in the dat files using Java to read the class headers
echo Checking data files... > "%LOG%"

:: Check size of both dat files
echo ngumba_fleet.dat: >> "%LOG%"
dir "c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\ngumba_fleet.dat" >> "%LOG%" 2>&1

echo ngumba_fleet_app.dat: >> "%LOG%"
dir "c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\ngumba_fleet_app.dat" >> "%LOG%" 2>&1

:: Dump first bytes of ngumba_fleet_app.dat to see the serialized class name
echo First 200 bytes hex of ngumba_fleet_app.dat: >> "%LOG%"
powershell -Command "$bytes = [System.IO.File]::ReadAllBytes('c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\ngumba_fleet_app.dat'); [System.Text.Encoding]::ASCII.GetString($bytes) -replace '[^\x20-\x7E]','.' | Write-Host" >> "%LOG%" 2>&1

echo Done >> "%LOG%"
exit /b 0

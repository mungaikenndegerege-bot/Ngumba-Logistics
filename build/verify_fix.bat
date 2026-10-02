@echo off
setlocal
set "LOG=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\build\verify_fix.log"
echo === VERIFY FIX === > "%LOG%"
echo. >> "%LOG%"

:: 1. Check port 7070 is listening
echo --- Check server up --- >> "%LOG%"
powershell -Command "try { $c=[Net.Sockets.TcpClient]::new(); $c.Connect('localhost',7070); Write-Host 'PORT 7070 OPEN'; $c.Dispose() } catch { Write-Host 'PORT 7070 CLOSED' }" >> "%LOG%" 2>&1

:: 2. Add a truck
echo --- POST truck KBZ 999Z --- >> "%LOG%"
powershell -Command "try { $r=Invoke-WebRequest -Uri 'http://localhost:7070/api/trucks' -Method POST -Body '{\"plate\":\"KBZ 999Z\",\"model\":\"HOWO 440\",\"driver\":\"Test Driver\",\"nextService\":\"2027-06-01\",\"insuranceExpiry\":\"2027-12-01\"}' -ContentType 'application/json' -UseBasicParsing; Write-Host \"STATUS $($r.StatusCode): $($r.Content)\" } catch { Write-Host \"FAIL: $_\" }" >> "%LOG%" 2>&1

:: 3. Read trucks back
echo --- GET trucks (should have KBZ 999Z) --- >> "%LOG%"
powershell -Command "(Invoke-WebRequest -Uri 'http://localhost:7070/api/trucks' -UseBasicParsing).Content" >> "%LOG%" 2>&1

:: 4. Confirm dat file was created beside the JAR
echo --- Check dat file location --- >> "%LOG%"
if exist "c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\dist\ngumba_fleet_app.dat" (
    echo DAT FILE EXISTS at dist\ngumba_fleet_app.dat >> "%LOG%"
    dir "c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\dist\ngumba_fleet_app.dat" >> "%LOG%"
) else (
    echo DAT FILE NOT FOUND at dist\ - checking other locations... >> "%LOG%"
    dir "c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\*.dat" >> "%LOG%" 2>&1
    dir "c:\Users\user\*.dat" >> "%LOG%" 2>&1
)

echo === DONE === >> "%LOG%"
exit /b 0

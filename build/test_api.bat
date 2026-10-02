@echo off
setlocal
set "LOG=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\build\api_test.log"
set "JAVA=C:\Program Files\Java\jdk1.8.0_202\bin\java.exe"

echo === API TEST === > "%LOG%"

:: Test GET trucks (should work)
echo --- GET /api/trucks --- >> "%LOG%"
powershell -Command "(Invoke-WebRequest -Uri 'http://localhost:7070/api/trucks' -UseBasicParsing).Content" >> "%LOG%" 2>&1

:: Test POST trucks - valid payload
echo --- POST /api/trucks (valid) --- >> "%LOG%"
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:7070/api/trucks' -Method POST -Body '{\"plate\":\"KAA 001A\",\"model\":\"HOWO T7H 440\",\"driver\":\"John Doe\",\"nextService\":\"2027-01-01\",\"insuranceExpiry\":\"2027-06-01\"}' -ContentType 'application/json' -UseBasicParsing; Write-Host \"STATUS: $($r.StatusCode) BODY: $($r.Content)\" } catch { Write-Host \"ERROR: $($_.Exception.Message) - $($_.Exception.Response.StatusCode)\" }" >> "%LOG%" 2>&1

:: Test GET trucks again (should show new truck)
echo --- GET /api/trucks (after add) --- >> "%LOG%"
powershell -Command "(Invoke-WebRequest -Uri 'http://localhost:7070/api/trucks' -UseBasicParsing).Content" >> "%LOG%" 2>&1

echo === DONE === >> "%LOG%"
exit /b 0

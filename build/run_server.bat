@echo off
setlocal
set "JAVA=C:\Program Files\Java\jdk1.8.0_202\bin\java.exe"
set "JAR=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\dist\NgumbaWeb.jar"
set "LOG=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\build\server_startup2.log"
echo Starting server... > "%LOG%"
"%JAVA%" -jar "%JAR%" >> "%LOG%" 2>&1
echo Server exited with code %ERRORLEVEL% >> "%LOG%"

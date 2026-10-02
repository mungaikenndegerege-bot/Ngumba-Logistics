@echo off
set "JAVA=C:\Program Files\Java\jdk1.8.0_202\bin\java.exe"
set "JAR=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\dist\NgumbaWeb.jar"
set "LOG=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\build\server.log"
echo Starting Ngumba Logistics web server... > "%LOG%"
"%JAVA%" -jar "%JAR%" >> "%LOG%" 2>&1

@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202"
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAR_TOOL=%JAVA_HOME%\bin\jar.exe"
set "BASE=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics"
set "SRC=%BASE%\src"
set "OUT=%BASE%\build\classes"
set "DIST=%BASE%\dist"
set "JARFILE=%DIST%\Ngumba_Logistics.jar"
set "LOG=%BASE%\build\build_output.txt"

echo Build started > "%LOG%"

if not exist "%OUT%" mkdir "%OUT%"
if not exist "%DIST%" mkdir "%DIST%"

echo === Compiling === >> "%LOG%"
"%JAVAC%" -d "%OUT%" "%SRC%\ngumba\logistics\NgumbaLogistics.java" "%SRC%\ngumba\logistics\NgumbaLogisticsCLI.java" >> "%LOG%" 2>&1

if errorlevel 1 (
    echo COMPILE FAILED >> "%LOG%"
    exit /b 1
)
echo Compile OK >> "%LOG%"

echo === Creating JAR === >> "%LOG%"
echo Main-Class: ngumba.logistics.NgumbaLogistics> "%OUT%\MANIFEST.MF"
"%JAR_TOOL%" cfm "%JARFILE%" "%OUT%\MANIFEST.MF" -C "%OUT%" . >> "%LOG%" 2>&1

if errorlevel 1 (
    echo JAR FAILED >> "%LOG%"
    exit /b 1
)
echo JAR OK >> "%LOG%"
echo BUILD SUCCESS >> "%LOG%"
exit /b 0

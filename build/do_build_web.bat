@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202"
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAR_TOOL=%JAVA_HOME%\bin\jar.exe"

set "BASE=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics"
set "SRC=%BASE%\src"
set "LIB=%BASE%\lib"
set "OUT=%BASE%\build\web-classes"
set "DIST=%BASE%\dist"
set "JARFILE=%DIST%\NgumbaWeb.jar"
set "WEB=%BASE%\web"
set "LOG=%BASE%\build\build_web_output.txt"

echo Web build started > "%LOG%"

if not exist "%OUT%" mkdir "%OUT%"
if not exist "%DIST%" mkdir "%DIST%"

:: Build classpath from all JARs in lib/
:: All JARs in lib/ on the classpath
set "CP=%LIB%\javalin-4.6.8.jar;%LIB%\jackson-core-2.13.4.jar;%LIB%\jackson-databind-2.13.4.jar;%LIB%\jackson-annotations-2.13.4.jar;%LIB%\slf4j-api-1.7.36.jar;%LIB%\slf4j-simple-1.7.36.jar;%LIB%\kotlin-stdlib-1.7.10.jar;%LIB%\jetty-server.jar;%LIB%\jetty-servlet.jar;%LIB%\jetty-webapp.jar;%LIB%\jetty-util.jar;%LIB%\jetty-http.jar;%LIB%\jetty-io.jar;%LIB%\jetty-security.jar;%LIB%\jetty-xml.jar;%LIB%\javax.servlet-api.jar;%LIB%\websocket-server.jar;%LIB%\websocket-servlet.jar;%LIB%\websocket-api.jar;%LIB%\websocket-common.jar"

echo === Compiling Java sources === >> "%LOG%"
"%JAVAC%" -cp "%CP%" -d "%OUT%" ^
  "%SRC%\ngumba\logistics\DataStore.java" ^
  "%SRC%\ngumba\logistics\ApiServer.java" >> "%LOG%" 2>&1

if errorlevel 1 (
    echo COMPILE FAILED >> "%LOG%"
    exit /b 1
)
echo Compile OK >> "%LOG%"

echo === Copying web assets === >> "%LOG%"
:: Copy web/ folder into OUT/web/ so Javalin can serve from classpath
if not exist "%OUT%\web\css" mkdir "%OUT%\web\css"
if not exist "%OUT%\web\js"  mkdir "%OUT%\web\js"
copy /Y "%WEB%\index.html"      "%OUT%\web\index.html" >> "%LOG%" 2>&1
copy /Y "%WEB%\css\style.css"   "%OUT%\web\css\style.css" >> "%LOG%" 2>&1
copy /Y "%WEB%\js\app.js"       "%OUT%\web\js\app.js" >> "%LOG%" 2>&1
echo Web assets copied >> "%LOG%"

echo === Creating fat JAR === >> "%LOG%"
:: Extract all dependency JARs into OUT/
cd /d "%OUT%"
for %%f in ("%LIB%\*.jar") do (
    echo Extracting %%f >> "%LOG%"
    "%JAVA_HOME%\bin\jar.exe" xf "%%f"
)
:: Remove module-info.class files that cause conflicts on Java 8
del /S /Q module-info.class 2>>"%LOG%"

:: Write manifest
echo Main-Class: ngumba.logistics.ApiServer> "%OUT%\MANIFEST.MF"

:: Build the fat JAR from everything in OUT/
"%JAR_TOOL%" cfm "%JARFILE%" "%OUT%\MANIFEST.MF" -C "%OUT%" . >> "%LOG%" 2>&1

if errorlevel 1 (
    echo JAR FAILED >> "%LOG%"
    exit /b 1
)
echo JAR OK: %JARFILE% >> "%LOG%"
echo === WEB BUILD SUCCESS === >> "%LOG%"
exit /b 0

@echo off
setlocal
set "LIB=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\lib"
set "LOG=%LIB%\download.log"
echo Downloading dependencies... > "%LOG%"

:: Javalin 4.6.8 (last Java 8 compatible release, self-contained)
set "JAVALIN_URL=https://repo1.maven.org/maven2/io/javalin/javalin/4.6.8/javalin-4.6.8.jar"
set "JAVALIN_JAR=%LIB%\javalin-4.6.8.jar"

:: Jackson (JSON) - required by Javalin
set "JACKSON_CORE_URL=https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.13.4/jackson-core-2.13.4.jar"
set "JACKSON_DATABIND_URL=https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.13.4/jackson-databind-2.13.4.jar"
set "JACKSON_ANNOTATIONS_URL=https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.13.4/jackson-annotations-2.13.4.jar"

:: SLF4J (logging facade required by Javalin)
set "SLF4J_API_URL=https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
set "SLF4J_SIMPLE_URL=https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar"

echo Downloading Javalin... >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%JAVALIN_URL%' -OutFile '%LIB%\javalin-4.6.8.jar'" >> "%LOG%" 2>&1

echo Downloading Jackson core... >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%JACKSON_CORE_URL%' -OutFile '%LIB%\jackson-core-2.13.4.jar'" >> "%LOG%" 2>&1

echo Downloading Jackson databind... >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%JACKSON_DATABIND_URL%' -OutFile '%LIB%\jackson-databind-2.13.4.jar'" >> "%LOG%" 2>&1

echo Downloading Jackson annotations... >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%JACKSON_ANNOTATIONS_URL%' -OutFile '%LIB%\jackson-annotations-2.13.4.jar'" >> "%LOG%" 2>&1

echo Downloading SLF4J API... >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%SLF4J_API_URL%' -OutFile '%LIB%\slf4j-api-1.7.36.jar'" >> "%LOG%" 2>&1

echo Downloading SLF4J Simple... >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%SLF4J_SIMPLE_URL%' -OutFile '%LIB%\slf4j-simple-1.7.36.jar'" >> "%LOG%" 2>&1

echo. >> "%LOG%"
echo Checking files: >> "%LOG%"
dir "%LIB%\*.jar" >> "%LOG%" 2>&1
echo Done. >> "%LOG%"
exit /b 0

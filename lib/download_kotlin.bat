@echo off
setlocal
set "LIB=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\lib"
set "LOG=%LIB%\download_kotlin.log"
echo Downloading Kotlin stdlib... > "%LOG%"

:: Kotlin stdlib (Javalin 4.x is written in Kotlin and needs this at runtime)
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/1.7.10/kotlin-stdlib-1.7.10.jar' -OutFile '%LIB%\kotlin-stdlib-1.7.10.jar'" >> "%LOG%" 2>&1

echo Done >> "%LOG%"
dir "%LIB%\kotlin*.jar" >> "%LOG%" 2>&1
exit /b 0

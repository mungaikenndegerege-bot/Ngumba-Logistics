@echo off
setlocal
set "LIB=c:\Users\user\Documents\NetBeansProjects\Ngumba Logistics\lib"
set "LOG=%LIB%\download_all.log"
set "BASE=https://repo1.maven.org/maven2"
echo Downloading all Javalin 4.6.8 runtime deps... > "%LOG%"

:: --- Jetty (embedded server, needed by Javalin) ---
powershell -Command "Invoke-WebRequest -Uri '%BASE%/org/eclipse/jetty/jetty-server/9.4.48.v20220622/jetty-server-9.4.48.v20220622.jar' -OutFile '%LIB%\jetty-server.jar'" >> "%LOG%" 2>&1
echo jetty-server done >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%BASE%/org/eclipse/jetty/jetty-servlet/9.4.48.v20220622/jetty-servlet-9.4.48.v20220622.jar' -OutFile '%LIB%\jetty-servlet.jar'" >> "%LOG%" 2>&1
echo jetty-servlet done >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%BASE%/org/eclipse/jetty/jetty-webapp/9.4.48.v20220622/jetty-webapp-9.4.48.v20220622.jar' -OutFile '%LIB%\jetty-webapp.jar'" >> "%LOG%" 2>&1
echo jetty-webapp done >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%BASE%/org/eclipse/jetty/jetty-util/9.4.48.v20220622/jetty-util-9.4.48.v20220622.jar' -OutFile '%LIB%\jetty-util.jar'" >> "%LOG%" 2>&1
echo jetty-util done >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%BASE%/org/eclipse/jetty/jetty-http/9.4.48.v20220622/jetty-http-9.4.48.v20220622.jar' -OutFile '%LIB%\jetty-http.jar'" >> "%LOG%" 2>&1
echo jetty-http done >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%BASE%/org/eclipse/jetty/jetty-io/9.4.48.v20220622/jetty-io-9.4.48.v20220622.jar' -OutFile '%LIB%\jetty-io.jar'" >> "%LOG%" 2>&1
echo jetty-io done >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%BASE%/org/eclipse/jetty/jetty-security/9.4.48.v20220622/jetty-security-9.4.48.v20220622.jar' -OutFile '%LIB%\jetty-security.jar'" >> "%LOG%" 2>&1
echo jetty-security done >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%BASE%/org/eclipse/jetty/jetty-xml/9.4.48.v20220622/jetty-xml-9.4.48.v20220622.jar' -OutFile '%LIB%\jetty-xml.jar'" >> "%LOG%" 2>&1
echo jetty-xml done >> "%LOG%"

:: --- Servlet API ---
powershell -Command "Invoke-WebRequest -Uri '%BASE%/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar' -OutFile '%LIB%\javax.servlet-api.jar'" >> "%LOG%" 2>&1
echo servlet-api done >> "%LOG%"

:: --- Websocket (needed by Javalin) ---
powershell -Command "Invoke-WebRequest -Uri '%BASE%/org/eclipse/jetty/websocket/websocket-server/9.4.48.v20220622/websocket-server-9.4.48.v20220622.jar' -OutFile '%LIB%\websocket-server.jar'" >> "%LOG%" 2>&1
echo websocket-server done >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%BASE%/org/eclipse/jetty/websocket/websocket-servlet/9.4.48.v20220622/websocket-servlet-9.4.48.v20220622.jar' -OutFile '%LIB%\websocket-servlet.jar'" >> "%LOG%" 2>&1
echo websocket-servlet done >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%BASE%/org/eclipse/jetty/websocket/websocket-api/9.4.48.v20220622/websocket-api-9.4.48.v20220622.jar' -OutFile '%LIB%\websocket-api.jar'" >> "%LOG%" 2>&1
echo websocket-api done >> "%LOG%"
powershell -Command "Invoke-WebRequest -Uri '%BASE%/org/eclipse/jetty/websocket/websocket-common/9.4.48.v20220622/websocket-common-9.4.48.v20220622.jar' -OutFile '%LIB%\websocket-common.jar'" >> "%LOG%" 2>&1
echo websocket-common done >> "%LOG%"

echo. >> "%LOG%"
echo === All downloads complete === >> "%LOG%"
dir "%LIB%\*.jar" >> "%LOG%" 2>&1
exit /b 0

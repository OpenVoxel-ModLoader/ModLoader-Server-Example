@echo off
rem Builds HungerGames.jar into dist\ (needs the dedicated server JAR: OPENVOXEL_SERVER_JAR).
setlocal
cd /d "%~dp0"
set "SERVER=%OPENVOXEL_SERVER_JAR%"
if "%SERVER%"=="" if exist "..\ModLoader-Server\server\openvoxel-server.jar" set "SERVER=..\ModLoader-Server\server\openvoxel-server.jar"
if "%SERVER%"=="" if exist "..\..\..\ServerTest\openvoxel-server-0.0.8-SNAPSHOT.jar" set "SERVER=..\..\..\ServerTest\openvoxel-server-0.0.8-SNAPSHOT.jar"
if "%SERVER%"=="" ( echo Server JAR not found; set OPENVOXEL_SERVER_JAR. & exit /b 1 )
if exist build rmdir /s /q build
mkdir build
if not exist dist mkdir dist
dir /s /b src\*.java > build\sources.txt
javac --release 11 -encoding UTF-8 -Xlint:-options -classpath "%SERVER%" -d build @build\sources.txt
if errorlevel 1 exit /b 1
del build\sources.txt
copy /y resources\plugin.yml build\plugin.yml >nul
jar --create --file "dist\HungerGames.jar" -C build .
if errorlevel 1 exit /b 1
if exist "..\ModLoader-Server\mods" copy /y "dist\HungerGames.jar" "..\ModLoader-Server\mods\" >nul
echo HungerGames.jar -^> dist ^(and ..\ModLoader-Server\mods if present^)

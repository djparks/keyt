@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM
@REM Required ENV vars:
@REM   JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars
@REM   MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the batch commands
@REM   MAVEN_BATCH_PAUSE - set to 'on' to wait for a keystroke before ending
@REM   MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@echo off
if "%MAVEN_BATCH_ECHO%" == "on"  echo %MAVEN_BATCH_ECHO%

setlocal

if not "%MAVEN_SKIP_RC%" == "" goto skipRcPre
@REM check for pre script, once with legacy .bat ending and once with .cmd ending
if exist "%USERPROFILE%\mavenrc_pre.bat" call "%USERPROFILE%\mavenrc_pre.bat"
if exist "%USERPROFILE%\mavenrc_pre.cmd" call "%USERPROFILE%\mavenrc_pre.cmd"
:skipRcPre

set ERROR_CODE=0

@REM Find Java
if not "%JAVA_HOME%" == "" goto OkJHome

set "JAVA_EXE=java.exe"
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% == 0 goto Init

echo.
echo Error: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2

echo Please set the JAVA_HOME variable in your environment to match the 1>&2

echo location of your Java installation. 1>&2

goto error

:OkJHome
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

if exist "%JAVA_EXE%" goto Init

echo.
echo Error: JAVA_HOME is set to an invalid directory. 1>&2

echo JAVA_HOME = "%JAVA_HOME%" 1>&2

echo Please set the JAVA_HOME variable in your environment to match the 1>&2

echo location of your Java installation. 1>&2

goto error

:Init
@REM Find the project base dir, i.e. the directory that contains the ".mvn" subdirectory.
set "CURRENT_DIR=%cd%"
set "BASEDIR=%CURRENT_DIR%"
if not "%MAVEN_PROJECTBASEDIR%" == "" goto endDetectBaseDir

set "EXEC_DIR=%BASEDIR%"
:findBaseDir
if exist "%EXEC_DIR%\mvnw.cmd" set "BASEDIR=%EXEC_DIR%" & goto endDetectBaseDir
set "EXEC_DIR=%EXEC_DIR%\.."
if "%EXEC_DIR%" == "%EXEC_DIR%\.." goto endDetectBaseDir
goto findBaseDir

:endDetectBaseDir
if not exist "%BASEDIR%\.mvn" goto error

set "MAVEN_PROJECTBASEDIR=%BASEDIR%"

@REM Read properties
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
set WRAPPER_PROPERTIES="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"

@REM If the wrapper jar doesn't exist, download it
if exist %WRAPPER_JAR% goto afterWrapperDownload

set wrapperUrl=
for /F "usebackq tokens=1,* delims==" %%A in (%WRAPPER_PROPERTIES%) do (
  if /I "%%A"=="wrapperUrl" set wrapperUrl=%%B
)
if "%wrapperUrl%"=="" set wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

if exist "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper" goto downloadWithPowershell
mkdir "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper" >NUL 2>&1

:downloadWithPowershell
powershell -NoProfile -ExecutionPolicy Bypass -Command "^$wc = New-Object System.Net.WebClient; ^$wc.DownloadFile('%wrapperUrl%', '%WRAPPER_JAR:~1,-1%')" >NUL 2>&1
if exist %WRAPPER_JAR% goto afterWrapperDownload

@REM Fallback to curl or wget if PowerShell failed
where curl >NUL 2>&1 && curl -fsSL -o %WRAPPER_JAR% %wrapperUrl%
if exist %WRAPPER_JAR% goto afterWrapperDownload
where wget >NUL 2>&1 && wget -q -O %WRAPPER_JAR% %wrapperUrl%
if exist %WRAPPER_JAR% goto afterWrapperDownload

echo.
echo Error: Could not download Maven Wrapper JAR from %wrapperUrl% 1>&2

goto error

:afterWrapperDownload

set MAVEN_JAVA_EXE="%JAVA_EXE%"
set WRAPPER_LAUNCH_CMD="%MAVEN_JAVA_EXE%" %MAVEN_OPTS% -classpath %WRAPPER_JAR% "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*

if "%MAVEN_BATCH_PAUSE%" == "on" pause
%WRAPPER_LAUNCH_CMD%
if %ERRORLEVEL% NEQ 0 goto error

:end
if "%MAVEN_SKIP_RC%" == "" goto skipRcPost
@REM check for post script, once with legacy .bat ending and once with .cmd ending
if exist "%USERPROFILE%\mavenrc_post.bat" call "%USERPROFILE%\mavenrc_post.bat"
if exist "%USERPROFILE%\mavenrc_post.cmd" call "%USERPROFILE%\mavenrc_post.cmd"
:skipRcPost

endlocal & set ERROR_CODE=%ERRORLEVEL%

if not "%MAVEN_BATCH_PAUSE%" == "" pause

if exist "%CURRENT_DIR%" cd "%CURRENT_DIR%"

exit /B %ERROR_CODE%

:error
endlocal & set ERROR_CODE=1
if not "%MAVEN_BATCH_PAUSE%" == "" pause
exit /B %ERROR_CODE%

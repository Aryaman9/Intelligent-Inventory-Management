@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM   http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "MVNW_ARG0_NAME=mvnw.cmd")
@SET MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
@IF NOT "%MAVEN_PROJECTBASEDIR%"=="" GOTO endDetectBaseDir

@SET EXEC_DIR=%CD%
@SET WDIR=%EXEC_DIR%
:findBaseDir
@IF EXIST "%WDIR%"\.mvn GOTO baseDirFound
@cd ..
@SET "WDIR=%CD%"
@IF NOT "%WDIR%"=="%EXEC_DIR%" GOTO findBaseDir
@SET MAVEN_PROJECTBASEDIR=%EXEC_DIR%
@GOTO endDetectBaseDir
:baseDirFound
@SET MAVEN_PROJECTBASEDIR=%WDIR%
@cd "%EXEC_DIR%"
:endDetectBaseDir

@SET MVNW_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar

@IF EXIST "%MVNW_JAR%" (
  GOTO runMvnw
)

@SET MVNW_URL=""
@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") DO (
  @IF "%%A"=="wrapperUrl" SET MVNW_URL=%%B
)

@IF "%MVNW_URL%"=="" (
  SET "MVNW_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
)

@powershell -Command "&{"^
  "$webclient = new-object System.Net.WebClient;"^
  "if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%') -and [string]::IsNullOrEmpty('%MVNW_PASSWORD%'))) {"^
  "$webclient.Credentials = new-object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');"^
  "}"^
  "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;"^
  "$webclient.DownloadFile('%MVNW_URL%', '%MVNW_JAR%')"^
  "}"
if "%ERRORLEVEL%"=="0" GOTO runMvnw
echo Failed to download Maven wrapper jar. >&2
exit /B 1

:runMvnw
@SET MAVEN_JAVA_EXE="%JAVA_HOME%/bin/java.exe"
@IF EXIST %MAVEN_JAVA_EXE% GOTO foundJava
@SET MAVEN_JAVA_EXE=java.exe
:foundJava

@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
@SET MAVEN_OPTS="%MAVEN_OPTS%"

@SET MAVEN_CMD_LINE_ARGS=%MAVEN_CONFIG% %*

%MAVEN_JAVA_EXE% %MAVEN_OPTS% ^
  -classpath "%MVNW_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*

if "%ERRORLEVEL%"=="0" GOTO end
exit /B %ERRORLEVEL%

:end

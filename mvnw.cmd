@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF)
@REM Maven Wrapper startup batch script, version 3.3.1
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0") ELSE (SET "BASE_DIR=%~dp0")
@SET "MAVEN_PROJECTBASEDIR=%BASE_DIR%"
@SET MAVEN_OPTS=%MAVEN_OPTS% -Xss10M

@SET "MVNW_VERBOSE=false"
@IF NOT "%MVNW_VERBOSE%"=="false" @ECHO "[mvnw] MVNW_VERBOSE=true"

@SET "_JAVACMD_=%JAVACMD%"
@IF "%_JAVACMD_%"=="" (
  @SET "_JAVACMD_=java"
  @IF NOT "%JAVA_HOME%"=="" @SET "_JAVACMD_=%JAVA_HOME%\bin\java"
)

@SET WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@SET DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.1/maven-wrapper-3.3.1.jar"

@IF EXIST %WRAPPER_JAR% (
  @IF "%MVNW_VERBOSE%"=="true" @ECHO "Found %WRAPPER_JAR%"
) ELSE (
  @ECHO "Downloading %DOWNLOAD_URL% to %WRAPPER_JAR%"
  @powershell -Command "&{"^
    "$webclient = new-object System.Net.WebClient;"^
    "if ($env:MVNW_USERNAME -ne '' -and $env:MVNW_PASSWORD -ne '') {"^
      "$webclient.Credentials = new-object System.Net.NetworkCredential($env:MVNW_USERNAME, $env:MVNW_PASSWORD);"^
    "}"^
    "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile(%DOWNLOAD_URL%, %WRAPPER_JAR%)"^
  "}"
)

@"%_JAVACMD_%" %JVM_CONFIG_MAVEN_PROPS% %MAVEN_OPTS% %MAVEN_DEBUG_OPTS% ^
  -classpath %WRAPPER_JAR% ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*
@IF ERRORLEVEL 1 GOTO error
@GOTO end

:error
@SET ERROR_CODE=%ERRORLEVEL%

:end
@ENDLOCAL & SET ERROR_CODE=%ERROR_CODE%
@CMD /C EXIT /B %ERROR_CODE%

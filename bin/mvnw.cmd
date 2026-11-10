@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------
@IF "%DEBUG%" == "" @ECHO OFF
@setlocal
set ERROR_CODE=0
set MAVEN_CMD_LINE_ARGS=%*
if not "%MAVEN_SKIP_RC%" == "" goto skipRcPre
if exist "%USERPROFILE%\.mavenrc_pre.bat" call "%USERPROFILE%\.mavenrc_pre.bat"
:skipRcPre
set "DIRNAME=%~dp0"
if "%DIRNAME%" == "" set "DIRNAME=."
set "MAVEN_PROJECTBASEDIR=%DIRNAME%"
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
if not exist "%WRAPPER_JAR%" (
  mvn %MAVEN_CMD_LINE_ARGS%
  goto end
)
java -jar "%WRAPPER_JAR%" %MAVEN_CMD_LINE_ARGS%
:end
@endlocal

@echo off

setlocal

set groupId=com.alx.appserver
set version=1.0.0
@REM you can specify repositoryId and url here,
@REM or you can specify value in command. Syntax: deploylib.sh [repositoryId] [url]
::set repositoryId=
::set url=

set "libDir=%cd%/lib"
if not exist "%libDir%" (
  echo Directory %libDir% does not exist, please put jars into lib directory at first!
  goto end
)

if not "%1" == "" set "repositoryId=%1"
if not "%2" == "" set "url=%2"

if "%repositoryId%" == "" goto error_param
if "%url%" == "" goto error_param
goto ok_param

:error_param
echo Please specify repositoryId and url in deploylib.bat
echo Or you can specify by command parameter, Usage: deploylib.bat [repositoryId] [url]
goto end

:ok_param
call mvn deploy:deploy-file -Dfile="%libDir%/alxas-core-%version%.jar" -DgroupId=%groupId% -DartifactId=alxas-core -Dversion=%version% -Dpackaging=jar -DrepositoryId=%repositoryId% -Durl=%url% >> deploy.log
call :validateResult %groupId%:alxas-core:%version%

call mvn deploy:deploy-file -Dfile="%libDir%/alxas-websocket-%version%.jar" -DgroupId=%groupId% -DartifactId=alxas-websocket -Dversion=%version% -Dpackaging=jar  -DrepositoryId=%repositoryId% -Durl=%url% >> deploy.log
call :validateResult %groupId%:alxas-websocket:%version%

call mvn deploy:deploy-file -Dfile="%libDir%/alxas-jasper-%version%.jar" -DgroupId=%groupId% -DartifactId=alxas-jasper -Dversion=%version% -Dpackaging=jar  -DrepositoryId=%repositoryId% -Durl=%url% >> deploy.log
call :validateResult  %groupId%:alxas-jasper:%version%

call mvn deploy:deploy-file -Dfile="%libDir%/alxas-el-%version%.jar" -DgroupId=%groupId% -DartifactId=alxas-el -Dversion=%version% -Dpackaging=jar  -DrepositoryId=%repositoryId% -Durl=%url% >> deploy.log
call :validateResult %groupId%:alxas-el:%version%
goto :end

:validateResult
    if %errorlevel% == 0 (
      echo info: deploy %1 success.
    ) else (
       echo error: deploy %1 failed!
    )
goto:eof

endlocal
:end
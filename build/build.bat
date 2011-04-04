@echo off

set TARGET=%1%

set JAVA_HOME=c:\java

set HELMA_HOME_14=c:\helma.14

REM --------------------------------------------
REM No need to edit anything past here
REM --------------------------------------------

set BUILDFILE=build.xml

if "%JAVA_HOME%" == "" goto javahomeerror

set CP=%CLASSPATH%;ant.jar;crimson.jar
if exist %JAVA_HOME%\lib\tools.jar set CP=%CP%;%JAVA_HOME%\lib\tools.jar

echo Classpath: %CP%
echo JAVA_HOME: %JAVA_HOME%

%JAVA_HOME%\bin\java.exe -classpath "%CP%" org.apache.tools.ant.Main -buildfile %BUILDFILE% %TARGET%
goto end


:javahomeerror
echo ERROR: JAVA_HOME not found in your environment.
echo Please, set the JAVA_HOME variable in your environment to match the
echo location of the Java Virtual Machine you want to use.

:end
echo Done.

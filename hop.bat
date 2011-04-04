@echo off

rem Batch file for starting Hop with a JDK-like virtual machine.

set JAVA_HOME=c:\java

set HOP_HOME=c:\helma
set WEB_PORT=8080
set XMLRPC_PORT=8090

set JARS=classes;jgroups-all.jar;
set JARS=%JARS%;%HOP_HOME%\lib\activation.jar
set JARS=%JARS%;%HOP_HOME%\lib\apache-dom.jar
set JARS=%JARS%;%HOP_HOME%\lib\commons-logging.jar
set JARS=%JARS%;%HOP_HOME%\lib\crimson.jar
set JARS=%JARS%;%HOP_HOME%\lib\helma.jar
set JARS=%JARS%;%HOP_HOME%\lib\jetty.jar
set JARS=%JARS%;%HOP_HOME%\lib\jimi.jar
set JARS=%JARS%;%HOP_HOME%\lib\mail.jar
set JARS=%JARS%;%HOP_HOME%\lib\mysql.jar
set JARS=%JARS%;%HOP_HOME%\lib\netcomponents.jar
set JARS=%JARS%;%HOP_HOME%\lib\rhino.jar
set JARS=%JARS%;%HOP_HOME%\lib\servlet.jar
set JARS=%JARS%;%HOP_HOME%\lib\xmlrpc.jar

%JAVA_HOME%\bin\java -classpath c:\winnt\java\packages\rmi.zip;%JARS% helma.main.Server -w %WEB_PORT% -x %XMLRPC_PORT%

rem echo %JARS%
rem run groupextension-test
rem %JAVA_HOME%\bin\java -classpath %JARS% helma.extensions.helmagroups.GroupExtension -group f:/dev/helma.12/extensions/helmagroups/helmagroups/default.xml





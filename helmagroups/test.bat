@echo off

rem Shell script for running helmagroups utilities

rem // uncomment to set java home
rem set JAVA_HOME=c:\java

set JARS=../lib/ext/helmagroups-0.7.jar;../lib/ext/jgroups-all.jar;../lib/helma.jar

set MCAST_ADDR=224.0.0.150
set MCAST_PORT=45000
set MCAST_TTL=32

rem set ip address to bind multicasting to
set BIND_ADDR=127.0.0.1

if "%1%" == "-version" (
   %JAVA_HOME%/bin/java -cp %JARS% helma.extensions.helmagroups.GroupExtension -version
   goto end
)

if "%1%" == "-group" (
   IF "%2%" == "" (
      set FILE=default.xml
      echo using default.xml as config
   ) ELSE (
      set FILE=%2%
   )
   %JAVA_HOME%/bin/java -cp %JARS% helma.extensions.helmagroups.GroupExtension -group %FILE%
   goto end
)

if "%1%" == "-sender" (
   echo type quit or exit to leave
   %JAVA_HOME%/bin/java -cp %JARS% org.jgroups.tests.McastSenderTest1_4 -bind_addr %BIND_ADDR% -mcast_addr %MCAST_ADDR% -port %MCAST_PORT% -ttl %MCAST_TTL%
   goto end
)

if "%1%" == "-receiver" (
   echo type CTRL-C to leave
   %JAVA_HOME%/bin/java -cp %JARS% org.jgroups.tests.McastReceiverTest1_4 -bind_addr %BIND_ADDR% -mcast_addr %MCAST_ADDR% -port %MCAST_PORT%
   goto end
)

echo usage: test.bat [-version] [-group configfile.xml] [-sender] [-receiver]

:end


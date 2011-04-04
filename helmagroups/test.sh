#!/bin/sh
# Shell script for running helmagroups utilities

# uncomment to set JAVA_HOME variable
# JAVA_HOME=/usr/lib/java

JARS=../lib/ext/helmagroups-0.7.jar:../lib/ext/jgroups-all.jar:../lib/helma.jar

MCAST_ADDR=224.0.0.150
MCAST_PORT=45000
MCAST_TTL=32

# set ip address to bind multicasting to
BIND_ADDR=127.0.0.1

if [ "$1" == "-version" ] ; then
   $JAVA_HOME/bin/java -cp $JARS helma.extensions.helmagroups.GroupExtension -version
   exit 0
fi

if [ "$1" == "-group" ] ; then
   $JAVA_HOME/bin/java -cp $JARS helma.extensions.helmagroups.GroupExtension -group $2
   exit 0
fi

if [ "$1" == "-sender" ] ; then
   echo "type quit or exit to leave"
   $JAVA_HOME/bin/java -cp $JARS org.jgroups.tests.McastSenderTest1_4 -bind_addr $BIND_ADDR -mcast_addr $MCAST_ADDR -port $MCAST_PORT
   exit 0
fi

if [ "$1" == "-receiver" ] ; then
   echo "type ctrl-c to leave"
   $JAVA_HOME/bin/java -cp $JARS org.jgroups.tests.McastReceiverTest1_4 -bind_addr $BIND_ADDR -mcast_addr $MCAST_ADDR -port $MCAST_PORT
   exit 0
fi

echo "helmagroups test utilities"
echo "usage: test.sh [-version] [-group configfile.xml] [-sender] [-receiver]"



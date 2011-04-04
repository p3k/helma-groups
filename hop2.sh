#!/bin/sh

# Shell script for starting a demo of HelmaGroups with a JDK-like virtual machine.

# Change these parameters according to your system:
export JAVA_HOME=/usr/lib/java
export HOP_HOME=..
export WEB_PORT=8081
export XMLRPC_PORT=8091

export JARS=helmagroups.jar:jgroups-all.jar
export JARS=$JARS:$HOP_HOME/lib/activation.jar
export JARS=$JARS:$HOP_HOME/lib/apache-dom.jar
export JARS=$JARS:$HOP_HOME/lib/crimson.jar
export JARS=$JARS:$HOP_HOME/lib/helma.jar
export JARS=$JARS:$HOP_HOME/lib/jdom.jar
export JARS=$JARS:$HOP_HOME/lib/jetty.jar
export JARS=$JARS:$HOP_HOME/lib/jimi.jar
export JARS=$JARS:$HOP_HOME/lib/mail.jar
export JARS=$JARS:$HOP_HOME/lib/minml.ja
export JARS=$JARS:$HOP_HOME/lib/mysql.jar
export JARS=$JARS:$HOP_HOME/lib/netcomponents.jar
export JARS=$JARS:$HOP_HOME/lib/regexp.jar
export JARS=$JARS:$HOP_HOME/lib/servlet.jar
export JARS=$JARS:$HOP_HOME/lib/village.jar

$JAVA_HOME/bin/java -Xms32M -Xmx180M -classpath $JARS helma.main.Server -w $WEB_PORT -x $XMLRPC_PORT


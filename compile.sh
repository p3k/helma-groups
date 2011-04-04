!#/bin/sh

cd build
build.sh compile
cd ..

export JAVA_HOME=/usr/lib/java
export HOP_HOME=/usr/local/helma
export WEB_PORT=8080
export XMLRPC_PORT=8090

export JARS=classes:javagroups-all.jar
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

$JAVA_HOME/bin/java -Xms32M -Xmx180M -Xdebug -Xrunjdwp:transport=dt_socket,address=8079,server=y,suspend=n -classpath $JARS helma.main.Server -w $WEB_PORT -x $XMLRPC_PORT



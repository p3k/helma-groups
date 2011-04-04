@ECHO OFF
set HOP=D:\work\hop\
set JARS=%HOP%lib\helma.jar
set JARS=%JARS%;%HOP%lib\activation.jar
set JARS=%JARS%;%HOP%lib\apache-dom.jar
set JARS=%JARS%;%HOP%lib\crimson.jar
set JARS=%JARS%;%HOP%lib\gnu-regexp.jar
set JARS=%JARS%;%HOP%lib\jdom.jar
set JARS=%JARS%;%HOP%lib\jetty.jar
set JARS=%JARS%;%HOP%lib\jimi.jar
set JARS=%JARS%;%HOP%lib\mail.jar
set JARS=%JARS%;%HOP%lib\minml.jar
set JARS=%JARS%;%HOP%lib\netcomponents.jar
set JARS=%JARS%;%HOP%lib\regexp.jar
set JARS=%JARS%;%HOP%lib\servlet.jar
set JARS=%JARS%;%HOP%lib\village.jar

set JARS=%JARS%;lib\helma-group.jar

C:\java\j2sdk1.4.0\bin\serialver -classpath %JARS% [Lhelma.extensions.helmagroups.GroupObject;
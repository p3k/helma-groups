#! /bin/sh

#   Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
#   reserved.
#   Modified for HelmaGroups 2002

export JAVA_HOME=/usr/lib/java
export HELMA_HOME_14=/usr/local/helma-1.4


#--------------------------------------------
# No need to edit anything past here
#--------------------------------------------

# provide default values for people who don't use RPMs
if [ -z "$rpm_mode" ] ; then
  rpm_mode=false;
fi
if [ -z "$usejikes" ] ; then
  usejikes=false;
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true
           if [ -z "$JAVA_HOME" ] ; then
             JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home   
           fi
           ;;
esac

# set ANT_LIB location
ANT_LIB=.

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$ANT_HOME" ] &&
    ANT_HOME=`cygpath --unix "$ANT_HOME"`
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

if [ -z "$JAVACMD" ] ; then 
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then 
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=java
  fi
fi
 
if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute $JAVACMD"
  exit 1
fi

if [ -n "$CLASSPATH" ] ; then
  LOCALCLASSPATH="$CLASSPATH"
fi

# add in the dependency .jar files in non-RPM mode (the default)
for i in "${ANT_LIB}"/*.jar
do
  # if the directory is empty, then it will return the input string
  # this is stupid, so case for it
  if [ "$i" != "${ANT_LIB}/*.jar" ] ; then
    if [ -z "$LOCALCLASSPATH" ] ; then
      LOCALCLASSPATH=$i
    else
      LOCALCLASSPATH="$i":"$LOCALCLASSPATH"
    fi
  fi
done

if [ -n "$JAVA_HOME" ] ; then
  if [ -f "$JAVA_HOME/lib/tools.jar" ] ; then
    LOCALCLASSPATH="$LOCALCLASSPATH:$JAVA_HOME/lib/tools.jar"
  fi

  if [ -f "$JAVA_HOME/lib/classes.zip" ] ; then
    LOCALCLASSPATH="$LOCALCLASSPATH:$JAVA_HOME/lib/classes.zip"
  fi

  # OSX hack to make Ant work with jikes
  if $darwin ; then
    OSXHACK="/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Classes"
    if [ -d ${OSXHACK} ] ; then
      for i in ${OSXHACK}/*.jar
      do
        JIKESPATH=$JIKESPATH:$i
      done
    fi
  fi
else
  echo "Warning: JAVA_HOME environment variable is not set."
  echo "  If build fails because sun.* classes could not be found"
  echo "  you will need to set the JAVA_HOME environment variable"
  echo "  to the installation directory of java."
fi

# supply JIKESPATH to Ant as jikes.class.path
if [ -n "$JIKESPATH" ] ; then
  if $cygwin ; then
    JIKESPATH=`cygpath --path --windows "$JIKESPATH"`
  fi
  ANT_OPTS="$ANT_OPTS -Djikes.class.path=$JIKESPATH"
fi

# Allow Jikes support (off by default)
if $usejikes; then
  ANT_OPTS="$ANT_OPTS -Dbuild.compiler=jikes"
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  ANT_HOME=`cygpath --path --windows "$ANT_HOME"`
  JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  LOCALCLASSPATH=`cygpath --path --windows "$LOCALCLASSPATH"`
  ANT_OPTS="$ANT_OPTS -Dcygwin.user.home="`cygpath --path --windows "$HOME"`
fi
echo $LOCALCLASSPATH
"$JAVACMD" -classpath "$LOCALCLASSPATH" -Dant.home="${ANT_HOME}" $ANT_OPTS org.apache.tools.ant.Main $ANT_ARGS "$@"

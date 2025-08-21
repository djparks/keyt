#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Maven Start Up Batch script
#
# Required ENV vars:
#   JAVA_HOME - location of a JDK home dir
#
# Optional ENV vars
#   MAVEN_OPTS - parameters passed to the Java VM when running Maven
#     e.g. to debug Maven itself, use
#       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
#   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
# ----------------------------------------------------------------------------

if [ -z "$MAVEN_SKIP_RC" ] ; then
  if [ -f /usr/local/etc/mavenrc ] ; then
    . /usr/local/etc/mavenrc
  fi
  if [ -f /etc/mavenrc ] ; then
    . /etc/mavenrc
  fi
  if [ -f "$HOME"/.mavenrc ] ; then
    . "$HOME"/.mavenrc
  fi
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
mingw=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  MINGW*) mingw=true ;;
  Darwin*) darwin=true ;;
  MSYS*) mingw=true ;;
  *) ;;
esac

# Resolve links: $0 may be a symlink
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
  if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
    # IBM's JDK on AIX uses strange locations for the executables
    JAVACMD="$JAVA_HOME/jre/sh/java"
  else
    JAVACMD="$JAVA_HOME/bin/java"
  fi
  if [ ! -x "$JAVACMD" ] ; then
    echo "Error: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
    echo "Please set the JAVA_HOME variable in your environment to match the" >&2
    echo "location of your Java installation." >&2
    exit 1
  fi
else
  JAVACMD="java"
  which java >/dev/null 2>&1 || {
    echo "Error: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    echo "Please set the JAVA_HOME variable in your environment to match the" >&2
    echo "location of your Java installation." >&2
    exit 1
  }
fi

# For Cygwin or MSYS, ensure paths are in Unix format before anything is touched.
if $cygwin || $mingw ; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# Locate the .mvn directory
BASE_DIR=`pwd`
while [ "$BASE_DIR" != '/' ] && [ ! -d "$BASE_DIR/.mvn" ] ; do
  BASE_DIR=`cd "$BASE_DIR/.."; pwd`
done
if [ ! -d "$BASE_DIR/.mvn" ] ; then
  BASE_DIR="$APP_HOME"
fi

MAVEN_PROJECTBASEDIR="$BASE_DIR"
export MAVEN_PROJECTBASEDIR

WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

# If the wrapper jar doesn't exist, download it
if [ ! -f "$WRAPPER_JAR" ]; then
  if [ -r "$WRAPPER_PROPERTIES" ]; then
    . "$WRAPPER_PROPERTIES" >/dev/null 2>&1 || true
  fi
  # Read wrapperUrl from properties
  wrapperUrl=`sed -n 's/^wrapperUrl=//p' "$WRAPPER_PROPERTIES"`
  if [ -z "$wrapperUrl" ]; then
    wrapperUrl="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
  fi
  mkdir -p "`dirname "$WRAPPER_JAR"`"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL -o "$WRAPPER_JAR" "$wrapperUrl"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$WRAPPER_JAR" "$wrapperUrl"
  else
    echo "Error: Could not find 'curl' or 'wget' to download the Maven Wrapper." >&2
    exit 1
  fi
fi

# Provide a sane default for MAVEN_OPTS, if not already set
MAVEN_OPTS="${MAVEN_OPTS:-}"

# For Darwin, ensure that JAVA_HOME is set
if $darwin ; then
  if [ -z "$JAVA_HOME" ] ; then
    if [ -x "/usr/libexec/java_home" ] ; then
      JAVA_HOME=`/usr/libexec/java_home`
    fi
  fi
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin || $mingw ; then
  MAVEN_PROJECTBASEDIR=`cygpath --path --mixed "$MAVEN_PROJECTBASEDIR"`
  CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
  JAVACMD=`cygpath --unix "$JAVACMD"`
fi

# Traverses directory structure from process work directory to filesystem root
# first directory with .mvn subdirectory is considered project base directory.
# For Cygwin or MSYS, the .mvn directory is expected in Unix format only.

exec "$JAVACMD" \
  $MAVEN_OPTS \
  -classpath "$WRAPPER_JAR" \
  "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
  $WRAPPER_LAUNCHER \
  "$@"

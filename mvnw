#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.2
# ----------------------------------------------------------------------------

set -e

# Find the project base dir, i.e. the directory that contains the folder ".mvn".
find_maven_basedir() (
  if [ -z "$1" ]; then
    echo "Path not specified to find_maven_basedir"
    return 1
  fi
  basedir="$1"
  wdir="$1"
  while true; do
    if [ -d "$wdir"/.mvn ]; then
      basedir=$wdir
      break
    fi
    if [ "$wdir" = "/" ]; then
      break
    fi
    wdir=$(cd "$wdir/.." && pwd)
  done
  printf '%s' "$(cd "$basedir" && pwd)"
)

# concatenates all lines of a file
concat_lines() {
  if [ -f "$1" ]; then
    echo "$(tr -s '\n' ' ' <"$1")"
  fi
}

log() {
  if [ "$MVNW_VERBOSE" = true ]; then
    printf '%s\n' "$1"
  fi
}

BASE_DIR=$(find_maven_basedir "$(dirname "$0")")
if [ -z "$BASE_DIR" ]; then
  exit 1
fi

MAVEN_PROJECTBASEDIR=${MAVEN_BASEDIR:-"$BASE_DIR"}
log "$MAVEN_PROJECTBASEDIR"

##########################################################################################
# Extension to allow automatically downloading the maven-wrapper.jar from Maven-central
# This allows using the maven wrapper in projects that prohibit checking in binary data.
##########################################################################################
wrapperJarPath="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
if [ -r "$wrapperJarPath" ]; then
  log "Found $wrapperJarPath"
else
  log "Couldn't find $wrapperJarPath, downloading it ..."

  if [ -n "$MVNW_REPOURL" ]; then
    wrapperUrl="$MVNW_REPOURL/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  else
    wrapperUrl="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
  fi

  while IFS="=" read -r key value; do
    # Remove '\r' from value to fix Windows line endings
    value=$(echo "$value" | tr -d '\r')
    case "$key" in
    wrapperUrl) wrapperUrl="$value" ;;
    esac
  done <"$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

  log "Downloading from: $wrapperUrl"

  if $cygwin; then
    wrapperJarPath=$(cygpath --path --windows "$wrapperJarPath")
  fi

  if command -v wget > /dev/null; then
    log "Found wget ... using wget"
    [ "$MVNW_VERBOSE" = true ] && QUIET="" || QUIET="--quiet"
    if [ -z "$MVNW_USERNAME" ] || [ -z "$MVNW_PASSWORD" ]; then
      wget $QUIET "$wrapperUrl" -O "$wrapperJarPath" || rm -f "$wrapperJarPath"
    else
      wget $QUIET --http-user="$MVNW_USERNAME" --http-password="$MVNW_PASSWORD" "$wrapperUrl" -O "$wrapperJarPath" || rm -f "$wrapperJarPath"
    fi
  elif command -v curl > /dev/null; then
    log "Found curl ... using curl"
    [ "$MVNW_VERBOSE" = true ] && QUIET="" || QUIET="--silent"
    if [ -z "$MVNW_USERNAME" ] || [ -z "$MVNW_PASSWORD" ]; then
      curl $QUIET -o "$wrapperJarPath" "$wrapperUrl" -f
    else
      curl $QUIET --user "$MVNW_USERNAME:$MVNW_PASSWORD" -o "$wrapperJarPath" "$wrapperUrl" -f
    fi
  else
    log "Falling back to using Java to download"
    javaSource="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/MavenWrapperDownloader.java"
    javaClass="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/MavenWrapperDownloader"
    if [ ! -e "$javaClass.class" ]; then
      if [ -e "$javaSource" ]; then
        javac "$javaSource"
      fi
    fi
    if [ -e "$javaClass.class" ]; then
      java -cp .mvn/wrapper MavenWrapperDownloader "$wrapperUrl" "$wrapperJarPath"
    fi
  fi
fi

##########################################################################################
# End of extension
##########################################################################################

# If specified, validate the SHA-256 sum of the Maven wrapper jar file
wrapperSha256Sum=""
while IFS="=" read -r key value; do
  value=$(echo "$value" | tr -d '\r')
  case "$key" in
  wrapperSha256Sum) wrapperSha256Sum=$value ;;
  esac
done <"$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ -n "$wrapperSha256Sum" ]; then
  wrapperSha256Result=false
  if command -v sha256sum > /dev/null; then
    if echo "$wrapperSha256Sum  $wrapperJarPath" | sha256sum -c > /dev/null 2>&1; then
      wrapperSha256Result=true
    fi
  elif command -v shasum > /dev/null; then
    if echo "$wrapperSha256Sum  $wrapperJarPath" | shasum -a 256 -c > /dev/null 2>&1; then
      wrapperSha256Result=true
    fi
  else
    echo "Checksum validation was requested but neither 'sha256sum' or 'shasum' are available."
    echo "Please install one of them. Alternatively, you can set the environment variable"
    echo "MVNW_DISABLE_CHECK to 'true' to disable the checksum validation and avoid this error."
    exit 1
  fi
  if [ $wrapperSha256Result = false ]; then
    echo "Error: Failed to validate Maven wrapper SHA-256, your Maven wrapper installation is corrupt."
    echo "Please re-run the Maven wrapper installation as described in https://maven.apache.org/wrapper/index.html"
    rm -f "$wrapperJarPath"
    exit 1
  fi
fi

MAVEN_OPTS="$(concat_lines "$MAVEN_PROJECTBASEDIR/.mvn/jvm.config") $MAVEN_OPTS"

# Provide a "standardized" way to retrieve the CLI args that will
# work with both Windows and non-Windows executions.
MAVEN_CMD_LINE_ARGS="$MAVEN_CONFIG $*"
export MAVEN_CMD_LINE_ARGS

WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

# shellcheck disable=SC2086
exec "$JAVACMD" \
  $MAVEN_OPTS \
  $MAVEN_DEBUG_OPTS \
  -classpath "$wrapperJarPath" \
  "-Dmaven.multiModuleProjectDirectory=${MAVEN_PROJECTBASEDIR}" \
  ${WRAPPER_LAUNCHER} $MAVEN_CONFIG "$@"

#!/bin/bash
set -e

IS_EOL=false
IS_UNSUPPORTED=false
IS_DEV=true

JAR_NAME="leaf-1.21.8"
CURRENT_TAG="ver-1.21.8"
RELEASE_NOTES="release_notes.md"

# Rename Leaf jar
mv ./leaf-server/build/libs/leaf-paperclip-1.21.8-R0.1-SNAPSHOT-mojmap.jar ./$JAR_NAME-${BUILD_NUMBER}.jar

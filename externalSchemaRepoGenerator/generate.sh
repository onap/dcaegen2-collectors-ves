#!/bin/bash -x

# ============LICENSE_START=======================================================
# VES
# ================================================================================
# Copyright (C) 2020 Nokia. All rights reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#      http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================


#Arguments renaming
ARGUMENTS=$#
REPO_URL=$1
BRANCHES=$2
SCHEMAS_LOCATION=$3
VENDOR=$4
CONFIGMAP_FILENAME=$5
CONFIGMAP_NAME=$6
SCHEMA_MAP_FILENAME=$7

#Constants
TMP_LOCATION=tmpRepo

# Indents each line of string by adding indentSize*indentString spaces on the beginning
# Optional argument is indentString level, default: 1
# correct usage example:
# echo "Sample Text" | indent 2
indentString() {
  local indentSize=2
  local indentString=1
  if [ -n "$1" ]; then indentString=$1; fi
  pr -to $(($indentString * $indentSize))
}

# Checks whether number of arguments is valid
# $1 is actual number of arguments
# $2 is expected number of arguments
checkArguments() {
  if [ $1 -ne $2 ]; then
    echo "Incorrect number of arguments"
    exit 1
  fi
}

# Creates file with name $CONFIGMAP_FILENAME
# Inserts ConfigMap metadata
addConfigMapMetadata() {
  echo "Creating ConfigMap spec file: "$CONFIGMAP_FILENAME
  cat << EOT > $CONFIGMAP_FILENAME
apiVersion: v1
kind: ConfigMap
metadata:
  name: $CONFIGMAP_NAME
  labels:
    name: $CONFIGMAP_NAME
  namespace: onap
data:
EOT
}

# For each selected branch:
#   clones it from repository,
#   adds selected schemas to ConfigMap spec
# Removes cloned branches
addSchemas() {
  for ACTUAL_BRANCH in $BRANCHES; do
    cloneRepo $ACTUAL_BRANCH
    addSchemasFromBranch $ACTUAL_BRANCH
  done
}

# Clones single branch $1 from $REPO_URL.
# $1 - branch name
cloneRepo() {
  checkArguments 1
  if [ -d $TMP_LOCATION/$1 ]; then
    echo "Skipping cloning repository."
    echo "Repository has already been cloned in the directory ./"$TMP_LOCATION"."
    echo "To redownload repository remove ./"$TMP_LOCATION"."
  else
    mkdir $TMP_LOCATION
    echo "Cloning repository with branch "$1
    git clone -b $1 --single-branch -q $REPO_URL $TMP_LOCATION/$1
  fi
}

# Adds schemas from single branch to spec
# $1 - branch name
addSchemasFromBranch() {
  checkArguments $# 1
  echo "Adding schemas from branch "$1" to spec"
  SCHEMAS=$(ls -g $TMP_LOCATION/$1/$SCHEMAS_LOCATION/*.yaml | awk '{print $NF}')
  for FILENAME in $SCHEMAS; do
    echo $(basename "$FILENAME")": |-" | indentString 1 >> $CONFIGMAP_FILENAME
    cat "$FILENAME" | indentString 2 >> $CONFIGMAP_FILENAME
  done
}

# Generates mapping file for collected schemas directly in spec
addMappingFile() {
  echo $SCHEMA_MAP_FILENAME": |-" | indentString 1 >> $CONFIGMAP_FILENAME
  echo "[" | indentString 2 >> $CONFIGMAP_FILENAME
  REPO_ENDPOINT=$(echo $REPO_URL | cut -d/ -f4- | rev | cut -d. -f2- | rev)

  for ACTUAL_BRANCH in $BRANCHES; do
    addMappingsFromBranch $ACTUAL_BRANCH
    #todo zrobić schema-map z wielu branchy
  done

  echo "]" | indentString 2 >> $CONFIGMAP_FILENAME
}

# Adds mappings from single branch directly to spec
# $1 - branch name
addMappingsFromBranch() {
  SCHEMAS=$(ls -g $TMP_LOCATION/$1/$SCHEMAS_LOCATION/*.yaml | awk '{print $NF}')
  for FILENAME in $SCHEMAS; do
    FILENAME_NO_TMP=$(echo $FILENAME | cut -d/ -f2-)
    PUBLIC_URL_SCHEMAS_LOCATION="${REPO_URL%.*}"
    PUBLIC_URL=$PUBLIC_URL_SCHEMAS_LOCATION"/blob/"$FILENAME_NO_TMP
    LOCAL_URL=$VENDOR/$REPO_ENDPOINT"/blob/"$FILENAME_NO_TMP

    echo "{" | indentString 3 >> $CONFIGMAP_FILENAME
    echo "\"publicURL\": \""$PUBLIC_URL"\"," | indentString 4 >> $CONFIGMAP_FILENAME
    echo "\"localURL\": \""$LOCAL_URL"\"" | indentString 4 >> $CONFIGMAP_FILENAME
    echo "}," | indentString 3 >> $CONFIGMAP_FILENAME
  done
}

# Cleans cloned repository branches
cleanTmpRepos() {
  rm -rf tmpRepo
}

main() {
  checkArguments $ARGUMENTS 7
  addConfigMapMetadata
  addSchemas
  addMappingFile
  cleanTmpRepos
}

main
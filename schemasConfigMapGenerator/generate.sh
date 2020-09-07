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

TMP_LOCATION=tmpRepo
CONFIGMAP_FILENAME=stndDefined-schemas-configmap-spec.yaml
CONFIGMAP_NAME=stnd-defined-configmap
REPO_URL=$1
BRANCH=$2
SCHEMAS_LOCATION=$3
VENDOR=$4

# Indents string by adding indentSize*indentString spaces on the beginning
# Optional argument is indentString level, default: 1
# correct usage example:
# echo "Sample Text" | indent 2
indentString() {
  local indentSize=2
  local indentString=1
  if [ -n "$1" ]; then indentString=$1; fi
  pr -to $(($indentString * $indentSize))
}

checkArguments() {
  if [ $1 -ne 4 ]; then
    echo "Incorrect number of arguments"
    exit 1
  fi
}

cloneRepo() {
  if [ -d $TMP_LOCATION ]; then
    echo "Skipping cloning repository."
    echo "Repository has already been cloned in the directory ./"$TMP_LOCATION"."
    echo "To redownload repository remove ./"$TMP_LOCATION"."
  else
    mkdir $TMP_LOCATION
    echo "Cloning repository"
    git clone -b $BRANCH --single-branch -q $REPO_URL $TMP_LOCATION
  fi
}

addConfigMapDescription() {
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

addSchemasToSpec() {
  SCHEMAS=$(ls -g $TMP_LOCATION/$SCHEMAS_LOCATION/*.yaml | awk '{print $NF}')
  for FILENAME in $SCHEMAS; do
      echo $(basename "$FILENAME")": |-" | indentString 1 >> $CONFIGMAP_FILENAME
      cat "$FILENAME" | indentString 2 >> $CONFIGMAP_FILENAME
  done
}

addSchemasMapToSpec() {
  echo "schema-map.json: |-" | indentString 1 >> $CONFIGMAP_FILENAME
  echo "[" | indentString 2 >> $CONFIGMAP_FILENAME
  REPO_ENDPOINT=$(echo $REPO_URL | cut -d/ -f4- | rev | cut -d. -f2- | rev)

  for FILENAME in $SCHEMAS; do
    FILENAME_NO_TMP=$(echo $FILENAME | cut -d/ -f2-)
    PUBLIC_URL_SCHEMAS_LOCATION="${REPO_URL%.*}"

    echo "{" | indentString 3 >> $CONFIGMAP_FILENAME
    echo "\"publicURL\": \""$PUBLIC_URL_SCHEMAS_LOCATION/$FILENAME_NO_TMP"\"," | indentString 4 >> $CONFIGMAP_FILENAME
    echo "\"localURL\": \""$VENDOR/$REPO_ENDPOINT/$FILENAME_NO_TMP"\"" | indentString 4 >> $CONFIGMAP_FILENAME
    echo "}," | indentString 3 >> $CONFIGMAP_FILENAME
  done

  echo "]" | indentString 2 >> $CONFIGMAP_FILENAME
}

uploadConfigMap() {
  MAX_SPEC_SIZE=262144
  SPEC_SIZE=$(stat --printf="%s" $CONFIGMAP_FILENAME)
  if [ $SPEC_SIZE -ge $MAX_SPEC_SIZE ]; then
    echo "ConfigMap spec file is too long for 'kubectl apply'. Actual spec length: "$SPEC_SIZE", max spec lenth: "$MAX_SPEC_SIZE
    echo "Creating new ConfigMap "$CONFIGMAP_NAME
    kubectl -n onap create -f $CONFIGMAP_FILENAME
    UPLOAD_RESULT=$?
    if [ $UPLOAD_RESULT == 1 ]; then
      echo "ConfigMap with name "$CONFIGMAP_NAME" already exists"
      echo "Removing old ConfigMap "$CONFIGMAP_NAME
      kubectl -n onap delete configmap $CONFIGMAP_NAME
      echo "Adding ConfigMap "$CONFIGMAP_NAME
      kubectl -n onap create -f $CONFIGMAP_FILENAME
    fi
  else
    echo "Applying ConfigMap "$CONFIGMAP_NAME
    kubectl -n onap apply -f $CONFIGMAP_FILENAME
  fi
  return $?
}

cleanTmpRepo() {
  rm -rf tmpRepo
}

main() {
  checkArguments $1
  cloneRepo
  addConfigMapDescription
  addSchemasToSpec
  addSchemasMapToSpec
  uploadConfigMap
  cleanTmpRepo
}

ARGUMENTS=$#
main $ARGUMENTS
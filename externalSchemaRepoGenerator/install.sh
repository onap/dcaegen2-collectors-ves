#!/bin/bash

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

CONFIGMAP_FILENAME=stndDefined-schemas-configmap-spec.yaml
CONFIGMAP_NAME=stnd-defined-configmap

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

uploadConfigMap
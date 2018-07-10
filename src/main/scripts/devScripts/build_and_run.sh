#!/bin/bash
###
# ============LICENSE_START=======================================================
# PROJECT
# ================================================================================
# Copyright (C) 2018 Nokia Networks Intellectual Property. All rights reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================
###


###
# A script that will build the image and run the container.
# Considerations:
#  - will use hosts network so it can access internet in 'proxy problem' scenarios
#  - name of the container -> VESCollector
#  - default ports exposed on host
###

# path to the location where pom.xml lives, using relative paths as it is easiest
PROJECT_ROOT=$(realpath ../../../..)
# if 'mvn' is not on path, pass a full path to it here
MVN_EXECUTABLE=mvn
# if 'docker' is not on path, pass a full path to it here
DOCKER_EXECUTABLE=docker
# produce the image and add into local docker repository
${MVN_EXECUTABLE} clean package -f ${PROJECT_ROOT}/pom.xml
# run it
${DOCKER_EXECUTABLE} run \
       --rm \
       --net=host \
       -it \
       -p 8080:8080/tcp \
       -p 8443:8443/tcp \
       --name VESCollector \
       onap/org.onap.dcaegen2.collectors.ves.vescollector:latest
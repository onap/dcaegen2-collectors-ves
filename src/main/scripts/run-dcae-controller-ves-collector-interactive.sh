#!/bin/bash

###
# ============LICENSE_START=======================================================
# PROJECT
# ================================================================================
# Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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


IMAGE="dcae-controller-ves-collector"
VER="latest"
HOST=$(hostname)
NAME="ves-collector-$HOST"
HOST=$(hostname -f)
CMD="/bin/bash"

HOST_VM_LOGDIR="/var/log/${HOST}-docker-${IMAGE}"

# remove the imate, interactive terminal, map exposed port
set -x
docker run --rm -it  -v ${HOST_VM_LOGDIR}/manager_ves-collector:/opt/app/manager/logs \
    -v ${HOST_VM_LOGDIR}/VEScollector:/opt/app/VEScollector/logs \
    -v /etc/dcae:/etc/dcae \
    --name ${NAME} ${IMAGE}:${VER} \
   ${CMD}

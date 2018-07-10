#!/bin/sh

###
# ============LICENSE_START=======================================================
# PROJECT
# ================================================================================
# Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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
source ${SCRIPTS_PATH}/logger.sh

if [ -z ${CONSUL_HOST} ] || [ -z ${CONFIG_BINDING_SERVICE} ] || [ -z ${HOSTNAME} ]; then
    log "Using standard controller (start-manager.sh)"
    /opt/app/manager/start-manager.sh
else
    log "Using DCAEGEN2 controller (VESrestfulCollector.sh)"
    ${SCRIPTS_PATH}/VESrestfulCollector.sh stop
    ${SCRIPTS_PATH}/VESrestfulCollector.sh start &
fi
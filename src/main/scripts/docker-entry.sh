#!/bin/bash
###
# ============LICENSE_START=======================================================
# PROJECT
# ================================================================================
# Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

# Redirect all stdout & stderr to a main log file, but also let it print into the console
# At the time this script is invoked, these directories and files do not exist yet, so we need to create them
mkdir -p ${LOGS_PATH}
touch ${MAIN_LOG_FILE}
exec &> >(tee -a ${MAIN_LOG_FILE})

log "Main application entry-point invoked"

if [ ! -z ${COLLECTOR_IP} ]; then
    log "Collector ip (${COLLECTOR_IP}) (env var 'COLLECTOR_IP') found, adding entry to /etc/hosts"
    echo ${COLLECTOR_IP}  $(hostname).dcae.simpledemo.onap.org >> /etc/hosts
fi

if [ ! -z ${DMAAPHOST} ]; then
    if [ -z "$(echo ${DMAAPHOST} | sed -e 's/[0-9\.]//g')" ]; then
        log "DMaaP host (${DMAAPHOST}) (env var 'DMAAPHOST') found, adding entry to /etc/hosts"
        echo "${DMAAPHOST}  onap-dmaap" >> /etc/hosts
    else
        log "DMaaP host (${DMAAPHOST}) (env var 'DMAAPHOST') found, adding entry to /etc/host.aliases"
        echo "onap-dmaap ${DMAAPHOST}" >> /etc/host.aliases
    fi
else
	logWarn "DMaaP host (env var 'DMAAPHOST') is missing. Events will not be published to DMaaP"
fi

log "Scheduling application to be started, looping indefinitely to hold the docker process"
${SCRIPTS_PATH}/VESrestfulCollector.sh stop
${SCRIPTS_PATH}/VESrestfulCollector.sh start &

# Add below if config polling should be enabled. More specific to K8 deployment in ONAP
if [ ! -z ${CBSPOLLTIMER} ]; then
    log "Configuration poll time (${CBSPOLLTIMER}) (env var 'CBSPOLLTIMER') found, enabling configuration polling from CBS"
    ${SCRIPTS_PATH}/VESConfigPoller.sh &
fi

while true; do sleep 1000; done

#!/bin/bash
###
# ============LICENSE_START=======================================================
# PROJECT
# ================================================================================
# Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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
source bin/logger.sh

# This scripts job is to continuously run in background and watch for changes in collector.properties
# and in case it has changed, restart application.
# collector.properties (and DmaapConfig.json) is being updated periodically by calling for configuration from CBS and it is
# done inside the VESCollector application itself.
# Configuration poller can be run regardless of deployment type.
# It will always check for changes in collector.properties and in deployment scenario,
# where dynamic configuration should not be used, necessary environment
# variables that are needed (consul host, cbs name, app name) will be missing, and java app will
# not update the configuration files so restart won't be triggered.

# Start after a while, because once the application starts, it might happen that
# it fetched new configuration. In that case, the application will already be started with newest config, there would
# be no point in restarting it once again.
sleep 2m

while true
do
    sleep 1m
    if [[ $(find etc/collector.properties -mmin -1 -print) ]]; then
        log "Found change in collector.properties, updating keystore and restarting application"
        bin/appController.sh restart
    fi
done


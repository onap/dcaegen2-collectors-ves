#!/bin/sh
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
#echo \$COLLECTOR_IP  \$(hostname).dcae.simpledemo.openecomp.org >> /etc/hosts
if [ -z "$CONSUL_HOST" ] || [ -z "$CONFIG_BINDING_SERVICE" ] || [ -z "$HOSTNAME" ]; then
                echo "INFO: USING STANDARD ALONE CONFIGURATION SETUP; DMAAP PUBLISH NOT SUPPORTED"
               # /opt/app/manager/start-manager.sh
else
                echo "INFO: USING DCAEGEN2 CONTROLLER"
fi
                /opt/app/VESCollector/bin/VESrestfulCollector.sh stop
                /opt/app/VESCollector/bin/VESrestfulCollector.sh start &
#while true; do sleep 1000; done


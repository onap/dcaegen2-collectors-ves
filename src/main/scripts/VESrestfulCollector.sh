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
source bin/logger.sh

start() {
    log "Starting application"
    appPids=`pidof java`

    if [ ! -z ${appPids} ]; then
        logWarn "Tried to start an application, but it is already running on PID(s): ${appPids}. Startup aborted."
        exit 1
    fi

    ${JAVA_HOME}/bin/java -cp "etc:lib/*" \
      -Xms256m -Xmx512m \
      -XX:ErrorFile=logs/java_error%p.log \
      -XX:+HeapDumpOnOutOfMemoryError \
      -Dhttps.protocols=TLSv1.1,TLSv1.2 \
      org.onap.dcae.VesApplication $* & &>> logs/collector.log
}

stop() {
    log "Stopping application"
    appPids=`pidof java`

    if [ ! -z ${appPids} ]; then
        echo "Killing java PID(s): ${appPids}"
        kill -9 ${appPids}
        sleep 5
        if [ ! $(pidof java) ]; then
            log "Application stopped"
        else
            logWarn "Application did not stop after 5 seconds"
        fi
    else
        logWarn "Tried to stop an application, but it was not running";
    fi
}

collector_configupdate() {
    if [ -z ${CONSUL_HOST} ] || [ -z ${CONFIG_BINDING_SERVICE} ] || [ -z ${HOSTNAME} ]; then
        log "Using standard controller configuration (no dynamic configuration done)"
    else
        ${JAVA_HOME}/bin/java -cp "etc:lib/*" org.onap.dcae.controller.FetchDynamicConfig $*

        if [ $? -ne 0 ]; then
            logWarn "Failed to fetch dynamic configuration from consul into container /opt/app/KV-Configuration.json"
        else
            log "Dynamic config fetched and written successfully into container /opt/app/KV-Configuration.json"
        fi

        if [ -f /opt/app/KV-Configuration.json ]; then
            ${JAVA_HOME}/bin/java -cp "etc:lib/*" org.onap.dcae.controller.LoadDynamicConfig $*
            if [ $? -ne 0 ]; then
                echo "ERROR: Failed to update dynamic configuration into Application"
            else
                echo "INFO: Dynamic config updated successfully into VESCollector configuration!"
            fi
            paramName="collector.keystore.alias"
            localpropertyfile="etc/collector.properties"
            tmpfile="etc/collector.properties.tmp"
            keystore=`grep collector.keystore.file.location $localpropertyfile | tr -d '[:space:]' | cut -d"=" -f2`
            keypwdfile=`grep collector.keystore.passwordfile $localpropertyfile | tr -d '[:space:]' | cut -d"=" -f2`
            echo "/usr/bin/keytool -list -keystore $keystore < $keypwdfile | grep "PrivateKeyEntry" | cut -d"," -f1"
            tmpalias=`/usr/bin/keytool -list -keystore $keystore < $keypwdfile | grep "PrivateKeyEntry" | cut -d"," -f1`
            alias=`echo $tmpalias | cut -d":" -f2`
            sed "s~$paramName=.*~$paramName=$alias~g" $localpropertyfile > $tmpfile
            echo `cat $tmpfile > $localpropertyfile`
            rm $tmpfile
            log "Keystore alias updated"
        else
            logWarn "Configuration file /opt/app/KV-Configuration.json missing"
        fi
    fi
}

case $1 in
  "start") collector_configupdate; start ;;
  "stop")  stop ;;
  *)       echo "Bad usage. Should be: /bin/bash <this> start/stop"
esac


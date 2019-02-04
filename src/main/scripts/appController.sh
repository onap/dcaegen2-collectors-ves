

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

#updateKeystore() {
#    log "Updating keystore configuration"
#    aliasParameterName="collector.keystore.alias"
#    originalPropertyFile="etc/collector.properties"
#    temporaryPropertyFile="etc/collector.properties.tmp"
#    keystorePath=`grep collector.keystore.file.location ${originalPropertyFile} | tr -d '[:space:]' | cut -d"=" -f2`
#    keystorePasswordFile=`grep collector.keystore.passwordfile ${originalPropertyFile} | tr -d '[:space:]' | cut -d"=" -f2`
#    temporaryAlias=`/usr/bin/keytool -list -keystore $keystorePath < $keystorePasswordFile | grep "PrivateKeyEntry" | cut -d"," -f1`
#    newAlias=`echo $temporaryAlias | cut -d":" -f2`
#    sed "s~$aliasParameterName=.*~$aliasParameterName=$newAlias~g" ${originalPropertyFile} > ${temporaryPropertyFile}
#    echo `cat ${temporaryPropertyFile} > ${originalPropertyFile}`
#    rm ${temporaryPropertyFile}
#    log "Keystore configuration updated"
#}
#
#tryToPollConfiguration() {
#    log "Trying to poll configuration from CBS before application starts"
#    ${JAVA_HOME}/bin/java -cp "etc:lib/*" org.onap.dcae.controller.PreAppStartupConfigUpdater
#}

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

case $1 in
  "start")    start ;;
  "stop")     stop ;;
  "restart")  stop; start ;;
  *)          echo "Bad usage. Should be: /bin/bash <this> start/stop"
esac


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
      org.onap.dcae.VesApplication $* &
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


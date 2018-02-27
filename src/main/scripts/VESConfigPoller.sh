#!/bin/sh -x
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
# redirect stdout/stderr to a file
#exec &> /opt/app/VESCollector/logs/console.txt

usage() {
        echo "VESConfigPoller.sh"
}


## Remove singel execution logic (loop 0)
## On configupdate function, remove LoadDynamicConfig and invoke VESrestfulCollector stop/start

BASEDIR=/opt/app/VESCollector/
CONFIGFILENAME=/opt/app/KV-Configuration.json


collector_configupdate() {

        echo `date +"%Y%m%d.%H%M%S%3N"` - VESConfigPoller.sh:collector_configupdate
        if [ -z "$CONSUL_HOST" ] || [ -z "$CONFIG_BINDING_SERVICE" ] || [ -z "$HOSTNAME" ]; then
                echo "INFO: USING STANDARD CONTROLLER CONFIGURATION"
        else
            # move into base directory
            cd $BASEDIR

            CONFIG_FETCH=org.onap.dcae.controller.FetchDynamicConfig
            $JAVA -cp "etc${PATHSEP}lib/*"  $CONFIG_FETCH $*
            if [ $? -ne 0 ]; then
                echo "ERROR: Failed to fetch dynamic configuration from consul into container $CONFIGFILENAME"
            else
               echo "INFO: Dynamic config fetched successfully"
            fi
                sleep 10s
                FLAG=0

            if [ -f $CONFIGFILENAME ]; then
                if [[ $(find $CONFIGFILENAME -mmin -$CBSPOLLTIMER -print) ]]; then
                        echo "File  $CONFIGFILENAME  is updated under $CBSPOLLTIMER minutes; Loader to be invoked"
                        FLAG=1
                else
                        echo "File  $CONFIGFILENAME  NOT updated in last $CBSPOLLTIMER minutes; no configuration update!"
                        FLAG=0
                fi

                if [ $FLAG -eq 1 ]; then
                        echo "INFO: CONFIGFILE updated; triggering restart"
                        /opt/app/VESCollector/bin/VESrestfulCollector.sh stop
                        /opt/app/VESCollector/bin/VESrestfulCollector.sh start &
                else
                        echo "INFO: CONFIGFILE load skipped"
                fi
            else
                echo "ERROR: Configuration file $CONFIGFILENAME missing"
            fi
        fi
}



if [ -z "$CBSPOLLTIMER" ]; then
        echo "CBSPOLLTIMER not set; set this to polling frequency in minutes"
        exit 1
fi


## Pre-setting

# use JAVA_HOME if provided
if [ -z "$JAVA_HOME" ]; then
        echo "ERROR: JAVA_HOME not setup"
        echo "Startup Aborted!!"
        exit 1
        #JAVA=java
else
        JAVA=$JAVA_HOME/bin/java
fi



# determine a path separator that works for this platform
PATHSEP=":"
case "$(uname -s)" in

        Darwin)
                ;;

         Linux)
                ;;

         CYGWIN*|MINGW32*|MSYS*)
                PATHSEP=";"
                ;;

        *)
                ;;
esac



##Run in loop the config pull and check
while true
do
        sleep $(echo $CBSPOLLTIMER)m
        collector_configupdate | tee -a ${BASEDIR}/logs/console.txt
done


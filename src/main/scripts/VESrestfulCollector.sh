#!/bin/sh

###
# ============LICENSE_START=======================================================
# PROJECT
# ================================================================================
# Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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
        echo "VESrestfulCollector.sh <start/stop>"
}


BASEDIR=/opt/app/VESCollector/

collector_start() {
        echo `date +"%Y%m%d.%H%M%S%3N"` - collector_start | tee -a ${BASEDIR}/logs/console.txt
        collectorPid=`pidof org.onap.dcae.commonFunction`

        if [ ! -z "$collectorPid" ]; then
                echo  "WARNING: VES Restful Collector already running as PID $collectorPid" | tee -a ${BASEDIR}/logs/console.txt
                echo  "Startup Aborted!!!" | tee -a ${BASEDIR}/logs/console.txt
                exit 1
        fi


        # run java. The classpath is the etc dir for config files, and the lib dir
        # for all the jars.
        #cd /opt/app/VESCollector/
        cd ${BASEDIR}
        #nohup $JAVA -cp "etc${PATHSEP}lib/*" $JAVA_OPTS -Dhttps.protocols=TLSv1.1,TLSv1.2 $MAINCLASS $* &
		nohup $JAVA -cp "etc${PATHSEP}lib/*" -Xms256m -Xmx512m -XX:ErrorFile=/opt/app/VESCollector/logs/java_error%p.log -XX:+HeapDumpOnOutOfMemoryError -Dhttps.protocols=TLSv1.1,TLSv1.2 $MAINCLASS $* &
        if [ $? -ne 0 ]; then
                echo "VES Restful Collector has been started!!!" | tee -a ${BASEDIR}/logs/console.txt
        fi


}

collector_stop() {
         echo `date +"%Y%m%d.%H%M%S%3N"` - collector_stop
         collectorPid=`pidof org.onap.dcae.commonFunction`
         if [ ! -z "$collectorPid" ]; then
                echo "Stopping PID $collectorPid"

                kill -9 $collectorPid
                sleep 5
                if [ ! "$(pidof org.onap.dcae.commonFunction)" ]; then
                         echo "VES Restful Collector has been stopped!!!"
                else
                         echo "VES Restful Collector is being stopped!!!"
                fi
         else
                echo  "WARNING: No VES Collector instance is currently running";
                exit 1
         fi


}

collector_configupdate() {

        echo `date +"%Y%m%d.%H%M%S%3N"` - collector_configupdate
        if [ -z "$CONSUL_HOST" ] || [ -z "$CONFIG_BINDING_SERVICE" ] || [ -z "$HOSTNAME" ]; then
                echo "INFO: USING STANDARD CONTROLLER CONFIGURATION"
        else

            echo "INFO: DYNAMIC CONFIG INTERFACE SUPPORTED"
            # move into base directory

            #BASEDIR=`dirname $0`
            #cd $BASEDIR/..
            cd /opt/app/VESCollector

            CONFIG_FETCH=org.onap.dcae.controller.FetchDynamicConfig
            $JAVA -cp "etc${PATHSEP}lib/*"  $CONFIG_FETCH $*
            if [ $? -ne 0 ]; then
                echo "ERROR: Failed to fetch dynamic configuration from consul into container /opt/app/KV-Configuration.json"
            else
                echo "INFO: Dynamic config fetched and written successfully into container /opt/app/KV-Configuration.json"
            fi


            if [ -f /opt/app/KV-Configuration.json ]; then

                    CONFIG_UPDATER=org.onap.dcae.controller.LoadDynamicConfig
                    $JAVA -cp "etc${PATHSEP}lib/*"  $CONFIG_UPDATER $*
                    if [ $? -ne 0 ]; then
                        echo "ERROR: Failed to update dynamic configuration into Application"
                    else
                        echo "INFO: Dynamic config updated successfully into VESCollector configuration!"
                    fi

					# Identify alias names from keystore and password provided

            		paramName="collector.keystore.alias"
					localpropertyfile="/opt/app/VESCollector/etc/collector.properties"
					tmpfile="/opt/app/VESCollector/etc/collector.properties.tmp"

					keystore=`grep collector.keystore.file.location $localpropertyfile | tr -d '[:space:]' | cut -d"=" -f2`
					keypwdfile=`grep collector.keystore.passwordfile $localpropertyfile | tr -d '[:space:]' | cut -d"=" -f2`

					echo "/usr/bin/keytool -list -keystore $keystore < $keypwdfile | grep "PrivateKeyEntry" | cut -d"," -f1"
                    tmpalias=`/usr/bin/keytool -list -keystore $keystore < $keypwdfile | grep "PrivateKeyEntry" | cut -d"," -f1`
                    echo "tmpalias:" $tmpalias
                    alias=`echo $tmpalias | cut -d":" -f2`
                    echo "alias:" $alias
                    sed "s~$paramName=.*~$paramName=$alias~g" $localpropertyfile > $tmpfile
                    echo `cat $tmpfile > $localpropertyfile`
                    rm $tmpfile
                	echo "INFO: Keystore alias updated into configuration"

            else
                echo "ERROR: Configuration file /opt/app/KV-Configuration.json missing"
            fi

        fi
}


## Check usage
if [ $# -ne 1 ]; then
        usage
        exit
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


MAINCLASS=org.onap.dcae.commonFunction.CommonStartup

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




case $1 in
        "start")
                collector_configupdate | tee -a ${BASEDIR}/logs/console.txt
                collector_start
                ;;
        "stop")
                collector_stop | tee -a ${BASEDIR}/logs/console.txt
                ;;
        *)
                usage
                ;;
esac


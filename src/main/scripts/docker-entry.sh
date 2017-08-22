#!/bin/sh
if [ -z "$CONSUL_HOST" ] || [ -z "$CONFIG_BINDING_SERVICE" ] || [ -z "$HOSTNAME" ]; then
                echo "INFO: USING STANDARD CONTROLLER"
                /opt/app/manager/start-manager.sh
else
                echo "INFO: USING DCAEGEN2 CONTROLLER"
                /opt/app/VESCollector/bin/VESrestfulCollector.sh stop
                /opt/app/VESCollector/bin/VESrestfulCollector.sh start &
fi
#while true; do sleep 1000; done

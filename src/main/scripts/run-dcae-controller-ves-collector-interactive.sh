#!/bin/bash

IMAGE="dcae-controller-ves-collector"
VER="latest"
HOST=$(hostname)
NAME="ves-collector-$HOST"
HOST=$(hostname -f)
CMD="/bin/bash"

HOST_VM_LOGDIR="/var/log/${HOST}-docker-${IMAGE}"

# remove the imate, interactive terminal, map exposed port
set -x
docker run --rm -it  -v ${HOST_VM_LOGDIR}/manager_ves-collector:/opt/app/manager/logs \
    -v ${HOST_VM_LOGDIR}/VEScollector:/opt/app/VEScollector/logs \
    -v /etc/dcae:/etc/dcae \
    --name ${NAME} ${IMAGE}:${VER} \
   ${CMD}

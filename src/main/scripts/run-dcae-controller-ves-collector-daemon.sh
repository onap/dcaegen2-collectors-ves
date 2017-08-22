#!/bin/bash

IMAGE="dcae-controller-ves-collector"
VER="latest"
HOST=$(hostname)
NAME="ves-collector-$HOST"
HOST=$(hostname -f)

HOST_VM_LOGDIR="/var/log/${HOST}-docker"

CMD="/bin/bash"
# remove the imate, interactive terminal, map exposed port
set -x
#docker run -d -p 8080:8080/tcp -p 8443:8443/tcp -P --name ${NAME} ${IMAGE}:${VER}
#docker run -td --name ${NAME} ${IMAGE}:${VER} ${CMD}
#docker run -td --name ${NAME} ${IMAGE}:${VER}
docker run -td -p 8080:8080/tcp -p 8443:8443/tcp -P --name ${NAME} ${IMAGE}:${VER} ${CMD}


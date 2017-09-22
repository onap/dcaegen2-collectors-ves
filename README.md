DCAE VESCollector
======================================

This is the repository for VES Collector for Open DCAE.

### Build Instructions

This project is organized as a mvn project for a jar package.

```
git clone ssh://git@<repo-address>:dcae-collectors/OpenVESCollector.git
mvn clean install
```

### Docker Image

The jar file is bundled into a docker image installed by the DCAE Controller. Following is the process to creating the image

#### Set up the packaging environment
1. Extract the VESCollector code and do mvn build
```
$ git clone ssh://git@<repo-address>:dcae-collectors/OpenVESCollector.git
```

2. Once the collector build is successful build dcae-controller
```
BASE_WS="/var/lib/jenkins/workspace"
PROJECT="build-dcae-controller"
DCM_DIR="dcae-org.onap.dcae.controller/dcae-controller-service-standardeventcollector-manager/target/"
ARTIFACT="dcae-controller-service-standardeventcollector-manager-0.1.0-SNAPSHOT-runtime.zip"
DCM_AR="${BASE_WS}/${PROJECT}/${DCM_DIR}/${ARTIFACT}"
echo "WORKSPACE: ${WORKSPACE}"
if [ ! -f "${DCM_AR}" ]
then
	echo "FATAL error cannot locate ${DCM_AR}"
    exit 2
fi
TARGET=${WORKSPACE}/target
STAGE=${TARGET}/stage
DCM_DIR=${STAGE}/opt/app/manager
[ ! -d ${DCM_DIR} ] && mkdir -p ${DCM_DIR}
unzip -qo -d ${DCM_DIR} ${DCM_AR}
```
3.  Get the VES collector Service manager artifacts.
```
DCM_DIR=${WORKSPACE}/target/stage/opt/app/manager
[ -f "${DCM_DIR}/start-manager.sh" ] && exit 0
cat <<'EOF' > ${DCM_DIR}/start-manager.sh
#!/bin/bash
MAIN=org.openecomp.dcae.controller.service.standardeventcollector.servers.manager.DcaeControllerServiceStandardeventcollectorManagerServer
ACTION=start
WORKDIR=/opt/app/manager
LOGS="${WORKDIR}/logs"
[ ! -d $LOGS ] && mkdir -p $LOGS
echo 10.0.4.102 $(hostname).dcae.simpledemo.openecomp.org >> /etc/hosts
exec java -cp ./config:./lib:./lib/*:./bin ${MAIN} ${ACTION} > logs/manager.out 2>logs/manager.err
EOF
chmod 775 ${DCM_DIR}/start-manager.sh
```
3.	Obtain the required packages to be included in docker
```
cat <<'EOF' > ${WORKSPACE}/target/stage/Dockerfile
FROM ubuntu:14.04
MAINTAINER dcae@lists.openecomp.org
WORKDIR /opt/app/manager
ENV HOME /opt/app/SEC
ENV JAVA_HOME /usr
RUN apt-get update && apt-get install -y \
        bc \
        curl \
        telnet \
        vim \
        netcat \
        openjdk-7-jdk
COPY opt /opt
EXPOSE 9999
CMD [ "/opt/app/manager/start-manager.sh" ]
EOF
```
4.	Extract VES collector jar and copy required directory into image build directory
```
AR=${WORKSPACE}/target/OpenVESCollector-0.0.1-SNAPSHOT-bundle.tar.gz
STAGE=${WORKSPACE}/target/stage
APP_DIR=${STAGE}/opt/app/SEC
[ -d ${STAGE}/opt/app/OpenVESCollector-0.0.1-SNAPSHOT ] && rm -rf ${STAGE}/opt/app/OpenVESCollector-0.0.1-SNAPSHOT
[ ! -f $APP_DIR ] && mkdir -p ${APP_DIR}
gunzip -c ${AR} | tar xvf - -C ${APP_DIR} --strip-components=1
# lji: removal of ^M in the VES startup script
sed -i 's/\r$//g' ${APP_DIR}/bin/SErestfulCollector.sh
#find ${APP_DIR} -name "*.sh" -print0 |xargs -0 sed -i 's/\r$//g'
```
#### Create the Docker image and push package to the OpenECOMP Nexus distribution server
```
#
# build the docker image. tag and then push to the remote repo
#
IMAGE="dcae-controller-common-event"
TAG="latest"
LFQI="${IMAGE}:${TAG}"
REPO="ecomp-nexus:51212"
RFQI="${REPO}/${LFQI}"
BUILD_PATH="${WORKSPACE}/target/stage"
# build a docker image
docker build --rm -t ${LFQI} ${BUILD_PATH}
# tag
docker tag ${LFQI} ${RFQI}
# push to remote repo
docker push ${RFQI}
```

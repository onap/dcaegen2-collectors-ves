#!/bin/bash
#
#
# 1 build the docker image with both service manager and ves collector
# 2 tag and then push to the remote repo if not verify
#

phase=$1

VERSION=$(xpath -e '//project/version/text()' 'pom.xml')
VERSION=${VERSION//\"/}
EXT=$(echo "$VERSION" | rev | cut -s -f1 -d'-' | rev)
if [ -z "$EXT" ]; then
  EXT="STAGING"
fi
case $phase in
  verify|merge)
    if [ "$EXT" != 'SNAPSHOT' ]; then
      echo "$phase job only takes SNAPSHOT version, got \"$EXT\" instead"
      exit 1
    fi
    ;;
  release)
    if [ ! -z "$EXT" ] && [ "$EXT" != 'STAGING' ]; then
      echo "$phase job only takes STAGING or pure numerical version, got \"$EXT\" instead"
      exit 1
    fi
    ;;
  *)
    echo "Unknown phase \"$phase\""
    exit 1
esac
echo "Running \"$phase\" job for version \"$VERSION\""


# DCAE Controller service manager for VES collector
DCM_AR="${WORKSPACE}/manager.zip"
if [ ! -f "${DCM_AR}" ]
then
    echo "FATAL error cannot locate ${DCM_AR}"
    exit 2
fi

# unarchive the service manager
TARGET="${WORKSPACE}/target"
STAGE="${TARGET}/stage"
DCM_DIR="${STAGE}/opt/app/manager"
[ ! -d "${DCM_DIR}" ] && mkdir -p "${DCM_DIR}"
unzip -qo -d "${DCM_DIR}" "${DCM_AR}"

# unarchive the collector
AR=${WORKSPACE}/target/VESCollector-${VERSION}-bundle.tar.gz
APP_DIR=${STAGE}/opt/app/VESCollector

[ -d "${STAGE}/opt/app/VESCollector-${VERSION}" ] && rm -rf "${STAGE}/opt/app/VESCollector-${VERSION}"

[ ! -f "${APP_DIR}" ] && mkdir -p "${APP_DIR}"

gunzip -c "${AR}" | tar xvf - -C "${APP_DIR}" --strip-components=1

#
# generate the manager start-up.sh
#
## [ -f "${DCM_DIR}/start-manager.sh" ] && exit 0

cat <<EOF > "${DCM_DIR}/start-manager.sh"
#!/bin/bash

MAIN=org.openecomp.dcae.controller.service.standardeventcollector.servers.manager.DcaeControllerServiceStandardeventcollectorManagerServer
ACTION=start

WORKDIR=/opt/app/manager

LOGS=\$WORKDIR/logs

mkdir -p \$LOGS

cd \$WORKDIR

echo \$COLLECTOR_IP  \$(hostname).dcae.simpledemo.openecomp.org >> /etc/hosts

if [ ! -e config ]; then
        echo no configuration directory setup: \$WORKDIR/config
        exit 1
fi

exec java -cp ./config:./lib:./lib/*:./bin \$MAIN \$ACTION > logs/manager.out 2>logs/manager.err

EOF

chmod 775 "${DCM_DIR}/start-manager.sh"


#
# generate docker file
#
cat <<EOF > "${STAGE}/Dockerfile"
FROM ubuntu:14.04

MAINTAINER dcae@lists.openecomp.org

WORKDIR /opt/app/manager

ENV HOME /opt/app/VESCollector
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

#
# build the docker image. tag and then push to the remote repo
#
IMAGE='openecomp/dcae-collector-common-event'
VERSION="${VERSION//[^0-9.]/}"
VERSION2=$(echo "$VERSION" | cut -f1-2 -d'.')

TIMESTAMP="-$(date +%C%y%m%dT%H%M%S)"
LFQI="${IMAGE}:${VERSION}${TIMESTAMP}"
BUILD_PATH="${WORKSPACE}/target/stage"
# build a docker image
echo docker build --rm -t "${LFQI}" "${BUILD_PATH}"
docker build --rm -t "${LFQI}" "${BUILD_PATH}"

case $phase in
  verify)
    exit 0
  ;;
esac

#
# push the image
#
# io registry  DOCKER_REPOSITORIES="nexus3.openecomp.org:10001 \
# release registry                   nexus3.openecomp.org:10002 \
# snapshot registry                   nexus3.openecomp.org:10003"
# staging registry                   nexus3.openecomp.org:10004"
case $EXT in
SNAPSHOT|snapshot)
    #REPO='nexus3.openecomp.org:10003'
    REPO='nexus3.onap.org:10003'
    EXT="-SNAPSHOT"
    ;;
STAGING|staging)
    #REPO='nexus3.openecomp.org:10003'
    REPO='nexus3.onap.org:10003'
    EXT="-STAGING"
    ;;
"")
    #REPO='nexus3.openecomp.org:10002'
    REPO='nexus3.onap.org:10002'
    EXT=""
    echo "version has no extension, intended for release, in \"$phase\" phase. donot do release here"
    exit 1
    ;;
*)
    echo "Unknown extension \"$EXT\" in version"
    exit 1
    ;;
esac

OLDTAG="${LFQI}"
PUSHTAGS="${REPO}/${IMAGE}:${VERSION}${EXT}${TIMESTAMP} ${REPO}/${IMAGE}:latest ${REPO}/${IMAGE}:${VERSION2}${EXT}-latest"
for NEWTAG in ${PUSHTAGS}
do
   echo "tagging ${OLDTAG} to ${NEWTAG}"
   docker tag "${OLDTAG}" "${NEWTAG}"
   echo "pushing ${NEWTAG}"
   docker push "${NEWTAG}"
   OLDTAG="${NEWTAG}"
done

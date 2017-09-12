#!/bin/bash
###
# ============LICENSE_START=======================================================
# PROJECT
# ================================================================================
# Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

# 1 build the docker image for ves collector
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

# unarchive the service manager
TARGET="${WORKSPACE}/target"
STAGE="${TARGET}/stage"
BASE_DIR="${STAGE}/opt/app"

# unarchive the collector
AR=${WORKSPACE}/target/VESCollector-${VERSION}-bundle.tar.gz
APP_DIR=${STAGE}/opt/app/VESCollector

[ -d "${STAGE}/opt/app/VESCollector-${VERSION}" ] && rm -rf "${STAGE}/opt/app/VESCollector-${VERSION}"

[ ! -f "${APP_DIR}" ] && mkdir -p "${APP_DIR}"

gunzip -c "${AR}" | tar xvf - -C "${APP_DIR}" --strip-components=1


if [ ! -f "${APP_DIR}/bin/docker-entry.sh" ]
then
		echo "FATAL error cannot locate ${APP_DIR}/bin/docker-entry.sh"
		exit 2
fi
cp -p ${APP_DIR}/bin/docker-entry.sh ${BASE_DIR}/docker-entry.sh
chmod 755 "${BASE_DIR}/docker-entry.sh"




#
# generate docker file
#
if [ ! -f "${APP_DIR}/Dockerfile" ]
then
		echo "FATAL error cannot locate ${APP_DIR}/Dockerfile"
		exit 2
fi
cp -p ${APP_DIR}/Dockerfile ${STAGE}/Dockerfile


#
# build the docker image. tag and then push to the remote repo
#
IMAGE='onap/dcaegen2-ves-collector'
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
    REPO='nexus3.onap.org:10003'
    EXT="-SNAPSHOT"
    ;;
STAGING|staging)
    REPO='nexus3.onap.org:10003'
    EXT="-STAGING"
    ;;
"")
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


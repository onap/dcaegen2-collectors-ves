#!/bin/bash

# ================================================================================
# Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================
#
# ECOMP is a trademark and service mark of AT&T Intellectual Property.

set -ex


echo "running script: [$0] for module [$1] at stage [$2]"

MVN_PROJECT_MODULEID="$1"
MVN_PHASE="$2"


PROJECT_ROOT=$(dirname $0)

FQDN="${MVN_PROJECT_GROUPID}.${MVN_PROJECT_ARTIFACTID}"
if [ "$MVN_PROJECT_MODULEID" == "__" ]; then
  MVN_PROJECT_MODULEID=""
fi

if [[ "$MVN_PROJECT_VERSION" == *SNAPSHOT ]]; then
  echo "=> for SNAPSHOT artifact build"
  MVN_DEPLOYMENT_TYPE='SNAPSHOT'
else
  echo "=> for STAGING/RELEASE artifact build"
  MVN_DEPLOYMENT_TYPE='STAGING'
fi
echo "MVN_DEPLOYMENT_TYPE is             [$MVN_DEPLOYMENT_TYPE]"


TIMESTAMP=$(date +%C%y%m%dT%H%M%S)

# expected environment variables
if [ -z "${MVN_NEXUSPROXY}" ]; then
    echo "MVN_NEXUSPROXY environment variable not set.  Cannot proceed"
    exit
fi
MVN_NEXUSPROXY_HOST=$(echo "$MVN_NEXUSPROXY" |cut -f3 -d'/' | cut -f1 -d':')
echo "=> Nexus Proxy at $MVN_NEXUSPROXY_HOST, $MVN_NEXUSPROXY"

if [ -z "$WORKSPACE" ]; then
    WORKSPACE=$(pwd)
fi

if [ -z "$SETTINGS_FILE" ]; then
    echo "SETTINGS_FILE environment variable not set.  Cannot proceed"
    exit
fi
   


# mvn phase in life cycle
MVN_PHASE="$2"


echo "MVN_PROJECT_MODULEID is            [$MVN_PROJECT_MODULEID]"
echo "MVN_PHASE is                       [$MVN_PHASE]"
echo "MVN_PROJECT_GROUPID is             [$MVN_PROJECT_GROUPID]"
echo "MVN_PROJECT_ARTIFACTID is          [$MVN_PROJECT_ARTIFACTID]"
echo "MVN_PROJECT_VERSION is             [$MVN_PROJECT_VERSION]"
echo "MVN_NEXUSPROXY is                  [$MVN_NEXUSPROXY]"
echo "MVN_RAWREPO_BASEURL_UPLOAD is      [$MVN_RAWREPO_BASEURL_UPLOAD]"
echo "MVN_RAWREPO_BASEURL_DOWNLOAD is    [$MVN_RAWREPO_BASEURL_DOWNLOAD]"
MVN_RAWREPO_HOST=$(echo "$MVN_RAWREPO_BASEURL_UPLOAD" | cut -f3 -d'/' |cut -f1 -d':')
echo "MVN_RAWREPO_HOST is                [$MVN_RAWREPO_HOST]"
echo "MVN_RAWREPO_SERVERID is            [$MVN_RAWREPO_SERVERID]"
echo "MVN_DOCKERREGISTRY_DAILY is        [$MVN_DOCKERREGISTRY_DAILY]"
echo "MVN_DOCKERREGISTRY_RELEASE is      [$MVN_DOCKERREGISTRY_RELEASE]"


source "${PROJECT_ROOT}"/mvn-phase-lib.sh 


# Customize the section below for each project
case $MVN_PHASE in
clean)
  echo "==> clean phase script"
  clean_templated_files
  clean_tox_files
  rm -rf ./venv-* ./*.wgn ./site
  ;;
generate-sources)
  echo "==> generate-sources phase script"
  expand_templates
  ;;
compile)
  echo "==> compile phase script"
  ;;
test)
  echo "==> test phase script"
  ;;
package)
  echo "==> package phase script"
  ;;
install)
  echo "==> install phase script"
  ;;
deploy)
  echo "==> deploy phase script"
  # below segments are example of how to deploy various artifacts
  # copy the ones suitable for your repo, and remove the "if false" statement

  # build docker image from Docker file (under root of repo) and push to registry
  if  false ; then
    build_and_push_docker
  fi

  # upload all yaml file under the root of repo
  if  false ; then
    upload_files_of_extension yaml
  fi

  if  false ; then
    case $MVN_PROJECT_MODULEID in
    bootstrap)
      # build docker image from Docker file (under module dir) and push to registry
      build_and_push_docker
      ;;
    scripts)
      # upload all sh file under the root of module 
      upload_files_of_extension sh
      ;;
    platformdoc)
      CURDIR=$(pwd)
      virtualenv ./venv-doc
      source ./venv-doc/bin/activate
      pip install --upgrade pip
      pip install --upgrade mkdocs mkdocs-material
      pip freeze

      mkdocs build
      build_and_push_docker
      deactivate
      rm -rf ./venv-doc

      # build docker image from Docker file (under module dir) and push to registry
      build_and_push_docker
    ;;
    *)
      echo "====> unknown mvn project module"
      ;;
    esac
  fi
  ;;
*)
  echo "==> unprocessed phase"
  ;;
esac


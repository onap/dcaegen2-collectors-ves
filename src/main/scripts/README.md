## What this is directory for
This directory contains .sh scripts that are used to start & stop & configure the VESCollector application
inside the docker image.
These scripts are packaged inside the docker by a mvn assembly & docker plugins.

## How the application starts:
General flow goes like this
- Docker image is build, and it points docker-entry.sh as the entrypoing.
- Docker-entry point, depending on the deployment type, 
configures a bunch of things and starts the application in a separate process
and loops indefinitely to hold the docker container process.

## Environment variables
Most of the configuration of how VESCollector should be started and managed is done through environment variables.
Some of them are set during the image build process and some of them are defined manually or by
a particular deployment system.

Variables set manually / coming from deployment system:
- COLLECTOR_IP
- DMAAPHOST - set outside the code of VESCollector, should contain an address to DMaaP, so that event publishing can work
- CBSPOLLTIMER - it should be put in here, if we want to automatically fetch configuration from CBS.
- CONSUL_HOST - used with conjunction with CBSPOLLTIMER, should be a host address (without port! e.g http://my-ip-or-host) where Consul service lies
- CONFIG_BINDING_SERVICE - should be a name of CBS as it is registered in Consul
- HOSTNAME - should be a name of VESCollector application as it is registered in CBS catalog

## Development scripts
Inside the scripts/devScripts we have a place for all utility scripts that are useful during the development
of VESCollector. These scripts should not be put into an image but can be saved into repository for a developers convenience.

## Docker file system layout
The main directory where all code resides in docker container, denoted by $APP_BASE_PATH
looks like this and is located in /opt/app/VESCollector
```
<host>:/opt/app/VESCollector# ls
Dockerfile  bin  etc  lib  logs  specs	tomcat.8080
```
- bin contains sh scripts (path here is denoted by env var $SCRIPTS_PATH)
- etc contain various application configuration, most notably it reflects 'etc' directory from repository
- lib contains all libraries that are pulled into the app during maven build
- logs contain all application logs, especially collector.log file which is a main log file denoted by $MAIN_LOG_FILE variable
- spec contain json schemas specs for ves-collector
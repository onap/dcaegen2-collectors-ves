DCAE VESCollector
======================================

This is the repository for VES Collector for Open DCAE.

Virtual Event Streaming (VES) Collector is RESTful collector for processing JSON messages into DCAE. The collector verifies the source (when authentication is enabled) and validates the events against VES schema before distributing to DMAAP MR topics for downstream system to subscribe. The VESCollector also provides configurable event transformation function and event distribution to DMAAP MR topics.

The collector supports individual events or eventbatch posted to collector end-point(s) and post them to interface/bus for other application to subscribe.


### Build Instructions

This project is organized as a mvn project and has "org.onap.dcaegen2" as parent project. The build generate a jar and package into docker container. 

```
git clone ssh://vv770d@gerrit.onap.org:29418/dcaegen2/collectors/ves
mvn clean install
```

### Running Locally

Build the image (it will go into your local docker repository)
```
mvn clean package
```

Run the image using docker-compose.yml
```
docker-compose up
```

### Environment variables in Docker Container
Most of the configuration of how VESCollector should be started and managed is done through environment variables.
Some of them are set during the image build process and some of them are defined manually or by
a particular deployment system.

Variables set manually / coming from deployment system:
- COLLECTOR_IP
- DMAAPHOST - should contain an address to DMaaP, so that event publishing can work
- CBSPOLLTIMER - it should be put in here if we want to automatically fetch configuration from CBS.
- CONSUL_PROTOCOL - Consul protocol by default set to **http**, if it is need to change it then that can be set to different value 
- CONSUL_HOST - used with conjunction with CBSPOLLTIMER, should be a host address (without port! e.g my-ip-or-host) where Consul service lies
- CBS_PROTOCOL - Config Binding Service protocol by default set to **http**, if it is need to change it then that can be set to different value
- CONFIG_BINDING_SERVICE - used with conjunction with CBSPOLLTIMER, should be a name of CBS as it is registered in Consul
- HOSTNAME - used with conjunction with CBSPOLLTIMER, should be a name of VESCollector application as it is registered in CBS catalog

### Docker file system layout
The main directory where all code resides in docker container
looks like this and is located in /opt/app/VESCollector
```
<host>:/opt/app/VESCollector# ls
Dockerfile  bin  etc  lib  logs  specs	tomcat.8080
```
- bin contains sh scripts (path here is denoted by env var $SCRIPTS_PATH)
- etc contains various application configuration, most notably it reflects 'etc' directory from repository
- lib contains all libraries that are pulled into the app during maven build
- logs contains all application logs, especially collector.log file which is a main log file denoted by $MAIN_LOG_FILE variable
- specs contains json schemas specs for ves-collector

## Managing application in Docker container
Scripts directory contain .sh scripts that are used to start & stop & configure the VESCollector application
inside the docker image.
These scripts are packaged inside the docker image by a mvn assembly & docker plugins.

## How the application starts inside container
General flow goes like this
- Docker image is build, and it points docker-entry.sh as the entrypoint.
- Docker-entry point, depending on the deployment type,
configures a bunch of things and starts the application in a separate process
and loops indefinitely to hold the docker container process.

### Release images
For R1 - image/version  pushed to nexus3 
```
nexus3.onap.org:10003/snapshots/onap/org.onap.dcaegen2.collectors.ves.vescollector   1.1 
```

### Deployment

VESCollector in DCAE will be deployed as mS via DCAEGEN2 controller. A blueprint will be generated (CLAMP/SDC) which will fetch the docker image and install on the dockerhost identified. VESCollector on startup will query the configbindingService for updated configuration and starts the service. When configuration change is detected by DCAEGEN2 controller (via policy flow) - then contoller will notify Collector to fetch new configuration again. 

For testing purpose, the docker image includes preset configuration which can be ran without DCAEGEN2 platform.


### Testing

For R1 as only measurement and faults are expected in ONAP, configuration are preset currently sto support these two topics only.

```
STEPS FOR SETUP/TEST
1)	Get the VESCollector image from Nexus
		docker pull nexus.onap.org:10001/onap/org.onap.dcaegen2.collectors.ves.vescollector:1.1
2)	Start the container (change the DMAAPHOST environment value to running DMAAP instance host)
		docker run -d -p 8080:8080/tcp -p 8443:8443/tcp -P -e DMAAPHOST='10.0.0.174' nexus.onap.org:10001/onap/org.onap.dcaegen2.collectors.ves.vescollector:1.1
3)	Login into container and tail /opt/app/VESCollector/logs/collector.log
4)	Simulate event into VEScollector (can be done from different vm or same)
		curl -i -X POST -d @measurement.txt --header "Content-Type: application/json" https://localhost:8443/eventListener/v5 -k
		or curl -i  -X POST -d @measurement.txt --header "Content-Type: application/json" http://localhost:8080/eventListener/v5 -k
        Note: If DMAAPHOST provided is invalid, you will see exception around publish on the collector.logs (collector queues and attempts to resend the event hence exceptions reported will be periodic).   If you don’t want to see the error, publish to dmaap can be disabled by changing either “collector.dmaap.streamid” on etc/collector.properties OR by modifying the “name” defined on  etc/DmaapConfig.json. 

	Any changes to property within container requires collector restart
	cd /opt/app/VESCollector/
	./bin/appController.sh stop
	./bin/appController.sh start 

5)	If DMAAP instance (and DMAAPHOST passed during VESCollector startup) and VES input is valid, then events will be pushed to below topics depending on the domain
	Fault :http://<dmaaphost>:3904/events/unauthenticated.SEC_FAULT_OUTPUT
	Measurement : http://<dmaaphost>:3904/events/unauthenticated.SEC_MEASUREMENT_OUTPUT
6)	When test is done – do ensure to remove the container (docker rm -f <containerid>) to avoid port conflict
```

Authentication is disabled on the container for R1; below are the steps for enabling HTTPS/authentication for VESCollector. 
```
1) Login to the container
2) Open /opt/app/VESCollector/etc/collector.properties and edit below properties
                a) Comment below property (with authentication enabled, standard http should be disabled)
	                collector.service.port=8080
                b) Enable basic-authentication 
	                header.authflag=1
     Note: The actual credentials is stored part of header.authlist parameter. This is list of userid,password (base64encoded) values. Default configuration has below set
                sample1,c2FtcGxlMQ==|vdnsagg,dmRuc2FnZw==, where password maps to same value as username.
3) Restart the collector
                cd /opt/app/VESCollector
                ./bin/appController.sh stop
                ./bin/appController.sh start                               
4) Exit from container and ensure tcp port on VM is not hanging on finwait – you can execute “netstat -an | grep 8443” . If under FIN_WAIT2, wait for server to release.
5) Simulate via curl (Note - username/pwd will be required)      
	Example of successfull POST:
		vv770d@osdcae-dev-16:~$ curl -i  -u 'sample1:sample1' -X POST -d @volte.txt --header "Content-Type: application/json" https://localhost:8443/eventListener/v5 -k
		HTTP/1.1 200 OK
		Server: Apache-Coyote/1.1
		X-Rathravane: ~ software is craft ~
		Content-Type: application/json;charset=ISO-8859-1
		Content-Length: 17
		Date: Thu, 21 Sep 2017 22:23:49 GMT
		Message Accepted

	Example of authentication failure:
		vv770d@osdcae-dev-16:~$ curl -i -X POST -d @volte.txt --header "Content-Type: application/json" https://localhost:8443/eventListener/v5 -k
		HTTP/1.1 401 Unauthorized
		Server: Apache-Coyote/1.1
		X-Rathravane: ~ software is craft ~
		Content-Type: application/json;charset=ISO-8859-1
		Content-Length: 96
		Date: Thu, 21 Sep 2017 22:20:43 GMT
		Connection: close
		{"requestError":{"GeneralException":{"MessagID":"\"POL2000\"","text":"\"Unauthorized user\""}}}

Note: In general support for HTTPS also require certificate/keystore be installed on target VM with FS mapped into the container for VESCollector to load. For demo and testing purpose - a self signed certificate is included within docker build. When deployed via DCAEGEN2 platform - these configuration will be overridden dynamically to map to required path/certificate name. This will be exercised post R1 though.
```

A client's certificate verification is disabled on the container by default; below are the steps for enabling mutual TLS authentication for VESCollector.
```
1) Login to the container
2) Open /opt/app/VESCollector/etc/collector.properties and edit below properties
                a) Comment below property (with authentication enabled, standard http should be disabled)
	                collector.service.port=8080
                b) Enable a client's certificate verification  
	                collector.service.secure.clientauth=1
3) Restart the collector
                cd /opt/app/VESCollector
                ./bin/appController.sh stop
                ./bin/appController.sh start
4) Exit from container and ensure tcp port on VM is not hanging on finwait – you can execute “netstat -an | grep 8443” . If under FIN_WAIT2, wait for server to release.
5) In order for VESCollector to accept a connection from a client, the client has to use TLS certificate signed by CA that is present in VESCollector truststore. If a default VESCollector truststore is used then a client's certificate may be generated using following steps:
                a) Generate a client's private key
                    openssl genrsa -out client.key 2048
                b) Create the signing
                    openssl req -new -key client.key -out client.csr
                c) Create the client's certificate (CA key password should be obtained from [VESCollectorRepository]/certs/password)
                    openssl x509 -req -in client.csr -CA [VESCollectorRepository]/certs/rootCA.crt -CAkey [VESCollectorRepository]/certs/rootCA.key -CAcreateserial -out client.crt -days 500 -sha256
6) Simulate via curl (assuming that the certificate was created via step 5)
    Example of successfull POST:
        curl -i -X POST -d @event.json --header "Content-Type: application/json" https://localhost:8443/eventListener/v7 -k --cert client.crt --key client.key
        HTTP/1.1 100 
        
        HTTP/1.1 202 
        Content-Type: application/json
        Content-Length: 8
        Date: Wed, 21 Nov 2018 11:37:58 GMT
        
    Example of authentication failure (without a client's certificate):
        curl -i -X POST -d @event.json --header "Content-Type: application/json" https://localhost:8443/eventListener/v7 -k
        curl: (35) error:14094412:SSL routines:ssl3_read_bytes:sslv3 alert bad certificate
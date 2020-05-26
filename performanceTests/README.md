DCAE VESCollector PerformanceTests Environment
==============================================

This section describes how to configure VES Performance Tests environment on the RKE node

### Prerequisites

First of all you have to change variable in file **ves/performanceTests/Makefile:**    
```
RKE_NODE_USER_AND_HOSTNAME = <RKE_USER>@<RKE_IP>
RKE_PRIVATE_KEY = <PEM_PRIVATE_KEY_FILE_PATH>
RKE_KUBECONFIG_FILE_PATH = <KUBECONFIG_FILE_PATH_ON_RKE>
```
Important:
Make sure you have entered the correct configuration path(**RKE_KUBECONFIG_FILE_PATH**),
because it is necessary for kubectl to work properly on RKE over ssh.

The VES image being tested must have the buildForPerfTests profile enabled
(how to do this is described below).

### Build VES Collector with buildForPerfTests profile enabled:
Download project VES collector (**If you didn't do it before**)
```
git clone "https://gerrit.onap.org/r/dcaegen2/collectors/ves" 
```
and build project with buildForPerfTests profile
```
mvn clean package -PbuildForPerfTests docker:build
```
Push docker image to docker repository for example JFrog Artifactory.

### Change VES Collector image on k8s

Go to RKE node and edit deployment:
```
kubectl edit deployment dep-dcae-ves-collector
```
change image :
```
image: <IMAGE_NAME_FROM_REPOSITORY>
imagePullPolicy: IfNotPresent
```
after saving changes VES Collector pod should restarted automatically


###Automatic configuration and run performance tests on RKE

In this step, the performance tests environment will be copied to your RKE node and Prometheus and Grafana will be deployed
```
make all
```
### Step by step configuration performance tests on RKE

###1. Copy performance tests environment to RKE
```
make copy-performanceTests
```
###2. Run performance tests environment on RKE
```
make run-performanceTests
```
###3. Clear performance tests environment on RKE
```
make clear-performanceTests
```
###4. Remove performance tests environment from RKE
```
make remove-performanceTests
```
DCAE VESCollector PerformanceTests Environment
==============================================

This section describes how to configure VES Performance Tests environment on the RKE node

### Prerequisites

First of all you have to change variable in file **ves/performanceTests/Makefile:**    
```
#Configuration for RKE
RKE_NODE_USER_AND_HOSTNAME = <RKE_USER>@<RKE_IP>
RKE_PRIVATE_KEY = <PEM_PRIVATE_KEY_FILE_PATH>
RKE_KUBECONFIG_FILE_PATH = <KUBECONFIG_FILE_PATH_ON_RKE>

#Configuration for JMeter
JMETER_VM_USER_AND_HOSTNAME = <RKE_USER>@<VM_IP>
JMETER_VM_PRIVATE_KEY = <PEM_PRIVATE_KEY_FILE_PATH>
```
Secondly change ip (**<WORKER_IP>**) in file **ves/performanceTests/testScenario/test_scenario.jmx:**
```
###Ves collector address
<stringProp name="HTTPSampler.domain"><WORKER_IP></stringProp>
<stringProp name="HTTPSampler.port">30417</stringProp>
<stringProp name="HTTPSampler.protocol">https</stringProp>

###Ves collector address
<elementProp name="" elementType="Authorization">
    <stringProp name="Authorization.url">https://<WORKER_IP>:30417/eventListener/v7</stringProp>

### Influxdb address
<elementProp name="influxdbUrl" elementType="Argument">
    <stringProp name="Argument.name">influxdbUrl</stringProp>
    <stringProp name="Argument.value">http://<WORKER_IP>:30002/write?db=jmeter</stringProp>
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

In this step, the performance tests environment will be copied to your RKE node and Prometheus, Grafana and Influxdb will be deployed
```
make all
```
###Run test scenario
```
make run-jmeter
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
###5. Copy JMeter to VM
```
make copy-jmeter
```
###6. Run JMeter test scenario on VM
```
make run-jmeter
```
###7. Remove JMeter from VM
```
make remove-jmeter
```
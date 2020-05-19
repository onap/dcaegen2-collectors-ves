DCAE VESCollector PerformanceTests
======================================

This section describes how to copy, run and clear performance tests from the local environment on RKE.

### Prerequisites

First of all you have to change variable in file ves/performanceTests/Makefile:    
```
RKE_NODE_USER_AND_HOSTNAME = ubuntu@10.183.36.205
RKE_PRIVATE_KEY = ~/.ssh/onap-5802.pem
RKE_KUBECONFIG_DIRECTORY = /home/ubuntu/.kube/config.onap
```
Important:
Make sure you have entered the correct configuration path(RKE_KUBECONFIG_DIRECTORY),
because it is necessary for proper connection of ssh with RKE.


The image being tested must have the buildForPerfTests profile enabled
```
<profile>
    <id>buildForPerfTests</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
```
###1. Copy performance tests to lab
```
make copy-performanceTests
```
###2. Run performance tests on lab
```
make run-performanceTests
```
###3. Clear performance tests on lab
```
make clear-performanceTests
```
###4. Remove performance tests from lab
```
make remove-performanceTests
```
###5 Copy and run performance tests on lab(steps 1-2)
```
make all
```
### Manually running and clearing performance tests on RKE
```
Read file : ves/performanceTests/k8s/README.md
```
DCAE VESCollector PerformanceTests
==================================

This directory contains all files needed for setting up VES Collector performance tests environment and performance tests execution.
JMeter was selected as load testing tool.

Following sections contain:
* brief architecture description
* performance tests environment setup procedure
* performance tests execution procedure

# Architecture
Architecture diagram:
https://wiki.onap.org/display/DW/VES+Collector+Performance+Test#VESCollectorPerformanceTest-Architecture

The architecture consists of three parts:  
* VM - which contains: 
    * JMeter - executes performance test scenarios
    * Collectd - collects CPU and RAM metrics from the VM
* K8s with ONAP installed -  which contains:
    * NodeExporter - collects metrics from K8s cluster worker nodes
    * Prometheus - collects metrics from VES Collector and NodeExporter
    * InfluxDB - collects metrics from Collectd and tests results from JMeter  
    * Grafana - displays all metrics collected from Prometheus and Influxdb
* User environment - local environment with VES repository downloaded, from which:
    * test environment can be set up
    * test can be executed

# Usage
1. Prerequisites
- K8s environment with:
    - ONAP installed 
    - VES Collector with Maven profile `buildForPerfTests` enabled (See `How to setup VES for performance tests` section)
- VM with minimum 16GB RAM and 4 cores

2. Setup
- Edit `enrivonment.config` file to match your environment:
    - RKE_NODE_USER_AND_HOSTNAME - user and hostname for ssh connection to RKE node
    - RKE_PRIVATE_KEY - private key for ssh connection to RKE node
    - WORKER_IP - IP address to any of K8s worker nodes
    - JMETER_VM_USER_AND_HOSTNAME - user and hostname for ssh connection to VM
    - JMETER_VM_PRIVATE_KEY - private key for ssh connection to VM
    - TEST_SCENARIO_FILE - name of test scenario file to be executed. Available test scenarios are located in `performanceTests/environment/jmeterVM/jmeter`
- Install performance tests environment:
    - `make all` - copies all files to K8s and VM, installs all components and prints links to Grafana and Prometheus GUI

3. Performance test execution
    - `make execute-test` - triggers JMeter on VM to execute performance test scenario defined in `enrivonment.config`
    
4. Performance test results and metrics
    Open up Grafana in browser - link to Grafana is printed at the end of `make all` command output
    
5. Other useful commands:
    - `make clear` - uninstalls and removes everything related to performance tests from K8s and VM
    - `make restart` - recreates performance tests environment from scratch by invoking `make clear` and `make all`


# How to setup VES for performance tests 
The VES image being tested must have the buildForPerfTests profile enabled.

1. Build VES Collector with buildForPerfTests profile enabled:
    - download project VES collector (**If you didn't do it before**)
    ```
    git clone "https://gerrit.onap.org/r/dcaegen2/collectors/ves" 
    ```
    - build project with buildForPerfTests profile
    ```
    mvn clean package -PbuildForPerfTests docker:build
    ```
    - push docker image to docker repository for example JFrog Artifactory.

2. Change VES Collector image on k8s
    - go to RKE node and edit deployment:
    ```
    kubectl edit deployment dep-dcae-ves-collector
    ```
    - change image :
    ```
    image: <IMAGE_NAME_FROM_REPOSITORY>
    imagePullPolicy: IfNotPresent
    ```
    - after saving changes VES Collector pod should restarted automatically
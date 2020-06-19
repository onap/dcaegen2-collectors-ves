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
    * DMaaP Simulator - collects events from VES Collector and calculates VES processing time (Client -> VES -> DMaaP) 
* User environment - local environment with VES repository downloaded, from which:
    * test environment can be set up
    * test can be executed

# Usage
1. Prerequisites
    - K8s environment with:
        - ONAP installed 
        - VES Collector with Maven profile `buildForPerfTests` enabled (See `How to set up VES for performance tests` section)
        - DMaaP Simulator docker image available (See `How to set up DMaaP Simulator for performance tests` section)
    - VM with minimum 30GB RAM, 8 cores, 35000 pid_max, 70000 max_map_count (How to setup pid_max and max_map_count https://stackoverflow.com/questions/34452302/how-to-increase-maximum-number-of-jvm-threads-linux-64bit)
    - K8s environment and VM should have synchronized clocks - set the same NTP Server on all K8s worker nodes and VM

2. Setup
    - Edit `enrivonment.config` file to match your environment:
        - RKE_NODE_USER_AND_HOSTNAME - user and hostname for ssh connection to RKE node
        - RKE_PRIVATE_KEY - private key for ssh connection to RKE node
        - WORKER_IP - IP address to any of K8s worker nodes
        - JMETER_VM_USER_AND_HOSTNAME - user and hostname for ssh connection to VM
        - JMETER_VM_PRIVATE_KEY - private key for ssh connection to VM 
        - DMAAP_SIMULATOR_IMAGE - DMaaP Simulator image available in K8s environment (See `How to set up DMaaP Simulator for performance tests` section)
        - TEST_SCENARIO_FILE - name of test scenario file to be executed. Available test scenarios are located in `performanceTests/environment/jmeterVM/jmeter/testScenarios`
    - Install performance tests environment:
        - `make all` - copies all files to K8s and VM, installs all components and prints links to Grafana and Prometheus GUI
    - Change DMaaP address for Fault Events in VES to DMaaP Simulator  
        - See `How to change DMaaP address for Fault Events in VES`

3. Performance test execution
    - `make execute-test` - triggers JMeter on VM to execute performance test scenario defined in `enrivonment.config`
    
4. Performance test results and metrics
    Open up Grafana in browser - link to Grafana is printed at the end of `make all` command output
    
5. Other useful commands:
    - `make clear` - uninstalls and removes everything related to performance tests from K8s and VM
    - `make restart` - recreates performance tests environment from scratch by invoking `make clear` and `make all`


# How to set up VES for performance tests 
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
    
    
# How to set up DMaaP Simulator for performance tests 
Currently, DMaaP Simulator is not contributed to ONAP, but its image is available on ONAP wiki.
It's not available in any public docker repository, thus you need to make it available in your environment. 

How to do so:
- Download compressed image from 
https://wiki.onap.org/display/DW/VES+Collector+Performance+Test?linked=true#VESCollectorPerformanceTest-DMaaPSimulator
- Extract image `docker load < ves-dmaa-simulator-image.tar`
- Now it's ready to push it to your docker repository for example JFrog Artifactory


# How to change DMaaP address for Fault Events in VES
Currently, we use only Fault Events in performance tests.

- Open VES configuration in Consul GUI in web browser http://<Worker_IP>:30270/ui/#/dc1/kv/dcae-ves-collector/edit 
- Edit configuration `streams_publishes.ves-fault.dmaap_info.topic_url`, change IP and PORT to `ves-dmaap-simulator:3904`, as below:
```
    "ves-fault": {
      "type": "message_router",
      "dmaap_info": {
        "topic_url": "http://ves-dmaap-simulator:3904/events/unauthenticated.SEC_FAULT_OUTPUT/"
      }
    }
```
- Click `Update` button
- Restart VES pod 


all: copy-performanceTests run-performanceTests copy-jmeter copy-collectd install-collectd


RKE_NODE_USER_AND_HOSTNAME = ubuntu@10.183.36.205
RKE_PRIVATE_KEY = ~/.ssh/onap-5802.pem
PERFORMANCE_TESTS_DIRECTORY = vesPerformanceTestsEnv
RKE_KUBECONFIG_FILE_PATH = /home/ubuntu/.kube/config.onap

#Configuration for jMeter
JMETER_VM_USER_AND_HOSTNAME = root@10.183.36.50
JMETER_VM_PRIVATE_KEY = ~/.ssh/onap-5802.pem

restart: clear-performanceTests remove-performanceTests remove-collectd all

copy-performanceTests:
	@echo "\n##### Copy performance tests directory to lab environment #####"
	scp -r -i $(RKE_PRIVATE_KEY) ./k8s $(RKE_NODE_USER_AND_HOSTNAME):$(PERFORMANCE_TESTS_DIRECTORY)
	@echo "##### DONE #####"

run-performanceTests:
	@echo "\n##### Run prometheus and grafana in lab environment #####"
	ssh -i $(RKE_PRIVATE_KEY) $(RKE_NODE_USER_AND_HOSTNAME) 'bash -c "export KUBECONFIG=$(RKE_KUBECONFIG_FILE_PATH) && cd $(PERFORMANCE_TESTS_DIRECTORY) && make all"'
	@echo "##### DONE #####"

clear-performanceTests:
	@echo "\n##### Stop and clear prometheus and grafana in lab environment #####"
	ssh -i $(RKE_PRIVATE_KEY) $(RKE_NODE_USER_AND_HOSTNAME) 'bash -c "export KUBECONFIG=$(RKE_KUBECONFIG_FILE_PATH) && cd $(PERFORMANCE_TESTS_DIRECTORY) && make clear"'
	@echo "##### DONE #####"

remove-performanceTests:
	@echo "\n##### Remove performance tests  #####"
	ssh -i $(RKE_PRIVATE_KEY) $(RKE_NODE_USER_AND_HOSTNAME) 'bash -c "export KUBECONFIG=$(RKE_KUBECONFIG_FILE_PATH) && rm -rf $(PERFORMANCE_TESTS_DIRECTORY)"'
	@echo "##### DONE #####"

copy-collectd:
	@echo "\n##### Copy collectd to JMeter VM #####"
	scp -r -i $(JMETER_VM_PRIVATE_KEY) ./k8s/collectd $(JMETER_VM_USER_AND_HOSTNAME):collectd
	@echo "##### DONE #####"

install-collectd:
	@echo "\n##### Install collectd on JMeter VM #####"
	ssh -i $(JMETER_VM_PRIVATE_KEY) $(JMETER_VM_USER_AND_HOSTNAME) 'cd collectd && make'
	@echo "##### DONE #####"

remove-collectd:
	@echo "\n##### Remove collectd  #####"
	ssh -i $(JMETER_VM_PRIVATE_KEY) $(JMETER_VM_USER_AND_HOSTNAME) 'rm -rf collectd'
	@echo "##### DONE #####"

copy-jmeter:
	@echo "\n##### Copy JMeter #####"
	scp -r -i $(JMETER_VM_PRIVATE_KEY) ./k8s/testScenario $(JMETER_VM_USER_AND_HOSTNAME):$(PERFORMANCE_TESTS_DIRECTORY)
	@echo "##### DONE #####"

run-jmeter:
	@echo "\n##### Run test scenario  #####"
	ssh -i $(JMETER_VM_PRIVATE_KEY) $(JMETER_VM_USER_AND_HOSTNAME) '/bin/bash ./$(PERFORMANCE_TESTS_DIRECTORY)/run_jmeter.sh'
	@echo "##### DONE #####"

remove-jmeter:
	@echo "\n##### Remove JMeter  #####"
	ssh -i $(JMETER_VM_PRIVATE_KEY) $(JMETER_VM_USER_AND_HOSTNAME) 'rm -rf $(PERFORMANCE_TESTS_DIRECTORY)'
	@echo "##### DONE #####"
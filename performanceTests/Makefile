include environment.config

# Additional configuration
PERFORMANCE_TESTS_ENV_DIRECTORY = vesPerformanceTestsEnv
RKE_KUBECONFIG_FILE_PATH = ~/.kube/config.onap

# Targets
all: --copy-k8s-performance-tests-env --copy-jmeter-vm-env --install-k8s-performance-tests-env --install-jmeter-vm-env
clear: --uninstall-k8s-performance-tests-env --remove-k8s-performance-tests-env --remove-jmeter-vm-env
restart: clear all

# K8s environment
--copy-k8s-performance-tests-env:
	@echo "\n##### Copy performance tests environment directory to k8s environment #####"
	scp -r -i $(RKE_PRIVATE_KEY) ./environment/k8s $(RKE_NODE_USER_AND_HOSTNAME):$(PERFORMANCE_TESTS_ENV_DIRECTORY)
	@echo "##### DONE #####"

--install-k8s-performance-tests-env:
	@echo "\n##### Deploy components(grafana, influxdb, prometheus, node exporter) on k8s #####"
	ssh -i $(RKE_PRIVATE_KEY) $(RKE_NODE_USER_AND_HOSTNAME) 'bash -c "export KUBECONFIG=$(RKE_KUBECONFIG_FILE_PATH) && cd $(PERFORMANCE_TESTS_ENV_DIRECTORY) && make all WORKER_IP=$(WORKER_IP)"'
	@echo "##### DONE #####"

--uninstall-k8s-performance-tests-env:
	@echo "\n##### Undeploy components(grafana, influxdb, prometheus, node exporter) from k8s #####"
	ssh -i $(RKE_PRIVATE_KEY) $(RKE_NODE_USER_AND_HOSTNAME) 'bash -c "export KUBECONFIG=$(RKE_KUBECONFIG_FILE_PATH) && cd $(PERFORMANCE_TESTS_ENV_DIRECTORY) && make clear"'
	@echo "##### DONE #####"

--remove-k8s-performance-tests-env:
	@echo "\n##### Remove performance tests environment directory from k8s environment  #####"
	ssh -i $(RKE_PRIVATE_KEY) $(RKE_NODE_USER_AND_HOSTNAME) 'bash -c "export KUBECONFIG=$(RKE_KUBECONFIG_FILE_PATH) && rm -rf $(PERFORMANCE_TESTS_ENV_DIRECTORY)"'
	@echo "##### DONE #####"

# JMeter VM environment
--copy-jmeter-vm-env:
	@echo "\n##### Copy JMeter environment directory to VM #####"
	scp -r -i $(JMETER_VM_PRIVATE_KEY) ./environment/jmeterVM $(JMETER_VM_USER_AND_HOSTNAME):$(PERFORMANCE_TESTS_ENV_DIRECTORY)
	@echo "##### DONE #####"

--install-jmeter-vm-env:
	@echo "\n##### Install collectd on VM #####"
	ssh -i $(JMETER_VM_PRIVATE_KEY) $(JMETER_VM_USER_AND_HOSTNAME) 'sed -i 's/WORKER_IP/$(WORKER_IP)/g' $(PERFORMANCE_TESTS_ENV_DIRECTORY)/jmeter/test_scenario.jmx'
	ssh -i $(JMETER_VM_PRIVATE_KEY) $(JMETER_VM_USER_AND_HOSTNAME) 'sed -i 's/WORKER_IP/$(WORKER_IP)/g' $(PERFORMANCE_TESTS_ENV_DIRECTORY)/collectd/collectd.conf'
	ssh -i $(JMETER_VM_PRIVATE_KEY) $(JMETER_VM_USER_AND_HOSTNAME) 'bash -c $(PERFORMANCE_TESTS_ENV_DIRECTORY)/collectd/install_collectd.sh'
	@echo "##### DONE #####"

--remove-jmeter-vm-env:
	@echo "\n##### Remove JMeter environment directory from VM #####"
	ssh -i $(JMETER_VM_PRIVATE_KEY) $(JMETER_VM_USER_AND_HOSTNAME) 'rm -rf $(PERFORMANCE_TESTS_ENV_DIRECTORY)'
	@echo "##### DONE #####"

# Execute test scenario
execute-test:
	@echo "\n##### Run test scenario #####"
	ssh -i $(JMETER_VM_PRIVATE_KEY) $(JMETER_VM_USER_AND_HOSTNAME) '/bin/bash $(PERFORMANCE_TESTS_ENV_DIRECTORY)/jmeter/run_jmeter.sh $(PERFORMANCE_TESTS_ENV_DIRECTORY) $(TEST_SCENARIO_FILE)'
	@echo "##### DONE #####"

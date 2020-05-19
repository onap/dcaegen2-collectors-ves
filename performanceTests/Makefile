all: copy-performanceTests run-performanceTests

RKE_NODE_USER_AND_HOSTNAME = ubuntu@10.183.36.205
RKE_PRIVATE_KEY = ~/.ssh/onap-5802.pem
PERFORMANCE_TESTS_DIRECTORY = vesPerformanceTestsEnv
RKE_KUBECONFIG_DIRECTORY = /home/ubuntu/.kube/config.onap

copy-performanceTests:
	@echo "\n##### Copy performance tests directory to lab environment #####"
	scp -r -i $(RKE_PRIVATE_KEY) ./k8s $(RKE_NODE_USER_AND_HOSTNAME):$(PERFORMANCE_TESTS_DIRECTORY)
	@echo "##### DONE #####"

run-performanceTests:
	@echo "\n##### Run prometheus and grafana in lab environment #####"
	ssh -i $(RKE_PRIVATE_KEY) $(RKE_NODE_USER_AND_HOSTNAME) 'bash -c "export KUBECONFIG=$(RKE_KUBECONFIG_DIRECTORY) && cd $(PERFORMANCE_TESTS_DIRECTORY) && make all"'
	@echo "##### DONE #####"

clear-performanceTests:
	@echo "\n##### Stop and clear prometheus and grafana in lab environment #####"
	ssh -i $(RKE_PRIVATE_KEY) $(RKE_NODE_USER_AND_HOSTNAME) 'bash -c "export KUBECONFIG=$(RKE_KUBECONFIG_DIRECTORY) && cd $(PERFORMANCE_TESTS_DIRECTORY) && make clear"'
	@echo "##### DONE #####"

remove-performanceTests:
	@echo "\n##### Remove performance tests  #####"
	ssh -i $(RKE_PRIVATE_KEY) $(RKE_NODE_USER_AND_HOSTNAME) 'bash -c "export KUBECONFIG=$(RKE_KUBECONFIG_DIRECTORY) && rm -rf $(PERFORMANCE_TESTS_DIRECTORY)"'
	@echo "##### DONE #####"
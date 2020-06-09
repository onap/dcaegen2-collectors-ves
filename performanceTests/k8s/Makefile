all: create-configmaps deploy-influxdb deploy-prometheus deploy-grafana display-urls

# Prometheus configuration
PROMETHEUS_DIRECTORY = ./prometheus
PROMETHEUS_DEPLOYMENT = deployment.yaml
PROMETHEUS_CONFIGMAP = configmap.yaml

# Grafana configuration
GRAFANA_DIRECTORY = ./grafana
GRAFANA_DEPLOYMENT = deployment.yaml
DATASOURCE_CONFIGMAP = datasource.yaml
DASHBOARD_PROVIDER_CONFIGMAP  = dashboards-provider.yaml
DASHBOARD_CONFIGMAP = dashboard.yaml

#INFLUXDB configuration
INFLUXDB_DIRECTORY = ./influxdb
INFLUXDB_DEPLOYMENT = deployment.yaml

clear:
	@echo "\n##### Delete configmaps and $(GRAFANA_DEPLOYMENT)(grafana, prometheus)#####"
	kubectl delete -f $(GRAFANA_DIRECTORY)/$(GRAFANA_DEPLOYMENT) || true
	kubectl delete -f $(GRAFANA_DIRECTORY)/$(DASHBOARD_PROVIDER_CONFIGMAP) || true
	kubectl delete -f $(GRAFANA_DIRECTORY)/$(DATASOURCE_CONFIGMAP) || true
	kubectl delete -n onap configmap ves-grafana-dashboards || true
	kubectl delete -f $(PROMETHEUS_DIRECTORY)/$(PROMETHEUS_DEPLOYMENT) || true
	kubectl delete -f $(PROMETHEUS_DIRECTORY)/$(PROMETHEUS_CONFIGMAP) || true
	kubectl delete -f $(INFLUXDB_DIRECTORY)/$(INFLUXDB_DEPLOYMENT) || true
	@echo "##### DONE #####"

create-configmaps:
	@echo "\n##### Create configmaps #####"
	kubectl apply -f $(PROMETHEUS_DIRECTORY)/$(PROMETHEUS_CONFIGMAP)
	kubectl apply -f $(GRAFANA_DIRECTORY)/$(DATASOURCE_CONFIGMAP)
	kubectl apply -f $(GRAFANA_DIRECTORY)/$(DASHBOARD_PROVIDER_CONFIGMAP)
	kubectl create configmap ves-grafana-dashboards -n onap --from-file grafana/dashboards/
	@echo "##### DONE #####"

deploy-grafana:
	@echo "\n##### Deploy grafana #####"
	kubectl apply -f $(GRAFANA_DIRECTORY)/$(GRAFANA_DEPLOYMENT)
	@echo "##### DONE #####"

deploy-prometheus:
	@echo "\n##### Deploy prometheus #####"
	kubectl apply -f $(PROMETHEUS_DIRECTORY)/$(PROMETHEUS_DEPLOYMENT)
	@echo "##### DONE #####"

deploy-influxdb:
	@echo "\n##### Deploy influxdb #####"
	kubectl apply -f $(INFLUXDB_DIRECTORY)/$(INFLUXDB_DEPLOYMENT)
	@echo "##### DONE #####"

display-urls:
	@echo "\e[32m##### Prometheus : http://<WORKER_IP>:30069/ #####\e[39m"
	@echo "\e[32m##### Grafana http://<WORKER_IP>:30001/ #####\e[39m"

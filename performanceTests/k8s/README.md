DCAE VESCollector Performance Tests environment
===============================================

### Prerequisites
Copy performance tests environment to RKE node (**If you didn't do it before**)
```
See step: "1. Copy performance tests environment to RKE" in ves/performanceTests/README.md 
```
###Automatic Prometheus and Grafana configuration at the RKE
```
make all
```
### Step by step ruinning performance tests at the RKE

###1. Clear environment(delete configmaps and deployment for Prometheus and Grafana)
```
make clear
```
###2. Create configmaps for Prometheus and Grafana
```
make create-configmaps
```
###3. Deploy grafana
```
make deploy-grafana
```
###4. Deploy prometheus
```
make deploy-prometheus
```
###5. Deploy influxdb
```
make deploy-influxdb
```
###6. Display URL of the graphical user interface Prometheus and Grafana
```
make display-urls
```
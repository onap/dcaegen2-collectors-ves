DCAE VESCollector Performance Tests environment
===============================================

This section describes how to prepare VES Performance Test environment at the RKE 

### Prerequisites
Copy performance tests environment to RKE node (**If you didn't do it before**)
```
Read: ves/performanceTests/README.md
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
###5. Display URL of the graphical user interface Prometheus and Grafana
```
make display-urls
```
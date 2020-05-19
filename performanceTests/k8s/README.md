DCAE VESCollector PerformanceTests
======================================

This section shows how to use ves performance tests from the RKE level 

### Prerequisites
Copy performance tests to lab (If you didn't do it before)
```
Read: ves/performanceTests/README.md
```
###1. Clear environment(delete configmaps and deployment for Prometheus and Grafana)
```
make clear
```
###2. Create configmaps for Prometheus and Grafana
```
make create-confimaps
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
###6. Create configmaps and deployment Prometheus and Grafana(steps 2-5)
```
make all
```
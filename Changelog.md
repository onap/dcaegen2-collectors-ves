# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [1.12.3] - 2023/02/14
         - [DCAEGEN2-3345] - Test updates for ConfigProcessor, DMaaPConfigurationParser & DMaaPEventPublisher

## [1.12.2] - 2023/01/20
         - [DCAEGEN2-3334] - Update tests execution to be platform agnostic
         - [DCAEGEN2-3345] - Add additional tests for ConfigProcessor and ApiAuthInterceptor

## [1.12.1] - 2022/12/05
         - [DCAEGEN2-3257] - Align DCAE components with the new logging GR.

## [1.12.0] - 2022/10/10
         - [DCAEGEN2-3295] - SDK upgrade to 1.9.0 (CBSclient changed to remove CBS/Consul parameters) 

## [1.11.1] - 2022/01/28
         - [DCAEGEN2-3214] - Dcaegen2-collectors-ves vulnerability updates

## [1.11.0] - 2022/01/28
         - [DCAEGEN2-2961] - Switch VESCollector to Integration base image(onap/integration-java11:10.0.0)
         - [DCAEGEN2-3045] - Vulnerability addressal for VESCollector

## [1.10.3] - 2022/01/18
         - [DCAEGEN2-3022] - Remediation for Log4Shell vulnerability (upgrade to 2.17.1)

## [1.10.2] - 2021/12/14
         - [DCAEGEN2-3022] - Remediation for Log4Shell vulnerability (upgrade to 2.16.0)

## [1.10.1] - 2021/08/31
         -  [DCAEGEN2-1483](https://jira.onap.org/browse/DCAEGEN2-2719) - CBS-Client supporting configMap
            - update CBS-Client from 1.8.0 to 1.8.7 in order to enable config file support
            - fix ambiguous spring-boot-maven-plugin import - set it to 2.4.3
            - fix ambiguous base docker image - set it to openjdk:11.0.11-jre-slim

## [1.10.0] - 2021/06/11
         -  [DCAEGEN2-1483](https://jira.onap.org/browse/DCAEGEN2-1483) - VESCollector Event ordering
            - remove cambria, add DmaaP client
            - sending event for many topics at once is no longer supported
            - add backward compatibility status codes
            - add additional validation for batchEvent            

## [1.9.2] - 2021/05/14
         -  [DCAEGEN2-2683](https://jira.onap.org/browse/DCAEGEN2-2683) - Enable Spring Prometheus metrics end-point in VES
            Temporary add mvn profile for enabling/disabling Prometheus metrics            

## [1.9.1] - 2021/03/22
         -  [DCAEGEN2-2683](https://jira.onap.org/browse/DCAEGEN2-2683) - Enable Spring Prometheus metrics end-point in VES
            Remove mvn profile for enable/disable Prometheus metrics

## [1.9.0] - 2021/03/18
         -  [DCAEGEN2-2682](https://jira.onap.org/browse/DCAEGEN2-2682) - Update libraries

## [1.8.0] - 2021/02/24
         -  [DCAEGEN2-2477](https://jira.onap.org/browse/DCAEGEN2-2477) - Update VESCollector CommonEventSchema to ONAP/Honolulu version            

## [1.7.11] - 2021/02/18
         -  [DCAEGEN2-2593](https://jira.onap.org/browse/DCAEGEN2-2593) - Vulnerability removal for ves collector
            Fix sonar reporting problem

## [1.7.10] - 2021/02/10
         -  [DCAEGEN2-2593](https://jira.onap.org/browse/DCAEGEN2-2593) - Vulnerability removal for ves collector

## [1.7.9] - 2020/11/01
         -  [DCAEGEN2-2495](https://jira.onap.org/browse/DCAEGEN2-2495) - Ves Collector is down because of java heap space

## [1.7.8] - 2020/10/13
          - [DCAEGEN2-2478](https://jira.onap.org/browse/DCAEGEN2-2478) - Add logs from external-repo-manager lib

## [1.7.7] - 2020/09/29
         - [DCAEGEN2-2462](https://jira.onap.org/browse/DCAEGEN2-2462) - Adapt schema-map.json and test files to updated 3GPP repos 

## [1.7.6] - 2020/09/18
        - [DCAEGEN-2374](https://jira.onap.org/browse/DCAEGEN2-2374) - Fix an error reported by DMaapEventPublisher test when pk is not available.
        - [DCAEGEN2-2453](https://jira.onap.org/browse/DCAEGEN2-2453) - Fix VES problem with subsequent fetching from CBS.

## [1.7.5] - 2020/09/09
        - [DCAEGEN2-2264](https://jira.onap.org/browse/DCAEGEN2-2264) - Update schema-map.json
        - [DCAEGEN2-2426](https://jira.onap.org/browse/DCAEGEN2-2426) - Fix bug throwing exception when first event is collected

## [1.7.4] - 2020/08/04
        - [DCAEGEN2-2212](https://jira.onap.org/browse/DCAEGEN2-2212) - Config fetch for VESCollector through DCAE-SDK (CBS Client)
        - [DCAEGEN2-2264](https://jira.onap.org/browse/DCAEGEN2-2264) - Post stndDefined implementation fixes  

## [1.7.3] - 2020/08/10
        - [DCAEGEN2-2264](https://jira.onap.org/browse/DCAEGEN2-2264) - Add implementation of stndDefined fields validation

## [1.7.2] - 2020/08/04
        - [DCAEGEN2-1771](https://jira.onap.org/browse/DCAEGEN2-1771) - Add StndDefined event routing to dmaap streams defined in namespace event field - no second stage event validation.
          Fix error response model
          Update DPO model

## [1.7.1] - 2020/07/13
        - [DCAEGEN2-1484](https://jira.onap.org/browse/DCAEGEN2-1484) - VESCollector DMaap publish optimization
        - [DCAEGEN2-2254](https://jira.onap.org/browse/DCAEGEN2-2254) - Add new data-format for 30.2_ONAP schema version

## [1.7.0] - 2020/07/09
        - [DCAEGEN2-2254](https://jira.onap.org/browse/DCAEGEN2-2254) - Update schema to CommonEventFormat_30.2_ONAP in the eventListerner/v7 interface

## [1.6.2] - 2020/06/01
        - [DCAEGEN2-2245](https://jira.onap.org/browse/DCAEGEN2-2245) - Code improvements 
          Increase code coverage:
           - HeaderUtil
           - EnvProps
           - WebMvcConfig 

## [1.6.1] - 2020/05/21
        - [DCAEGEN2-608](https://jira.onap.org/browse/DCAEGEN2-608) - Deployment Prometheus and Grafana on RKE for perf tests

## [1.6.0] - 2020/05/13
        - [DCAEGEN2-608](https://jira.onap.org/browse/DCAEGEN2-608) - Expose Prometheus API for performance tests

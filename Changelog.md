# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [1.6.0] - 13/05/2020
        - [DCAEGEN2-608](https://jira.onap.org/browse/DCAEGEN2-608) - Expose Prometheus API for performance tests
## [1.6.1] - 21/05/2020
        - [DCAEGEN2-608](https://jira.onap.org/browse/DCAEGEN2-608) - Deployment Prometheus and Grafana on RKE for perf tests
## [1.6.2] - 01/06/2020
        - [DCAEGEN2-2245](https://jira.onap.org/browse/DCAEGEN2-2245) - Code improvements 
          Increase code coverage:
           - HeaderUtil
           - EnvProps
           - WebMvcConfig 
## [1.7.0] - 09/07/2020
        - [DCAEGEN2-2254](https://jira.onap.org/browse/DCAEGEN2-2254) - Update schema to CommonEventFormat_30.2_ONAP in the eventListerner/v7 interface
## [1.7.1] - 13/07/2020
        - [DCAEGEN2-1484](https://jira.onap.org/browse/DCAEGEN2-1484) - VESCollector DMaap publish optimization
        - [DCAEGEN2-2254](https://jira.onap.org/browse/DCAEGEN2-2254) - Add new data-format for 30.2_ONAP schema version
## [1.7.2] - 04/08/2020
        - [DCAEGEN2-1771](https://jira.onap.org/browse/DCAEGEN2-1771) - Add StndDefined event routing to dmaap streams defined in namespace event field - no second stage event validation.
          Fix error response model
          Update DPO model
## [1.7.3] - 10/08/2020
        - [DCAEGEN2-2264](https://jira.onap.org/browse/DCAEGEN2-2264) - Add implementation of stndDefined fields validation
## [1.7.4] - 04/08/2020
        - [DCAEGEN2-2212](https://jira.onap.org/browse/DCAEGEN2-2212) - Config fetch for VESCollector through DCAE-SDK (CBS Client)
        - [DCAEGEN2-2264](https://jira.onap.org/browse/DCAEGEN2-2264) - Post stndDefined implementation fixes  
## [1.7.5] - 09/09/2020
        - [DCAEGEN2-2264](https://jira.onap.org/browse/DCAEGEN2-2264) - Update schema-map.json
        - [DCAEGEN2-2426](https://jira.onap.org/browse/DCAEGEN2-2426) - Fix bug throwing exception when first event is collected
## [1.7.6] - 18/09/2020
        - [DCAEGEN-2374](https://jira.onap.org/browse/DCAEGEN2-2374) - Fix an error reported by DMaapEventPublisher test when pk is not available.
        - [DCAEGEN2-2453](https://jira.onap.org/browse/DCAEGEN2-2453) - Fix VES problem with subsequent fetching from CBS.
## [1.7.7] - 29/09/2020
         - [DCAEGEN2-2462](https://jira.onap.org/browse/DCAEGEN2-2462) - Adapt schema-map.json and test files to updated 3GPP repos 
## [1.7.8] - 13/10/2020
          - [DCAEGEN2-2478](https://jira.onap.org/browse/DCAEGEN2-2478) - Add logs from external-repo-manager lib
## [1.7.9] - 01/11/2020
         -  [DCAEGEN2-2495](https://jira.onap.org/browse/DCAEGEN2-2495) - Ves Collector is down because of java heap space
## [1.7.10] - 10/02/2021
         -  [DCAEGEN2-2593](https://jira.onap.org/browse/DCAEGEN2-2593) - Vulnerability removal for ves collector
## [1.7.11] - 18/02/2021
         -  [DCAEGEN2-2593](https://jira.onap.org/browse/DCAEGEN2-2593) - Vulnerability removal for ves collector
            Fix sonar reporting problem
## [1.8.0] - 24/02/2021
         -  [DCAEGEN2-2477](https://jira.onap.org/browse/DCAEGEN2-2477) - Update VESCollector CommonEventSchema to ONAP/Honolulu version
            Use updated CommonEventSchema to validate IP in VES Collector             
            

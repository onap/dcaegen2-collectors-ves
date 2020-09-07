StndDefined schemas ConfigMap generator
=======================================

## Description
StndDefined schemas Kubernetes ConfigMap generator is a Makefile with two targets running bash scripts: 'generate' and 
'install'. This Makefile may be used by stndDefined schemas vendors to generate and install ConfigMap containing schemas 
and mapping file for stndDefined validation in VES Collector pod. Process of generation of ConfigMap spec file is 
configurable via environment.config file.

## Requirements and limitations

### Operating system
Script works and has been tested on Ubuntu 18.04 and Ubuntu 19.10 which covers actual ONAP OS environment.

### Environment
Target *generate* from Makefile require stable internet connection to properly clone Git repositories.
Despite the fact that this generator is located in the VES Collector repository, target *install* should be ran on the
RKE node, so it can create ConfigMap in the same Kubernetes environment as VES Pod is installed. 
It is possible to generate spec in one environment and move it together with all ConfigMap generator tool files to RKE
environment. 

### Repository limitations
When running the script whole selected repository is being downloaded. Time of script execution depends mostly on 
repository size and number of schemas. All YAML files from selected directory in repository will be considered as 
schemas and attached to ConfigMap spec.

### Generator tool files integration
It is recommended to consider files of this tool as unity and not split them during moving generator through 
environments e.g. from VES pod to RKE node if needed. Generator tool files that are required are:
- Makefile
- install.sh
- generate.sh
- environment.config

## Instruction

### Parameters description
Before running any target from Makefile, configuration in *environment.config* must be properly prepared. Description of
the configurable properties is below.

- **SPEC_CONFIGMAP_FILENAME** - Filename name of ConfigMap spec that will be generated.
- **K8S_CONFIGMAP_NAME** - Kubernetes name of ConfigMap that will be generated and installed.
- **MAPPING_FILE_NAME** - Schema mapping file name generated in the spec.
  
- **REPOSITORY_URL_HTTPS** - URL to remote Git repository which lets cloning repository via HTTPS.
- **REPOSITORY_BRANCH** - Valid branch from selected Git repository which contains schemas desired to mount. Script 
accepts an array of branches from which schemas will be collected. To pass an array split branch names with space and 
cover list in quotation marks.
- **SCHEMAS_LOCATION** - Path to schemas directory on selected Git repo. All YAML files from this repository will be 
considered as schema and added to ConfigMap.

- **VENDOR_NAME** - Name of organisation delivering schemas, used only for schemas destination directory in VES.

### Running commands

To run ConfigMap spec generation for set configuration use:

**NOTE**: Remember about environment requirement of stable internet connection.  

```
make generate
```

To run ConfigMap installation in Kubernetes use:

**NOTE**: Remember about running this command on RKE node.

```
make install
```

**NOTE**: It is possible that ConfigMap with selected K8S_CONFIGMAP_NAME already exists in Kubernetes. In such situation
either regenerate spec with new K8S_CONFIGMAP_NAME or remove existing ConfigMap from Kubernetes and install spec again.
To remove ConfigMap from Kubernetes use:
```
kubectl -n onap delete configmap <CONFIGMAP_NAME>
``` 

## ConfigMap validation
After running the script ConfigMap spec file is generated in current working directory.
Spec file can be manually validated via any text editor. The last file included in spec is schema-map.json file with
mappings of external URLs to prepared local URLs. 
  
To check whether it has been created use command:

```
kubectl -n onap get configmap
```

A ConfigMap with mentioned name should be visible on the list.

## Mounting ConfigMap into VES

To mount created ConfigMap in VES, its deployment must be edited. It can be done with:
```
kubectl -n onap edit deployment dep-dcae-ves-collector
```

1. Add volumeMounts element

    In spec.template.spec.containers[0].volumeMounts add new list element:
    
    **NOTE**: spec.template.spec.containers[0] should be container with the image:
        *nexus.onap.dyn.nesc.nokia.net:10001/onap/org.onap.dcaegen2.collectors.ves.vescollector:x.x.x*.
        It should be the first container, but make sure that the correct container is being edited.

    ```
    volumeMounts:
      - ...
      - mountPath: /opt/app/VESCollector/etc/externalRepoCustom
        name: stnd-defined-configmap
    ```
   
    - mountPath - Directory context for schemas, should be the same as configuration of VES Collector property 
    *collector.externalSchema.schemasLocation* in *collector.properties*. This property might be modified via Consul UI, 
    later after changes in deployment.
   
   - name - Name of ConfigMap volume. Must be the same as set in the 2. step. 

2. Add volumes element

    In spec.template.spec.volumes add a new list element with all desired to mount schemas from ConfigMap in 
    *items* list. *key* are file names from generated previously spec and *path* is relative path from directory set up 
    in step 1 as *mountPath*.
    
    **NOTE**: For correct schemas detection in VES Collector *path* of each schema should be the same as its localURL in 
    mapping file. Mapping file is included as the last file in generated ConfigMap spec.
    
    **NOTE**: For correct mapping file detection in VES Collector its *path* should be the same as in property 
    *collector.externalSchema.mappingFileLocation* in *collector.properties*. This property might be modified via Consul
     UI, later after changes in deployment. 
    
    ```
    volumes:
    - configMap:
        defaultMode: 420
        items:
        - key: schema-map.json
          path: schema-map.json
        - key: master-faultMnS.yaml
          path: 3gpp/rep/sa5/data-models/OpenAPI/faultMnS.yaml
        - ...
        name: stnd-defined-configmap
      name: stnd-defined-configmap
    - ...
    ```

3. Save and close an editor, K8S will automatically detect changes, terminate old VES Pod and deploy new one with 
mounted ConfigMap. Correctness of new VES Pod initialization and mounting ConfigMap can be tracked using 
`kubectl describe pod <VES_POD_NAME>`.


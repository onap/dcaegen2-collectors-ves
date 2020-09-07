StndDefined schemas ConfigMap generator
=======================================

## Description
Bash script generate.sh generates Kubernetes ConfigMap spec containing schema files downloaded from selected Git 
repository and mapping file for schemas. After spec creation script automatically create ConfigMap in Kubernetes from 
spec.

**NOTE:** This script overrides any existing ConfigMap with name `stnd-defined-configmap` in K8S.

## Requirements and limitations

### Operating system
Script works and has been tested on Ubuntu 18.04 and Ubuntu 19.10.

### Environment
Despite the fact that this script is located in the VES Collector repository, it should be ran on the RKE node.
This way it can create ConfigMap in the same K8S environment as VES Pod is installed.

### Repository limitations
When running the script whole selected repository is being downloaded. Time of script execution depends mostly on 
repo size and number of schemas. All YAML files from selected directory in repo will be considered as schemas and 
attached to ConfigMap spec.


## Instruction
To run script use the following command:
```
./create-schemas-configmap.sh <GIT_REPO_URL> <GIT_BRANCH> <SCHEMAS_PATH> <ORGANISATION_NAME>
```

#### Parameters description
- **GIT_REPO_URL** - URL from remote Git repository which lets to clone repo via HTTPS.  
- **GIT_BRANCH** - valid branch from selected Git repository which contains schemas desired to mount
- **SCHEMAS_PATH** - path to schemas directory on selected Git repo
- **ORGANISATION_NAME** - name of organisation delivering schemas, used only for schemas destination directory in VES

Example:
```
./create-schemas-configmap.sh https://forge.3gpp.org/rep/sa5/data-models.git master /OpenAPI 3gpp
```

## ConfigMap validation
After running the script ConfigMap spec file is generated in current working directory.
Spec file can be validated via any text editor. The last file included in spec should be schema-map.json file with
mappings of external URLs to prepared local URLs.
  
ConfigMap is added to K8S under name `stnd-defined-configmap`.

**NOTE:** This script overrides any existing ConfigMap with name `stnd-defined-configmap` in K8S.

To check whether it has been created use command:

```
kubectl -n onap get configmap | grep stnd-defined-configmap
```

A ConfigMap with mentioned name should be visible on the list.


## Mounting ConfigMap into VES

To mount created ConfigMap in VES, its deployment must be edited. It can be done with:
```
kubectl -n onap edit deployment dep-dcae-ves-collector
```

1. Add volumeMounts element

    In spec.template.spec.containers[0].volumeMounts add new list element:

    ```
    volumeMounts:
      - ...
      - mountPath: /opt/app/VESCollector/etc/externalRepo
        name: stnd-defined-configmap
    ```

    **NOTE**: spec.template.spec.containers[0] should be container with the image:
    *nexus.onap.dyn.nesc.nokia.net:10001/onap/org.onap.dcaegen2.collectors.ves.vescollector:x.x.x*.
    It should be the first container, but make sure that the correct container is being edited.

2. Add volumes element

    Next in spec.template.spec.volumes add new list element with all desired to mount schemas from ConfigMap in *items* 
    list. *key* are file names from generated previously spec and *path* is relative path from directory set up in 
    step 1 as *mountPath*.
    
    For correct schemas detection in VES Collector *path* of each schema should be the same as localURL in 
    schema-map.json for that schema. schema-map.json is included as last file in generated ConfigMap spec:
    
    ```
    volumes:
    - configMap:
        defaultMode: 420
        items:
        - key: schema-map.json
          path: schema-map.json
        - key: faultMnS.yaml
          path: 3gpp/rep/sa5/data-models/OpenAPI/faultMnS.yaml
        - ...
        name: stnd-defined-configmap
    - ...
    ```

3. Leave editor, K8S will automatically detect changes, terminate old VES Pod and deploy new one with mounted ConfigMap.
Correctness of new VES Pod initialization and mounting ConfigMap can be tracked using 
`kubectl describe pod <VES_POD_NAME>`.
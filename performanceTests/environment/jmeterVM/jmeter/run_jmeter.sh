#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters. Expected two parameters - performance tests env directory name and test scenario file name"
    exit 1
fi

performance_tests_env_directory=$1
test_scenario_file=$2

docker pull justb4/jmeter
export volume_path=~/${performance_tests_env_directory}/jmeter/testScenarios && \
  export jmeter_path=/mnt/jmeter && \
  docker run \
    --name jmeter \
    --rm \
    --volume "${volume_path}":${jmeter_path} \
    justb4/jmeter \
    -n -X \
    -t ${jmeter_path}/${test_scenario_file}
#!/bin/bash
docker pull justb4/jmeter

export volume_path=/root/vesPerformanceTestsEnv && \
export jmeter_path=/mnt/jmeter && \
export test_scenario_file=test_scenario.jmx && \
docker run \
  --volume "${volume_path}":${jmeter_path} \
  justb4/jmeter \
  -n -X \
  -t ${jmeter_path}/${test_scenario_file}
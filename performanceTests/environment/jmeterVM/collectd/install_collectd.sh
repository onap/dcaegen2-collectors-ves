#! /bin/bash

sudo apt-get update && sudo apt-get install collectd -y
sudo cp vesPerformanceTestsEnv/collectd/collectd.conf /etc/collectd/collectd.conf
sudo service collectd restart


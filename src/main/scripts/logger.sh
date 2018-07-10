#!/bin/bash
###
# ============LICENSE_START=======================================================
# PROJECT
# ================================================================================
# Copyright (C) 2018 Nokia Networks Intellectual Property. All rights reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================
###

log() {
  logMessage "INFO " "$1"
}

logWarn() {
  logMessage "WARN " "$1"
}

logError() {
  logMessage "ERROR " "$1"
}

# Mimics log4j formatter so log files are consistent
logMessage() {
	echo "[$(date -u +'%Y-%m-%d %H:%M:%S,%3N')][$1][$(printf "%-9s %s\n" "PID $$")][$0] - $2"
}

# Run command, catch all the stdout and stderr and based on whether it succeeded, take the output,
# and log them using common formatter.
# It is done, so that the log files could be consistent and not look like swiss cheese having
# nicely formatted lines surrounded with raw command outputs
# All log lines that are logged by those external comments are prepended with (ext process) so they
# can be distinguished from hand-rolled messages
loggedCommand() {
  output=$($1 2>&1)
  if [ ! -z "${output}" ]; then
    if [ $? -eq 0 ]; then
        while read -r line; do
            log "(ext process) $line"
        done <<< "$output"
    else
        while read -r line; do
            logError "(ext process) $line"
        done <<< "$output"
    fi
  fi
}
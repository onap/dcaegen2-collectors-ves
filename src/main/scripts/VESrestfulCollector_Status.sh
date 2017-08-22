###
# ============LICENSE_START=======================================================
# PROJECT
# ================================================================================
# Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

#!/bin/sh

#secPid=`pgrep -f com.att.dcae.commonFunction.CommonStartup` --> master
secPid=`pgrep -f org.onap.dcae.commonFunction.CommonStartup`


if [ "${secPid}" ]
then
        #errorcnt = `grep -c "CambriaSimplerBatchPublisher - Send failed" ../logs/collector.log`
        errorcnt=`tail -1000 ../logs/collector.log | grep -c "CambriaSimplerBatchPublisher - Send failed"`
        
        if [ $errorcnt -gt 10 ]
        then
                echo "VESCollecter_Is_HavingError to publish"
        else
                echo "VESCollecter_Is_Running as PID $secPid"
        fi
else
   echo "VESCollecter_Is_Not_Running"
fi
exit

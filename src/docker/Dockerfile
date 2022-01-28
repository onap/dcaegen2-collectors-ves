# ============LICENSE_START=======================================================
#  Copyright (c) 2020-2022 Nokia. All rights reserved.
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
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================

FROM nexus3.onap.org:10001/onap/integration-java11:10.0.0

ENV HOSTALIASES /etc/host.aliases
ARG user=vescollector
ARG group=vescollector
WORKDIR /opt/app/VESCollector

ADD ./ ./
USER root

RUN addgroup $group && adduser --system --disabled-password --no-create-home --ingroup $group $user
RUN chown -R vescollector:vescollector /opt/app
RUN apk update && apk add procps && apk add less && apk add vim && apk add curl && apk add bash
RUN mkdir /opt/app/VESCollector/logs
RUN chown -R vescollector:vescollector /opt/app/VESCollector/logs
RUN chown -R vescollector:vescollector /opt/app/VESCollector/etc
RUN chmod +x bin/*.sh
USER $user

EXPOSE 8080 8443

ENTRYPOINT bin/docker-entry.sh

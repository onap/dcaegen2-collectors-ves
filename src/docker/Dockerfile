# ============LICENSE_START=======================================================
#  Copyright (c) 2020-2022 Nokia. All rights reserved.
#  Copyright (c) 2026 Deutsche Telekom Intellectual Property. All rights reserved.
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

FROM eclipse-temurin:11-jre-alpine

ENV HOSTALIASES=/etc/host.aliases
ARG user=vescollector
ARG group=vescollector
WORKDIR /opt/app/VESCollector

COPY ./ ./

RUN addgroup "$group" \
    && adduser --system --disabled-password --no-create-home --ingroup "$group" "$user" \
    && apk add --no-cache procps curl \
    && mkdir -p /opt/app/VESCollector/logs \
    && chown -R vescollector:vescollector /opt/app \
    && chmod +x bin/*.sh

USER $user

EXPOSE 8080 8443

ENTRYPOINT ["bin/docker-entry.sh"]

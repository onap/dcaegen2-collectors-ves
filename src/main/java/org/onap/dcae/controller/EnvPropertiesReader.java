/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2018 Nokia. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.dcae.controller;

import io.vavr.collection.Map;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.vavr.API.List;
import static io.vavr.API.Try;
import static org.onap.dcae.common.event.publishing.VavrUtils.f;

final class EnvPropertiesReader {

    private final static Logger log = LoggerFactory.getLogger(EnvPropertiesReader.class);

    static Option<EnvProps> readEnvProps(Map<String, String> environmentVariables) {
        log.info("Loading necessary environment variables for dynamic configuration update");
        int consulPort = getConsulPort(environmentVariables);
        String consulProtocol = getConsulProtocol(environmentVariables);
        String cbsProtocol = getCbsProtocol(environmentVariables);
        Option<String> consulHost = getConsulHost(environmentVariables);
        Option<String> cbsServiceName = getCBSName(environmentVariables);
        Option<String> vesCollectorAppName = getAppName(environmentVariables);
        return Option.sequence(List(consulHost, cbsServiceName, vesCollectorAppName))
            .map(e -> new EnvProps(consulProtocol, e.get(0), consulPort, cbsProtocol, e.get(1), e.get(2)))
            .onEmpty(() -> log.warn("Some required environment variables are missing"))
            .peek(props -> log.info(f("Discovered following environment variables: '%s'", props)));
    }

    private static Option<String> getAppName(Map<String, String> environmentVariables) {
        return environmentVariables.get("HOSTNAME")
            .orElse(environmentVariables.get("SERVICE_NAME"))
            .onEmpty(() -> log.warn("App service name (as registered in CBS) (env var: 'HOSTNAME' / 'SERVICE_NAME') "
                + "is missing error environment variables."));
    }

    private static Option<String> getCBSName(Map<String, String> environmentVariables) {
        return environmentVariables.get("CONFIG_BINDING_SERVICE")
            .onEmpty(() -> log.warn("Name of CBS Service (as registered in Consul) (env var: 'CONFIG_BINDING_SERVICE') "
                + "is missing from environment variables."));
    }

    private static Integer getConsulPort(Map<String, String> environmentVariables) {
        return environmentVariables.get("CONSUL_PORT")
            .flatMap(str -> Try(() -> Integer.valueOf(str))
                .onFailure(exc -> log.warn("Consul port is not an integer value", exc))
                .toOption())
            .onEmpty(() -> log.warn("Consul port (env var: 'CONSUL_PORT') is missing from environment variables. "
                + "Using default value of 8500"))
            .getOrElse(8500);
    }

    private static Option<String> getConsulHost(Map<String, String> environmentVariables) {
        return environmentVariables.get("CONSUL_HOST")
            .onEmpty(() -> log.warn("Consul host (env var: 'CONSUL_HOST') (without port) "
                + "is missing from environment variables."));
    }

    private static String getConsulProtocol(Map<String, String> environmentVariables) {
        return getProtocolFrom("CONSUL_PROTOCOL", environmentVariables);
    }

    private static String getCbsProtocol(Map<String, String> environmentVariables) {
        return getProtocolFrom("CBS_PROTOCOL", environmentVariables);
    }

    private static String getProtocolFrom(String variableName, Map<String, String> environmentVariables) {
        return environmentVariables.get(variableName)
            .onEmpty(() -> log.warn("Consul protocol (env var: '" + variableName + "') is missing "
                + "from environment variables."))
            .getOrElse("http");
    }

}

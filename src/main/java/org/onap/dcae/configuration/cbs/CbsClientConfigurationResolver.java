/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
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
package org.onap.dcae.configuration.cbs;

import io.vavr.control.Option;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsClientConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.ImmutableCbsClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CbsClientConfigurationResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(CbsClientConfigurationResolver.class);
    private static final String HOSTNAME_ENV = "CONFIG_BINDING_SERVICE";
    private static final String PORT_ENV = "CONFIG_BINDING_SERVICE_SERVICE_PORT";
    private static final String APP_NAME_ENV = "HOSTNAME";
    static final String ENV_CONFIG_PROTOCOL = "http";

    private final String defaultProtocol;
    private final String defaultHostname;
    private final int defaultPort;
    private final String defaultAppName;

    CbsClientConfigurationResolver(String defaultProtocol, String defaultHostname, int defaultPort,
        String defaultAppName) {
        this.defaultProtocol = defaultProtocol;
        this.defaultHostname = defaultHostname;
        this.defaultPort = defaultPort;
        this.defaultAppName = defaultAppName;
    }

    CbsClientConfiguration resolve() {
        Option<CbsClientConfiguration> cbsClientConfiguration = readConfigurationFromEnvrionment();
        return cbsClientConfiguration.getOrElse(this::getFallbackConfiguration);
    }

    private ImmutableCbsClientConfiguration getFallbackConfiguration() {
        LOGGER.info("Falling back to use default CBS client configuration");
        return createCbsClientConfiguration(defaultProtocol, defaultHostname, defaultAppName, defaultPort);
    }

    private Option<CbsClientConfiguration> readConfigurationFromEnvrionment() {
        String hostname = System.getenv(HOSTNAME_ENV);
        String port = System.getenv(PORT_ENV);
        String appName = System.getenv(APP_NAME_ENV);

        if (!areEnvVarsPresent(hostname, port, appName)) {
            LOGGER.warn("Failed resolving CBS client configuration from system environments");
            return Option.none();
        }
        return Option.of(createCbsClientConfiguration(ENV_CONFIG_PROTOCOL, hostname, appName, Integer.valueOf(port)));
    }

    private boolean areEnvVarsPresent(String hostname, String port, String appName) {
        return hostname != null
            && port != null
            && appName != null;
    }

    private ImmutableCbsClientConfiguration createCbsClientConfiguration(String protocol, String hostname,
        String appName, Integer port) {
        return ImmutableCbsClientConfiguration.builder()
            .protocol(protocol)
            .hostname(hostname)
            .port(port)
            .appName(appName)
            .build();
    }
}
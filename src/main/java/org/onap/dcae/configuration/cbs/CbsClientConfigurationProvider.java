/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
 * Copyright (C) 2022 AT&T Intellectual Property. All rights reserved.
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

import org.jetbrains.annotations.NotNull;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsClientConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.ImmutableCbsClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CbsClientConfigurationProvider is used to provide production or dev configuration for CBS client.
 */
public class CbsClientConfigurationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CbsClientConfigurationProvider.class);


    private static final String DEFAULT_APP_NAME = "dcae-ves-collector";
    private static final String DEV_MODE_PROPERTY = "devMode";
    private static final String CBS_PORT_PROPERTY = "cbsPort";
    private static String CBS_CLIENT_CONFIG_PATH = "/app-config/application_config.yaml";
    
    /**
     * Returns configuration for CBS client.
     * @return Production or dev configuration for CBS client, depends on application run arguments.
     */
    public CbsClientConfiguration get() {
        try {
            if (isDevModeEnabled()) {
                return getDevConfiguration();
            } else {
                return CbsClientConfiguration.fromEnvironment();
            }
        } catch (Exception e) {
            LOGGER.warn(String.format("Failed resolving CBS client configuration from system environments: %s", e));
        }
        return getFallbackConfiguration();
    }

    @NotNull
    private ImmutableCbsClientConfiguration getDevConfiguration() {
        System.setProperty(CBS_CLIENT_CONFIG_PATH,"./etc/ves-application_config.yaml");
        return createCbsClientConfiguration(DEFAULT_APP_NAME);
    }

    private boolean isDevModeEnabled() {
        return System.getProperty(DEV_MODE_PROPERTY) != null;
    }

    private ImmutableCbsClientConfiguration getFallbackConfiguration() {
        LOGGER.info("Falling back to use default CBS client configuration");
        return createCbsClientConfiguration(DEFAULT_APP_NAME);
    }

    private ImmutableCbsClientConfiguration createCbsClientConfiguration(String appName) {
        return ImmutableCbsClientConfiguration.builder()
            .appName(appName)
            .build();
    }
}

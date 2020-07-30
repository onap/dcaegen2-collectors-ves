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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsClientConfiguration;

public class CbsClientConfigurationResolverTest {

    private static final String DEFAULT_PROTOCOL = "http";
    private static final String DEFAULT_HOSTNAME = "config-binding-service";
    private static final int DEFAULT_PORT = 10000;
    private static final String DEFAULT_APP_NAME = "dcae-ves-collector";

    @Test
    @DisabledIfEnvironmentVariable(named = "CONFIG_BINDING_SERVICE", matches = ".+")
    public void shouldLoadDefaultConfigWhenEnvNotPresent() {
        // when
        CbsClientConfiguration configuration = new CbsClientConfigurationResolver(
            DEFAULT_PROTOCOL, DEFAULT_HOSTNAME, DEFAULT_PORT, DEFAULT_APP_NAME).resolve();

        // then
        assertThat(configuration.protocol()).isEqualTo(DEFAULT_PROTOCOL);
        assertThat(configuration.hostname()).isEqualTo(DEFAULT_HOSTNAME);
        assertThat(configuration.port()).isEqualTo(DEFAULT_PORT);
        assertThat(configuration.appName()).isEqualTo(DEFAULT_APP_NAME);
    }
}
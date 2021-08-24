/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2020-2021 Nokia. All rights reserved.s
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
package org.onap.dcae.configuration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.vavr.control.Option;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.dcae.configuration.cbs.CbsClientConfigurationProvider;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsClientConfiguration;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CbsConfigurationHandlerTest {

    private static final String VES_CONFIG = "{\"collector.port\": 8081}";
    private static final String VES_CONSUL_CONFIG = String.format("{\"config\":%s}", VES_CONFIG);

    @Mock
    private CbsClientConfigurationProvider cbsClientConfigurationProvider;
    @Mock
    private CbsClientConfiguration cbsClientConfiguration;
    @Mock
    private Mono<CbsClient> cbsClient;
    @Mock
    private ConfigUpdater configLoader;

    @InjectMocks
    @Spy
    private ConfigurationHandler cbsConfigurationHandler;

    @Test
    public void shouldCreateCbsConfigurationHandler() {
        // given
        doReturn(cbsClient).when(cbsConfigurationHandler).createCbsClient(cbsClientConfiguration);
        doReturn(cbsClientConfiguration).when(cbsClientConfigurationProvider).get();

        // when
        final Disposable handler = cbsConfigurationHandler.startListen(Duration.ofMinutes(5));

        // then
        assertThat(handler).isNotNull();
    }

    @Test
    public void shouldUpdateAppConfigurationWhenConfigurationIsValid() {
        // given
        final JsonObject configuration = createConfiguration(VES_CONSUL_CONFIG);

        // when
        this.cbsConfigurationHandler.handleConfiguration(configuration);

        // then
        final ArgumentCaptor<Option<JSONObject>> acConfiguration = ArgumentCaptor.forClass(Option.class);
        verify(configLoader).updateConfig(acConfiguration.capture());
        assertThat(acConfiguration.getValue().get().toString()).hasToString(createJSONObject(VES_CONFIG));
    }

    @Test
    public void shouldReportAnErrorWhenConfigHandlerReturnsEmptyConfiguration() {
        // given
        final JsonObject configuration = createConfiguration("{}");

        // when
        assertThatThrownBy(() -> this.cbsConfigurationHandler.handleConfiguration(configuration))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(String.format("Invalid application configuration: %s ", "{}"));

        // then
        verify(configLoader, never()).updateConfig(any());
    }

    private String createJSONObject(String vesConfig) {
        return new JSONObject(vesConfig).toString();
    }

    private JsonObject createConfiguration(String vesConfig) {
        return new JsonParser().parse(vesConfig).getAsJsonObject();
    }


}

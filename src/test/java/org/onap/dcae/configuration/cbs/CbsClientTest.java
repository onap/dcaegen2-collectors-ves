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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsClientConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.ImmutableCbsClientConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CbsClientTest {

    private static final String VES_CONFIG = "{\"collector.port\": 8081}";
    private static final String HOSTNAME = "localhost";
    private static final String PROTOCOL = "http";
    private static final String APP_NAME = "VESCollector";

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(
            wireMockConfig().dynamicPort().dynamicPort());

    @Test
    public void shouldFetchConfigurationFromCBS() {
        // given
        final int PORT = wireMockRule.port();
        stubCBSToReturnAppConfig();

        // when
        CbsClientConfiguration cbsClientConfiguration = ImmutableCbsClientConfiguration.builder()
                .protocol(PROTOCOL)
                .hostname(HOSTNAME)
                .port(PORT)
                .appName(APP_NAME)
                .build();
        JSONObject appConfig = new CbsClient(cbsClientConfiguration).getAppConfig().get();

        // then
        assertThat(appConfig).isNotNull();
        assertThat(appConfig.toString()).isEqualTo(new JSONObject(VES_CONFIG).toString());
    }

    private void stubCBSToReturnAppConfig() {
        stubFor(get(urlEqualTo("/service_component/VESCollector"))
                .willReturn(aResponse().withBody(CbsClientTest.VES_CONFIG)));
    }
}
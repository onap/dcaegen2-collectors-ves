/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
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
package org.onap.dcae;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.vavr.API.Map;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.vavr.collection.Map;
import org.junit.Rule;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
public class WiremockBasedTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(
        wireMockConfig().dynamicPort().dynamicHttpsPort().keystorePath(null));

    protected void stubConsulToReturnLocalAddressOfCBS() {
        stubFor(get(urlEqualTo("/v1/catalog/service/CBSName"))
            .willReturn(aResponse().withBody(validLocalCBSConf())));
    }

    protected void stubCBSToReturnAppConfig(String sampleConfigForVES) {
        stubFor(get(urlEqualTo("/service_component/VESCollector"))
            .willReturn(aResponse().withBody(sampleConfigForVES)));
    }

    protected Map<String, String> wiremockBasedEnvProps() {
        return Map(
            "CONSUL_HOST", "http://localhost",
            "CONSUL_PORT", "" + wireMockRule.port(),
            "HOSTNAME", "VESCollector",
            "CONFIG_BINDING_SERVICE", "CBSName"
        );
    }

    protected String validLocalCBSConf() {
        return ""
            + "[{ "
            + "\"ServiceAddress\": \"http://localhost\","
            + "\"ServicePort\":" + wireMockRule.port()
            + "}]";
    }
}

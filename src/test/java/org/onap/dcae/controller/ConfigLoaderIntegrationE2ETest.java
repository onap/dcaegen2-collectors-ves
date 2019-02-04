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

import static io.vavr.API.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.onap.dcae.TestingUtilities.createTemporaryFile;
import static org.onap.dcae.TestingUtilities.readFile;
import static org.onap.dcae.TestingUtilities.readJSONFromFile;
import static org.onap.dcae.common.publishing.VavrUtils.f;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.dcae.AliasConfig;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.WiremockBasedTest;
import org.onap.dcae.common.publishing.DMaaPConfigurationParser;
import org.onap.dcae.common.publishing.EventPublisher;

@RunWith(MockitoJUnitRunner.class)
public class ConfigLoaderIntegrationE2ETest extends WiremockBasedTest {

    @Mock
    private ApplicationSettings properties;

    @Test
    public void testSuccessfulE2EFlow() {
        // given
        Path dMaaPConfigFile = createTemporaryFile("{}");
        Path collectorPropertiesFile = createTemporaryFile("");
        Path dMaaPConfigSource = Paths.get("src/test/resources/testParseDMaaPCredentialsGen2.json");
        JSONObject dMaaPConf = readJSONFromFile(dMaaPConfigSource);
        stubConsulToReturnLocalAddressOfCBS();
        stubCBSToReturnAppConfig(f("{\"collector.port\": 8080, \"streams_publishes\": %s}}", dMaaPConf));

        EventPublisher eventPublisherMock = mock(EventPublisher.class);
        ConfigFilesFacade configFilesFacade = new ConfigFilesFacade(dMaaPConfigFile, collectorPropertiesFile);
        ConfigLoader configLoader = new ConfigLoader(eventPublisherMock::reconfigure, configFilesFacade, ConfigSource::getAppConfig, () -> wiremockBasedEnvProps(),
            new AliasConfig(properties));
        configLoader.updateConfig();

        // then
        assertThat(readJSONFromFile(dMaaPConfigSource).toString()).isEqualTo(dMaaPConf.toString());
        assertThat(readFile(collectorPropertiesFile).trim()).isEqualTo("collector.port = 8080");
        verify(eventPublisherMock, times(1)).reconfigure(
            DMaaPConfigurationParser.parseToDomainMapping(dMaaPConf).get()
        );
    }

    @Test
    public void shouldNotReconfigureNotOverwriteIfConfigurationHasNotChanged() {
        // given
        Path dMaaPConfigFile = createTemporaryFile("{}");
        Path collectorPropertiesFile = createTemporaryFile("");
        JSONObject dMaaPConf = readJSONFromFile(Paths.get("src/test/resources/testParseDMaaPCredentialsGen2.json"));
        stubConsulToReturnLocalAddressOfCBS();
        stubCBSToReturnAppConfig(f("{\"collector.port\": 8080, \"streams_publishes\": %s}}", dMaaPConf));
        EventPublisher eventPublisherMock = mock(EventPublisher.class);
        ConfigFilesFacade configFilesFacade = new ConfigFilesFacade(dMaaPConfigFile, collectorPropertiesFile);
        configFilesFacade.writeProperties(Map("collector.port", "8080"));
        configFilesFacade.writeDMaaPConfiguration(dMaaPConf);

        // when
        ConfigLoader configLoader = new ConfigLoader(eventPublisherMock::reconfigure, configFilesFacade, ConfigSource::getAppConfig, () -> wiremockBasedEnvProps(),
            new AliasConfig(properties));
        configLoader.updateConfig();

        // then
        verifyZeroInteractions(eventPublisherMock);
    }

}
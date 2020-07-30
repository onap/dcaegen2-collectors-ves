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
package org.onap.dcae.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.dcae.TestingUtilities.readJSONFromFile;
import static org.onap.dcae.common.publishing.VavrUtils.f;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import java.nio.file.Paths;
import java.util.Optional;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.dcae.configuration.cbs.CbsClient;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ConfigLoaderTest {

    private static final String COLLECTOR_PORT = "collector.port";
    private static final String PORT_8080 = "8080";
    private static final String PORT_8081 = "8081";
    private static final String COLLECTOR_KEYSTORE_FILE_LOCATION = "collector.keystore.file.location";
    private static final String SOME_PATH = "some/path";
    private static final String COLLECTOR_SCHEMA_FILE = "collector.schema.file";
    private static final String SOME_SCHEMA = "some schema";

    @Mock
    private CbsClient cbsClientMock;

    @Mock
    private ConfigFilesFacade configFilesFacadeMock;

    @InjectMocks
    private ConfigLoader configLoader;

    @Mock
    private Runnable applicationRestarter;


    @Before
    public void setup() {
        when(configFilesFacadeMock.readCollectorProperties()).thenReturn(Try.of(HashMap::empty));
        when(configFilesFacadeMock.readDMaaPConfiguration()).thenReturn(Try.of(JSONObject::new));
    }

    @Test
    public void shouldCallConfigSourceForData() {
        // given
        HashMap<String, String> properties = HashMap.of(COLLECTOR_PORT, PORT_8080);
        mockVesInitialProperties(properties);
        mockVesConfigInCbs(properties);

        // when
        configLoader.updateConfig();

        // then
        verify(cbsClientMock).getAppConfig();
    }

    @Test
    public void shouldNotUpdatePropertiesWhenSameKeySetAndSameValues() {
        // given
        HashMap<String, String> properties = HashMap.of(COLLECTOR_PORT, PORT_8080);
        mockVesInitialProperties(properties);
        mockVesConfigInCbs(properties);

        // when
        configLoader.updateConfig();

        // then
        verify(configFilesFacadeMock, never()).writeProperties(any());
        verify(applicationRestarter, never()).run();
    }

    @Test
    public void shouldUpdatePropertiesWhenSameKeySetButDifferentValues() {
        // given
        HashMap<String, String> initialProperties = HashMap.of(COLLECTOR_PORT, PORT_8080);
        HashMap<String, String> cbsProperties = HashMap.of(COLLECTOR_PORT, PORT_8081);
        mockVesInitialProperties(initialProperties);
        mockVesConfigInCbs(cbsProperties);

        // when
        configLoader.updateConfig();

        // then
        verify(configFilesFacadeMock, times(1)).writeProperties(cbsProperties);
        verify(applicationRestarter, times(1)).run();
    }

    @Test
    public void shouldUpdatePropertiesWhenVesKeysAreSubsetOfCbsKeysAndSubsetHasSameValues() {
        // given
        HashMap<String, String> initialProperties = HashMap.of(
            COLLECTOR_PORT, PORT_8080);
        HashMap<String, String> cbsProperties = HashMap.of(
            COLLECTOR_PORT, PORT_8080,
            COLLECTOR_KEYSTORE_FILE_LOCATION, SOME_PATH);
        mockVesInitialProperties(initialProperties);
        mockVesConfigInCbs(cbsProperties);

        // when
        configLoader.updateConfig();

        // then
        verify(configFilesFacadeMock, times(1)).writeProperties(cbsProperties);
        verify(applicationRestarter, times(1)).run();
    }

    @Test
    public void shouldUpdatePropertiesWhenVesKeysAreSubsetOfCbsKeysAndSubsetHasDifferentValues() {
        HashMap<String, String> initialProperties = HashMap.of(
            COLLECTOR_PORT, PORT_8080);
        HashMap<String, String> cbsProperties = HashMap.of(
            COLLECTOR_PORT, PORT_8081,
            COLLECTOR_KEYSTORE_FILE_LOCATION, SOME_PATH);
        mockVesInitialProperties(initialProperties);
        mockVesConfigInCbs(cbsProperties);

        // when
        configLoader.updateConfig();

        // then
        verify(configFilesFacadeMock, times(1)).writeProperties(cbsProperties);
        verify(applicationRestarter, times(1)).run();
    }

    @Test
    public void shouldNotUpdatePropertiesWhenCbsKeysAreSubsetOfVesKeysAndSubsetHasSameValues() {
        HashMap<String, String> initialProperties = HashMap.of(
            COLLECTOR_PORT, PORT_8080,
            COLLECTOR_KEYSTORE_FILE_LOCATION, SOME_PATH);
        HashMap<String, String> cbsProperties = HashMap.of(
            COLLECTOR_PORT, PORT_8080);
        mockVesInitialProperties(initialProperties);
        mockVesConfigInCbs(cbsProperties);

        // when
        configLoader.updateConfig();

        // then
        verify(configFilesFacadeMock, never()).writeProperties(any());
        verify(applicationRestarter, never()).run();
    }

    @Test
    public void shouldUpdatePropertiesWhenCbsKeysAreSubsetOfVesKeysAndSubsetHasDifferentValues() {
        HashMap<String, String> initialProperties = HashMap.of(
            COLLECTOR_PORT, PORT_8080,
            COLLECTOR_KEYSTORE_FILE_LOCATION, SOME_PATH);
        HashMap<String, String> cbsProperties = HashMap.of(
            COLLECTOR_PORT, PORT_8081);
        mockVesInitialProperties(initialProperties);
        mockVesConfigInCbs(cbsProperties);

        // when
        configLoader.updateConfig();

        // then
        verify(configFilesFacadeMock, times(1)).writeProperties(cbsProperties);
        verify(applicationRestarter, times(1)).run();
    }

    @Test
    public void shouldUpdatePropertiesWhenVesAndCbsKeySetsIntersectAndIntersectingKeysHaveSameValues() {
        HashMap<String, String> initialProperties = HashMap.of(
            COLLECTOR_PORT, PORT_8080,
            COLLECTOR_KEYSTORE_FILE_LOCATION, SOME_PATH);
        HashMap<String, String> cbsProperties = HashMap.of(
            COLLECTOR_PORT, PORT_8080,
            COLLECTOR_SCHEMA_FILE, SOME_SCHEMA
        );
        mockVesInitialProperties(initialProperties);
        mockVesConfigInCbs(cbsProperties);

        // when
        configLoader.updateConfig();

        // then
        verify(configFilesFacadeMock, times(1)).writeProperties(cbsProperties);
        verify(applicationRestarter, times(1)).run();
    }

    @Test
    public void shouldUpdatePropertiesWhenVesAndCbsKeySetsIntersectAndIntersectingKeysHaveDifferentValues() {
        HashMap<String, String> initialProperties = HashMap.of(
            COLLECTOR_PORT, PORT_8080,
            COLLECTOR_KEYSTORE_FILE_LOCATION, SOME_PATH);
        HashMap<String, String> cbsProperties = HashMap.of(
            COLLECTOR_PORT, PORT_8081,
            COLLECTOR_SCHEMA_FILE, SOME_SCHEMA
        );
        mockVesInitialProperties(initialProperties);
        mockVesConfigInCbs(cbsProperties);

        // when
        configLoader.updateConfig();

        // then
        verify(configFilesFacadeMock, times(1)).writeProperties(cbsProperties);
        verify(applicationRestarter, times(1)).run();
    }

    @Test
    public void shouldUpdateDmaapConfigWhenConfigurationChanged() {
        // given
        JSONObject emptyDmaapConfig = new JSONObject();
        JSONObject dmaapConfig = loadSampleDmaapConfig();
        mockVesInitialDmaapConfig(emptyDmaapConfig);
        mockVesDmaapConfigInCbs(dmaapConfig);

        // when
        configLoader.updateConfig();

        // then
        verify(configFilesFacadeMock).writeDMaaPConfiguration(argThat(dmaapConfig::similar));
        verify(applicationRestarter, times(1)).run();
    }

    @Test
    public void shouldNotUpdateDmaapConfigWhenConfigurationNotChanged() {
        // given
        JSONObject dmaapConf = loadSampleDmaapConfig();
        mockVesInitialDmaapConfig(dmaapConf);
        mockVesDmaapConfigInCbs(dmaapConf);

        // when
        configLoader.updateConfig();

        // then
        verify(configFilesFacadeMock, never()).writeDMaaPConfiguration(any());
        verify(applicationRestarter, never()).run();
    }

    private void mockVesInitialDmaapConfig(JSONObject dmaapConf) {
        when(configFilesFacadeMock.readDMaaPConfiguration()).thenReturn(Try.of(() -> dmaapConf));
    }

    private void mockVesDmaapConfigInCbs(JSONObject dmaapConf) {
        JSONObject jsonObject = new JSONObject(f("{\"streams_publishes\": %s}}", dmaapConf));
        when(cbsClientMock.getAppConfig()).thenReturn(Optional.of(jsonObject));
    }

    private void mockVesConfigInCbs(HashMap<String, String> properties) {
        when(cbsClientMock.getAppConfig()).thenReturn(Optional.of(prepareConfigurationJson(properties)));
    }

    private void mockVesInitialProperties(HashMap<String, String> properties) {
        when(configFilesFacadeMock.readCollectorProperties()).thenReturn(Try.of(() -> properties));
    }


    private JSONObject loadSampleDmaapConfig() {
        return readJSONFromFile(Paths.get("src/test/resources/testParseDMaaPCredentialsGen2.json"));
    }

    private JSONObject prepareConfigurationJson(Map<String, String> properties) {
        String template = "{%s, \"streams_publishes\": {}}";
        String customProperties = properties
            .map(property -> "\"" + property._1 + "\": \"" + property._2 + "\"")
            .mkString(", ");
        String jsonBody = f(template, customProperties);
        return new JSONObject(jsonBody);
    }
}
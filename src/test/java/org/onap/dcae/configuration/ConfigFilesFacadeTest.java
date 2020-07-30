/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2018,2020 Nokia. All rights reserved.
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

import static io.vavr.API.Map;
import static io.vavr.API.Some;
import static org.assertj.core.api.Assertions.assertThat;
import static org.onap.dcae.TestingUtilities.assertFailureHasInfo;
import static org.onap.dcae.TestingUtilities.assertJSONObjectsEqual;
import static org.onap.dcae.TestingUtilities.createTemporaryFile;
import static org.onap.dcae.TestingUtilities.readFile;
import static org.onap.dcae.TestingUtilities.readJSONFromFile;

import io.vavr.collection.Map;
import io.vavr.control.Try;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONObject;
import org.junit.Test;

public class ConfigFilesFacadeTest {

    private static final Path NON_EXISTENT = Paths.get("/non-existent");
    private static final ConfigFilesFacade TO_NON_EXISTENT_POINTING_FACADE = new ConfigFilesFacade(NON_EXISTENT,
        NON_EXISTENT);

    @Test
    public void shouldReadPropertyFile() {
        // given
        Path temporaryFile = createTemporaryFile("some.property=10");

        // when
        ConfigFilesFacade configFilesFacade = new ConfigFilesFacade(temporaryFile, temporaryFile);

        Try<Map<String, String>> propertiesConfigurations = configFilesFacade.readCollectorProperties();

        // then
        assertThat(propertiesConfigurations.isSuccess()).isTrue();
        assertThat(propertiesConfigurations.get().containsKey("some.property")).isTrue();
        assertThat(propertiesConfigurations.get().get("some.property")).isEqualTo(Some("10"));
    }


    @Test
    public void shouldReadDMaaPFile() {
        // given
        Path temporaryFile = createTemporaryFile("{}");

        // when
        ConfigFilesFacade configFilesFacade = new ConfigFilesFacade(temporaryFile, temporaryFile);

        Try<JSONObject> dMaaPConfiguration = configFilesFacade.readDMaaPConfiguration();

        // then
        assertThat(dMaaPConfiguration.isSuccess()).isTrue();
        assertThat(dMaaPConfiguration.get().toString()).isEqualTo("{}");
    }

    @Test
    public void shouldWriteDMaaPConf() {
        // given
        Path temporaryFile = createTemporaryFile("{}");
        JSONObject desiredConf = new JSONObject("{\"key\": 1}");

        // when
        ConfigFilesFacade configFilesFacade = new ConfigFilesFacade(temporaryFile, temporaryFile);

        Try<Void> propertiesConfigurations = configFilesFacade.writeDMaaPConfiguration(desiredConf);

        // then
        assertThat(propertiesConfigurations.isSuccess()).isTrue();
        assertJSONObjectsEqual(readJSONFromFile(temporaryFile), desiredConf);
    }


    @Test
    public void shouldWriteProperties() {
        // given
        Path temporaryFile = createTemporaryFile("{}");

        // when
        ConfigFilesFacade configFilesFacade = new ConfigFilesFacade(temporaryFile, temporaryFile);
        Try<Void> propertiesConfigurations = configFilesFacade.writeProperties(Map("prop1", "hi"));

        // then
        assertThat(propertiesConfigurations.isSuccess()).isTrue();
        assertThat(readFile(temporaryFile).trim()).isEqualTo("prop1 = hi");
    }

    @Test
    public void shouldContainPropertiesPathInCaseOfFailures() {
        Try<Map<String, String>> result = TO_NON_EXISTENT_POINTING_FACADE.readCollectorProperties();
        assertThat(result.isFailure()).isTrue();
        assertFailureHasInfo(result, NON_EXISTENT.toString());
    }

    @Test
    public void shouldContainDMaaPPathPathInCaseOfFailures() {
        Try<JSONObject> result = TO_NON_EXISTENT_POINTING_FACADE.readDMaaPConfiguration();
        assertThat(result.isFailure()).isTrue();
        assertFailureHasInfo(result, NON_EXISTENT.toString());
    }

    @Test
    public void shouldContainPropertiesPathPathInCaseOfFailuresOnWrite() {
        // given
        Try<Void> result = TO_NON_EXISTENT_POINTING_FACADE.writeProperties(Map("any", "any"));
        assertThat(result.isFailure()).isTrue();
        assertFailureHasInfo(result, NON_EXISTENT.toString());
    }

    @Test
    public void shouldContainDMaaPPathPathInCaseOfFailuresOnWrite() {
        // given
        Try<Void> result = TO_NON_EXISTENT_POINTING_FACADE.writeDMaaPConfiguration(new JSONObject());
        assertThat(result.isFailure()).isTrue();
        assertFailureHasInfo(result, NON_EXISTENT.toString());
    }
}
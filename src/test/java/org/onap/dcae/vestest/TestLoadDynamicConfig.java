/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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
package org.onap.dcae.vestest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onap.dcae.vestest.TestingUtilities.createTemporaryFile;

import com.github.fge.jackson.JsonLoader;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.onap.dcae.controller.LoadDynamicConfig;

public class TestLoadDynamicConfig {

    private Path temporaryFile;

    @Before
    public void setUp() {
        temporaryFile = createTemporaryFile();
    }

    @Test
    public void shouldReadFileContent() throws IOException {
        // given
        String expectedJSON = "{ \"field\" : 1 }";
        Files.write(temporaryFile, expectedJSON.getBytes());

        // when
        String readFileContent = LoadDynamicConfig.readFile(temporaryFile.toString());

        // then
        assertEquals(JsonLoader.fromString(expectedJSON), JsonLoader.fromString(readFileContent));
    }

    @Test
    public void shouldWriteFileAndAttachDMaaPRelatedPropertiesFromConfiguration() {
        // given
        LoadDynamicConfig loadDynamicConfig = new LoadDynamicConfig();
        loadDynamicConfig.propFile = "src/test/resources/test_collector_ip_op.properties";
        loadDynamicConfig.configFile = "src/test/resources/controller-config_dmaap_ip.json";
        loadDynamicConfig.dMaaPOutputFile = temporaryFile.toString();
        String sampleConfiguration = LoadDynamicConfig.readFile(loadDynamicConfig.configFile);

        // when
        loadDynamicConfig.writeconfig(new JSONObject(sampleConfiguration));

        // then
        JsonObject actuallyWrittenJSONContent = TestingUtilities.readJSONFromFile(temporaryFile);
        assertTrue(actuallyWrittenJSONContent.has("ves-fault-secondary"));
    }

}


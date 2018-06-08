/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

/*
 *
 * Purpose: CommonCollectorJunitTest is the wrapper class to invoke all prescribed Junit test cases.
 *
 */

package org.onap.dcae.vestest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import org.json.JSONObject;
import org.junit.Test;

public class TestDefaultConfiguration {

    @Test
    public void shouldDefaultCollectorSchemaFileBeAValidJson() throws IOException {
        try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get("etc/collector.properties"))) {
            Properties properties = new Properties();
            properties.load(bufferedReader);
            new JSONObject(properties.getProperty("collector.schema.file"));
        }
    }
}


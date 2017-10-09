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

package org.onap.dcae.vestest;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.Map;

import org.json.simple.JSONObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onap.dcae.controller.LoadDynamicConfig;

public class TestLoadDynamicConfig {

    LoadDynamicConfig lc;
    String propop = "src/test/resources/testcollector.properties";

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testLoad() {

        //File file = new File(".");
        //for(String fileNames : file.list()) System.out.println(fileNames);

        lc = new LoadDynamicConfig();
        lc.propFile = "src/test/resources/testcollector.properties";
        lc.configFile = "src/test/resources/controller-config.json";

        String data = LoadDynamicConfig.readFile(propop);
        assertEquals(data.isEmpty(), false);
    }
}


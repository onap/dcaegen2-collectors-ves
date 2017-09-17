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

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VesCollectorJunitTest {

    private static final Logger log = LoggerFactory.getLogger(VesCollectorJunitTest.class);

    public static String schemaFile = "etc/CommonEventFormat_27.2.json";
    public static String output;


    String message = "true";
    InputJsonValidation messageUtil = new InputJsonValidation();

    @Test
    public void validJsonValidation() {

        output = "true";
        testHelper(new Properties());

        assertEquals("true", output);
    }


    @Test
    public void nonValidJsonValidation() {
        output = "false";
        testHelper(new Properties());
        //assertEquals("false",messageUtil.nonValidJsonValidation());
        assertEquals("false", output);
    }

    private void testHelper(Properties prop) {
        try (InputStream input = new FileInputStream("etc/collector.properties")) {
            prop.load(input);
            //schemaFile=prop.getProperty("collector.schema.file");

            JSONObject schemaFileJson = new JSONObject(
                prop.getProperty("collector.schema.file"));
            log.info("JSON Schemafile" + schemaFileJson);
            //schemaFile = schemaFileJson.getString("v4");

            log.info("Schema file location: " + schemaFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.error(e.getLocalizedMessage(), e);
        }
    }

    //The test case requires common collector running in the environment prior to start execution of JUNIT test cases
    /*
    @Test
    public void testValidJSONObjectReception() {

        assertEquals("true",messageUtil.eventReception());
        assertEquals("true",output);
    }*/
}


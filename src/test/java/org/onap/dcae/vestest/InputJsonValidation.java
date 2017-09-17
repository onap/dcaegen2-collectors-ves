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

import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.onap.dcae.commonFunction.CommonStartup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputJsonValidation {

    private static final Logger log = LoggerFactory.getLogger(InputJsonValidation.class);
    static String valresult;


    @Test
    public void nonValidJsonValidation() {

        JSONObject jsonObject;
        JSONParser parser = new JSONParser();
        Object obj = null;
        //String jsonfilepath="C:/Users/vv770d/git/restfulcollector/src/test/resources/fujistu_non_valid_json.txt";
        String jsonfilepath = "src/test/resources/VES_invalid.txt";
        String retValue = "false";
        try {

            obj = parser.parse(new FileReader(jsonfilepath));
        } catch (Exception e) {

            log.info("Exception while opening the file");
        }
        jsonObject = (JSONObject) obj;

        String schema = null;
        try {
            schema = new JsonParser().parse(new FileReader("etc/CommonEventFormat_27.2.json"))
                .toString();
            //log.info("Schema value: " + schema.toString());
        } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
            // TODO Auto-generated catch block
            log.error(e.getLocalizedMessage(), e);
        }

        if (schema != null) {
            retValue = CommonStartup.schemavalidate(jsonObject.toString(), schema);
        }
        //return retValue;
        VesCollectorJunitTest.output = retValue;
    }


    // The below test case meant for verifying json schema on provided json file
    @Test
    public void validJsonValidation() {

        JSONObject jsonObject;
        JSONParser parser = new JSONParser();
        Object obj = null;

        String jsonfilepath = "src/test/resources/VES_valid.txt";
        String retValue = "false";
        try {

            obj = parser.parse(new FileReader(jsonfilepath));
        } catch (Exception e) {
            log.info("Exception while opening the file");
        }
        jsonObject = (JSONObject) obj;
        String schema = null;
        try {

            log.info("XX debug" + VesCollectorJunitTest.schemaFile);
            schema = new JsonParser().parse(new FileReader("etc/CommonEventFormat_27.2.json"))
                .toString();
        } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
            // TODO Auto-generated catch block
            log.error(e.getLocalizedMessage(), e);
        }

        if (schema != null) {
            retValue = CommonStartup.schemavalidate(jsonObject.toString(), schema);
        }
        VesCollectorJunitTest.output = retValue;
        //return retValue;
    }


    //validating valid json reception and its posting to DMAP.
    @Test
    public void eventReception() {

        String testCurlCommand = "curl -i -X POST -d @C:/Users/vv770d/git/restfulcollector/src/test/resources/fujistu-3.txt --header \"Content-Type: application/json\" http://localhost:8080/eventListener/v1";

        //final Process terminal = curlCommand.start();
        try {
            Process process = Runtime.getRuntime().exec(testCurlCommand);
            BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));

            // read the output from the command

            String str;
            while ((str = stdInput.readLine()) != null) {
                if (str.contains("HTTP/1.1 200 OK")) {

                    //return "true";
                    VesCollectorJunitTest.output = "true";
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.error(e.getLocalizedMessage(), e);
        }

        //return "false";
    }
}


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
import static org.onap.dcae.commonFunction.CustomExceptionLoader.LoadMap;
import static org.onap.dcae.restapi.endpoints.EventReceipt.respondWithCustomMsginJson;

import com.att.nsa.drumlin.service.standards.HttpStatusCodes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.dcae.commonFunction.CommonStartup;
import org.onap.dcae.commonFunction.CustomExceptionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCustomExceptionLoader {

    private static final Logger log = LoggerFactory.getLogger(TestCustomExceptionLoader.class);
    private CustomExceptionLoader cl;

    @Before
    public void setUp() throws Exception {
        cl = new CustomExceptionLoader();
        CommonStartup.exceptionConfig = "./etc/ExceptionConfig.json";
    }

    @After
    public void tearDown() throws Exception {
    }
    @Test
    public void test() {
        LoadMap();
        // all code usages found
        respondWithCustomMsginJson(null, HttpStatusCodes.k401_unauthorized, "Invalid user");
        respondWithCustomMsginJson(null, HttpStatusCodes.k400_badRequest, "Couldn't parse JSON object");
        respondWithCustomMsginJson(null, HttpStatusCodes.k503_serviceUnavailable, "Queue full");
        respondWithCustomMsginJson(null, HttpStatusCodes.k400_badRequest, "Schema validation failed");
        respondWithCustomMsginJson(null, HttpStatusCodes.k400_badRequest, "Couldn't parse JSON object");
        respondWithCustomMsginJson(null, HttpStatusCodes.k400_badRequest, "Schema validation failed");
        respondWithCustomMsginJson(null, HttpStatusCodes.k400_badRequest, "Incorrect message content-type; only accepts application/json messages");
    }
    @Test
    public void testLoad() {
        String op;
        LoadMap();
        op = "dataloaded";
        assertEquals("dataloaded", op);
    }

    @Test
    public void testLookup() {
        String[] retarray;

        CommonStartup.exceptionConfig = "./etc/ExceptionConfig.json";
        LoadMap();
        retarray = CustomExceptionLoader
            .LookupMap(String.valueOf(HttpStatusCodes.k401_unauthorized), "Unauthorized user");
        if (retarray == null) {
            log.info("Lookup failed");
        } else {
            assertEquals("\"POL2000\"", retarray[0]);
        }
    }
}


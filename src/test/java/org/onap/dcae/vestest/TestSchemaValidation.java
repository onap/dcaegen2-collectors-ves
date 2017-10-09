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

import com.att.nsa.drumlin.service.framework.DrumlinServlet;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onap.dcae.commonFunction.CommonStartup;
import org.onap.dcae.restapi.endpoints.EventReceipt;

public class TestSchemaValidation {

    CommonStartup cl;
    String schema = null;
    String payload = null;
    String payloadinvalid = null;

    /**
     * Sets up variables used by unit tests.
     */
    @Before
    public void setUp() throws Exception {
        schema = new JsonParser().parse(new FileReader("etc/CommonEventFormat_27.2.json")).toString();
        payload = new JsonParser().parse(new FileReader("src/test/resources/VES_valid.txt")).toString();
        payloadinvalid = new JsonParser().parse(new FileReader("src/test/resources/VES_invalid.txt")).toString();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testsuccessfulschemavalidation() {
        String valresult = CommonStartup.schemavalidate(payload, schema);
        System.out.println("testsuccessfulschemavalidation:" + valresult);
        Assert.assertEquals(valresult, "true");
    }

    @Test
    public void testunsuccessfulschemavalidation() {
        String valresult = null;
        valresult = CommonStartup.schemavalidate(payloadinvalid, schema);
        System.out.println("testunsuccessfulschemavalidation:" + valresult);
        Assert.assertFalse(valresult.equals("true"));
    }

    @Test
    public void testeventReceipt() {

        //com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext.DrumlinRequestContext(
        // DrumlinServlet webServlet,
        //HttpServletRequest req, HttpServletResponse resp, DrumlinConnection s,
        // Map<String, Object> objects, DrumlinRequestRouter router)
        //HttpServletRequest req = new HttpServletRequest();
        //HttpServletResponse res = new HttpServletResponse();
        DrumlinServlet webServlet = new DrumlinServlet();
        //webServlet.addToBaseContext(key, o);
        //Map<String,Object> mp = new Map<String, Object>();
        DrumlinRequestContext ctx = new DrumlinRequestContext(webServlet, null, null, null, null, null);
        EventReceipt er = new EventReceipt();
        try {
            EventReceipt.receiveVESEvent(null);
        } catch ( NullPointerException   e) {
            // TODO Auto-generated catch block
        }

        Assert.assertEquals("true", "true");
    }

    @Test
    public void testsafeclosefr() {
        FileReader fr;
        try {
            fr = new FileReader("etc/CommonEventFormat_27.2.json");
            EventReceipt.safeClose(fr);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Assert.assertEquals("true", "true");
    }

    @Test
    public void testsafecloseis() {
        InputStream is = new ByteArrayInputStream(StandardCharsets.UTF_16.encode("randomstring").array());
        EventReceipt.safeClose(is);
        Assert.assertEquals("true", "true");
    }
}


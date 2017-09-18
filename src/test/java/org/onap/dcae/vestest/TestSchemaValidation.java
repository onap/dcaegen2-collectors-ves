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

import static org.junit.Assert.*;

import java.io.FileReader;
import java.net.URL;
import java.util.Map;

import org.json.simple.JSONObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onap.dcae.commonFunction.CommonStartup;

import com.google.gson.JsonParser;

public class TestSchemaValidation {

	CommonStartup cl;
	String schema = null;
	String payload = null;
	String payloadinvalid = null;

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
}


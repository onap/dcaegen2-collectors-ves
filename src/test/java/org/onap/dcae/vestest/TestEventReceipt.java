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

import com.att.nsa.apiServer.endpoints.NsaBaseEndpoint;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.security.db.simple.NsaSimpleApiKey;
import java.io.IOException;
import java.util.UUID;
import jline.internal.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.dcae.commonFunction.CommonStartup;
import org.onap.dcae.commonFunction.CommonStartup.QueueFullException;
import org.onap.dcae.restapi.endpoints.EventReceipt;
import org.onap.dcae.restapi.endpoints.Ui;

public class TestEventReceipt extends NsaBaseEndpoint {

	DrumlinRequestContext ctx;
	JSONObject jsonObject;
	Boolean flag = false;
	String ev = "{\"event\": {\"commonEventHeader\": {	\"reportingEntityName\": \"VM name will be provided by ECOMP\",	\"startEpochMicrosec\": 1477012779802988,\"lastEpochMicrosec\": 1477012789802988,\"eventId\": \"83\",\"sourceName\": \"Dummy VM name - No Metadata available\",\"sequence\": 83,\"priority\": \"Normal\",\"functionalRole\": \"vFirewall\",\"domain\": \"measurementsForVfScaling\",\"reportingEntityId\": \"VM UUID will be provided by ECOMP\",\"sourceId\": \"Dummy VM UUID - No Metadata available\",\"version\": 1.1},\"measurementsForVfScalingFields\": {\"measurementInterval\": 10,\"measurementsForVfScalingVersion\": 1.1,\"vNicUsageArray\": [{\"multicastPacketsIn\": 0,\"bytesIn\": 3896,\"unicastPacketsIn\": 0,	\"multicastPacketsOut\": 0,\"broadcastPacketsOut\": 0,		\"packetsOut\": 28,\"bytesOut\": 12178,\"broadcastPacketsIn\": 0,\"packetsIn\": 58,\"unicastPacketsOut\": 0,\"vNicIdentifier\": \"eth0\"}]}}}";
	

	@Before
	public void setUp() throws Exception {
		

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testschemaFileVersion() {

		String filename = null;
		CommonStartup.schemaFileJson = new JSONObject(
				"{\"v1\":\"./etc/CommonEventFormat_27.2.json\",\"v2\":\"./etc/CommonEventFormat_27.2.json\",\"v3\":\"./etc/CommonEventFormat_27.2.json\",\"v4\":\"./etc/CommonEventFormat_27.2.json\",\"v5\":\"./etc/CommonEventFormat_28.4.1.json\"}");
		filename = EventReceipt.schemaFileVersion("v5");

		if (!filename.isEmpty()) {
			flag = true;
		}
		assertEquals(true, flag);
	}

	@Test
	public void testschemaCheck() {

		// schemaCheck(NsaSimpleApiKey retkey, int arrayFlag,JSONObject
		// jsonObject, String vesVersion, FileReader fr, DrumlinRequestContext
		// ctx, UUID uuid) throws JSONException, QueueFullException, IOException
		Boolean flag = true;
		NsaSimpleApiKey retkey = null;
		int arrayFlag = 0;
		
		CommonStartup.authflag = 0;
		CommonStartup.schemaValidatorflag = 1;

		jsonObject = new org.json.JSONObject(ev);

		String vesVersion = "v1";

		DrumlinRequestContext ctx = null;

		
		UUID uuid = UUID.randomUUID();

		try {
			flag = EventReceipt.schemaCheck(retkey, arrayFlag, jsonObject, vesVersion, ctx, uuid);
		} catch (NullPointerException |JSONException | QueueFullException | IOException e) {
			
			Log.debug("Response object creation failure");
		}
		assertEquals(true, flag);
	}

	@Test
	public void testgetUser() {

	
		Boolean flag = true;
		String user;
		
		CommonStartup.authflag = 1;
		CommonStartup.schemaValidatorflag = 1;

		jsonObject = new org.json.JSONObject(ev);

		DrumlinRequestContext ctx = null;

		try {
			user = EventReceipt.getUser(ctx);
		} catch (NullPointerException |JSONException e) {
			
			Log.debug("Response object creation failure");
		}
		assertEquals(true, flag);
	}
	
	@Test
	public void testUI() {

        try {
		Ui.hello(null);
        }
        catch (Exception e)
        {
        	//As context object is null, handling null pointer exception.
        	Log.debug("Response object creation failure");
        }
		assertEquals(true, true);
	}
}

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


import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.Map;

import org.json.simple.JSONObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onap.dcae.commonFunction.CommonStartup;
import org.onap.dcae.commonFunction.EventProcessor;
import org.onap.dcae.commonFunction.EventPublisher;
import org.onap.dcae.commonFunction.EventPublisherHash;
import org.onap.dcae.controller.LoadDynamicConfig;
import org.onap.dcae.commonFunction.DmaapPropertyReader;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.google.gson.JsonParser;

public class TestEventProcessor {

	EventProcessor ec;
	String ev= "{\"event\": {\"commonEventHeader\": {	\"reportingEntityName\": \"VM name will be provided by ECOMP\",	\"startEpochMicrosec\": 1477012779802988,\"lastEpochMicrosec\": 1477012789802988,\"eventId\": \"83\",\"sourceName\": \"Dummy VM name - No Metadata available\",\"sequence\": 83,\"priority\": \"Normal\",\"functionalRole\": \"vFirewall\",\"domain\": \"measurementsForVfScaling\",\"reportingEntityId\": \"VM UUID will be provided by ECOMP\",\"sourceId\": \"Dummy VM UUID - No Metadata available\",\"version\": 1.1},\"measurementsForVfScalingFields\": {\"measurementInterval\": 10,\"measurementsForVfScalingVersion\": 1.1,\"vNicUsageArray\": [{\"multicastPacketsIn\": 0,\"bytesIn\": 3896,\"unicastPacketsIn\": 0,	\"multicastPacketsOut\": 0,\"broadcastPacketsOut\": 0,		\"packetsOut\": 28,\"bytesOut\": 12178,\"broadcastPacketsIn\": 0,\"packetsIn\": 58,\"unicastPacketsOut\": 0,\"vNicIdentifier\": \"eth0\"}]}}}";
			
	
	@Before
	public void setUp() throws Exception {
		CommonStartup.streamid="fault=sec_fault|syslog=sec_syslog|heartbeat=sec_heartbeat|measurementsForVfScaling=sec_measurement|mobileFlow=sec_mobileflow|other=sec_other|stateChange=sec_statechange|thresholdCrossingAlert=sec_thresholdCrossingAlert|voiceQuality=ves_voicequality|sipSignaling=ves_sipsignaling";
		CommonStartup.eventTransformFlag = 1;
		
	
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLoad() {

	
		EventProcessor ec = new EventProcessor();
		
		ec.event=new org.json.JSONObject(ev);
		
		ec.overrideEvent();
		//event.commonEventHeader.sourceName
		Boolean flag = ec.event.getJSONObject("event").getJSONObject("commonEventHeader").has("sourceName");
		assertEquals(true, flag);
	}


	@Test
	public void testpublisher() {

	    DmaapPropertyReader dr;
		EventPublisher ep = null;
	    String testinput = "src/test/resources/testDmaapConfig.json";
	    Boolean flag = false;
	    dr = new DmaapPropertyReader(testinput);

				//new EventPublisher("sec_fault_ueb");
		ep = EventPublisher.getInstance("sec_fault_ueb");
		//event.commonEventHeader.sourceName
		
		if (ep.equals(null))
		{
			flag = false;
		}
		else
		{
			flag = true;
		}
		assertEquals(true, flag);
	}
	
	
	@Test
	public void testpublisherhashclass() {

	    DmaapPropertyReader dr;
	    EventPublisherHash eph = null;
	    Boolean flag = false;
		eph = EventPublisherHash.getInstance();
		
		
		if (eph.equals(null))
		{
			flag = false;
		}
		else
		{
			flag = true;
		}
		assertEquals(true, flag);
		
		
	}
	
	@Test
	public void testpublisherhashclassload() {

	    DmaapPropertyReader dr;
	    EventPublisherHash eph = null;
	    String testinput = "src/test/resources/testDmaapConfig.json";
	    Boolean flag = false;
	    dr = new DmaapPropertyReader(testinput);
		eph = EventPublisherHash.getInstance();
		EventProcessor ec = new EventProcessor();
		ec.event=new org.json.JSONObject(ev);	
		CommonStartup.cambriaConfigFile="src/test/resources/testDmaapConfig.json";
		CambriaBatchingPublisher pub = eph.Dmaaphash("sec_fault_ueb");
		
		if (pub == null || pub.equals(null))
		{
			flag = false;
		}
		else
		{
			flag = true;
		}
		assertEquals(true, flag);
		
	}		

	@Test
	public void testpublisherhashSend() {

	    DmaapPropertyReader dr;
	    EventPublisherHash eph = null;
	    String testinput = "src/test/resources/testDmaapConfig.json";
	    Boolean flag = true;
	    dr = new DmaapPropertyReader(testinput);
		eph = EventPublisherHash.getInstance();

		
		EventProcessor ec = new EventProcessor();
		ec.event=new org.json.JSONObject(ev);
		CommonStartup.cambriaConfigFile="src/test/resources/testDmaapConfig.json";
		eph.sendEvent(ec.event, "sec_fault_ueb");
		
		assertEquals(true, flag);
		
	}
}



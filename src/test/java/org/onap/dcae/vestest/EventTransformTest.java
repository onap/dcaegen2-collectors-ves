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

import java.io.FileReader;
import java.io.IOException;
import org.json.JSONObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.junit.Test;

import org.onap.dcae.commonFunction.ConfigProcessors;



public class EventTransformTest {
	public JSONObject getFileAsJsonObject()
	{
		JSONObject jsonObject = null;
		FileReader fr = null;
		final JsonParser parser = new JsonParser();
		String jsonfilepath="src/test/resources/event4xjson.txt";
		try{
			fr = new FileReader ( jsonfilepath );
			final JsonObject jo =  (JsonObject) parser.parse (fr);
			final String jsonText = jo.toString ();
			jsonObject = new JSONObject ( jsonText );
		}
		catch(Exception e){
			System.out.println("Exception while opening the file");
			e.printStackTrace();
		}
		finally {
			//close the file
			if (fr != null) {
	    		try {
	    				fr.close();
	    			} catch (IOException e) {
	    				System.out.println("Error closing file reader stream : " +e.toString());
	    			}
	    	}
		}
		return jsonObject;
	}
	@Test
	public void testAttrMap(){
		
		final JSONObject jsonObject = getFileAsJsonObject();
		final String functionRole = (jsonObject.getJSONObject("event")).getJSONObject("commonEventHeader").get("functionalRole").toString();
		System.out.println("event==" + jsonObject.toString());
		System.out.println("functionRole==" + functionRole);
		final JSONObject jsonArgs = new JSONObject ( "{\"field\": \"event.commonEventHeader.nfNamingCode\",\"oldField\": \"event.commonEventHeader.functionalRole\"}" );
		ConfigProcessors cpEvent = new ConfigProcessors(jsonObject);
	 	cpEvent.map(jsonArgs);
	 	final String responseData = cpEvent.getEventObjectVal("event.commonEventHeader.nfNamingCode").toString();
	 	System.out.println("modified event==" + jsonObject.toString());
	 	System.out.println("responseData==" + responseData); 
	 	assertEquals (functionRole, responseData); 
	}
	
	@Test
	public void testArrayMap(){
		
		final JSONObject jsonObject = getFileAsJsonObject();
		final String alarmAdditionalInformation = (jsonObject.getJSONObject("event")).getJSONObject("faultFields").get("alarmAdditionalInformation").toString();
		System.out.println("event==" + jsonObject.toString());
		System.out.println("alarmAdditionalInformation==" + alarmAdditionalInformation);
		final JSONObject jsonArgs = new JSONObject ( "{\"field\": \"event.faultFields.eventAdditionalInformation\",\"oldField\": \"event.faultFields.alarmAdditionalInformation\"}" );
		ConfigProcessors cpEvent = new ConfigProcessors(jsonObject);
	 	cpEvent.map(jsonArgs);
	 	final String responseData = cpEvent.getEventObjectVal("event.faultFields.eventAdditionalInformation").toString();
	 	System.out.println("modified event==" + jsonObject.toString());
	 	System.out.println("responseData==" + responseData); 
	 	assertEquals (alarmAdditionalInformation, responseData); 
	}
	@Test
	public void testJobjMaptoArray(){
		
		final JSONObject jsonObject = getFileAsJsonObject();
		//final String receiveDiscards = (((jsonObject.getJSONObject("event")).getJSONObject("faultFields")).get("errors")).get("receiveDiscards").toString();
		System.out.println("event==" + jsonObject.toString());
		//System.out.println("alarmAdditionalInformation==" + alarmAdditionalInformation);
		final JSONObject jsonArgs = new JSONObject ( "{\"field\": \"event.faultFields.vNicPerformanceArray[]\",\"oldField\": \"event.faultFields.errors\",\"attrMap\":{\"receiveDiscards\":\"receivedDiscardedPacketsAccumulated\"}}" );
		ConfigProcessors cpEvent = new ConfigProcessors(jsonObject);
		final String receiveDiscards = cpEvent.getEventObjectVal("event.faultFields.errors.receiveDiscards").toString();
		System.out.println("receiveDiscards==" + receiveDiscards);
	 	cpEvent.map(jsonArgs);
	 	final String responseData = cpEvent.getEventObjectVal("event.faultFields.vNicPerformanceArray[0].receivedDiscardedPacketsAccumulated").toString();
	 	System.out.println("modified event==" + jsonObject.toString());
	 	System.out.println("responseData==" + responseData); 
	 	assertEquals (receiveDiscards, responseData); 
	}
	@Test
	public void testAttrAdd(){
		
		final JSONObject jsonObject = getFileAsJsonObject();
		//final String functionRole = (jsonObject.getJSONObject("event")).getJSONObject("commonEventHeader").get("functionalRole").toString();
		System.out.println("event==" + jsonObject.toString());
		//System.out.println("functionRole==" + functionRole);
		final JSONObject jsonArgs = new JSONObject ( "{\"field\": \"event.faultFields.version\",\"value\": \"2.0\",\"fieldType\": \"number\"}" );
		ConfigProcessors cpEvent = new ConfigProcessors(jsonObject);
	 	cpEvent.addAttribute(jsonArgs);
	 	final String responseData = cpEvent.getEventObjectVal("event.faultFields.version").toString();
	 	System.out.println("modified event==" + jsonObject.toString());
	 	System.out.println("responseData==" + responseData); 
	 	assertEquals ("2.0", responseData); 
	}
	
	@Test
	public void testAttrUpdate(){
		
		final JSONObject jsonObject = getFileAsJsonObject();
		//final String functionRole = (jsonObject.getJSONObject("event")).getJSONObject("commonEventHeader").get("functionalRole").toString();
		System.out.println("event==" + jsonObject.toString());
		//System.out.println("functionRole==" + functionRole);
		final JSONObject jsonArgs = new JSONObject ( "{\"field\": \"event.faultFields.version\",\"value\": \"2.0\",\"fieldType\": \"number\"}" );
		ConfigProcessors cpEvent = new ConfigProcessors(jsonObject);
	 	cpEvent.updateAttribute(jsonArgs);
	 	final String responseData = cpEvent.getEventObjectVal("event.faultFields.version").toString();
	 	System.out.println("modified event==" + jsonObject.toString());
	 	System.out.println("responseData==" + responseData); 
	 	assertEquals ("2.0", responseData); 
	}
	
	@Test
	public void testAttrConcatenate(){
		
		final JSONObject jsonObject = getFileAsJsonObject();
		final String eventType = (jsonObject.getJSONObject("event")).getJSONObject("commonEventHeader").get("eventType").toString();
		final String domain = (jsonObject.getJSONObject("event")).getJSONObject("commonEventHeader").get("domain").toString();
		final String alarmCondition = (jsonObject.getJSONObject("event")).getJSONObject("faultFields").get("alarmCondition").toString();
		System.out.println("event==" + jsonObject.toString());
		final String eventName = domain + "_" + eventType + "_" + alarmCondition;
		System.out.println("eventName==" + eventName);
		final JSONObject jsonArgs = new JSONObject ( "{\"field\":\"event.commonEventHeader.eventName\",\"concatenate\": [\"$event.commonEventHeader.domain\",\"$event.commonEventHeader.eventType\",\"$event.faultFields.alarmCondition\"],\"delimiter\":\"_\"}");
		ConfigProcessors cpEvent = new ConfigProcessors(jsonObject);
	 	cpEvent.concatenateValue(jsonArgs);
	 	final String responseData = cpEvent.getEventObjectVal("event.commonEventHeader.eventName").toString();
	 	System.out.println("modified event==" + jsonObject.toString());
	 	System.out.println("responseData==" + responseData); 
	 	assertEquals (eventName, responseData); 
	}
	
	@Test
	public void testAttrSubtract(){
		
		final JSONObject jsonObject = getFileAsJsonObject();
		final String memoryConfigured = (jsonObject.getJSONObject("event")).getJSONObject("faultFields").get("memoryConfigured").toString();
		final String memoryUsed = (jsonObject.getJSONObject("event")).getJSONObject("faultFields").get("memoryUsed").toString();
		System.out.println("event==" + jsonObject.toString());
		System.out.println("memoryConfigured==" + memoryConfigured);
		System.out.println("memoryUsed==" + memoryUsed);
		final JSONObject jsonArgs = new JSONObject ( "{\"field\": \"event.faultFields.memoryFree\",\"subtract\": [\"$event.faultFields.memoryConfigured\",\"$event.faultFields.memoryUsed\"]}" );
		ConfigProcessors cpEvent = new ConfigProcessors(jsonObject);
	 	cpEvent.subtractValue(jsonArgs);
	 	final String responseData = cpEvent.getEventObjectVal("event.faultFields.memoryFree").toString();
	 	System.out.println("modified event==" + jsonObject.toString());
	 	System.out.println("responseData==" + responseData); 
	 	assertEquals ("1980.0", responseData); 
	}
}  


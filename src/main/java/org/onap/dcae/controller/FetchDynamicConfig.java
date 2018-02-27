/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.dcae.controller;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onap.dcae.commonFunction.CommonStartup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class FetchDynamicConfig {

	private static final Logger log = LoggerFactory.getLogger(FetchDynamicConfig.class);

	public static String configFile = "/opt/app/KV-Configuration.json";
	static String url;
	public static String retString;
	public static String retCBSString;
	public static Map<String, String> env;

	public FetchDynamicConfig() {
	}

	public static void main(String[] args) {
		Boolean areEqual = false;
		// Call consul api and identify the CBS Service address and port
		getconsul();
		// Construct and invoke CBS API to get application Configuration
		getCBS();
		// Verify if data has changed
		areEqual = verifyConfigChange();
		// If new config then write data returned into configFile for
		// LoadDynamicConfig process
		if (! areEqual) {
			FetchDynamicConfig fc = new FetchDynamicConfig();
			fc.writefile(retCBSString);
		} else {
			log.info("New config pull results identical -  " + configFile + " NOT refreshed");
		}
	}

	public static void getconsul() {

		env = System.getenv();
		for (Map.Entry<String, String> entry : env.entrySet()) {
			log.info(entry.getKey() + ":" + entry.getValue());
		}

		if (env.containsKey("CONSUL_HOST") && env.containsKey("CONFIG_BINDING_SERVICE")) {
			// && env.containsKey("HOSTNAME")) {
			log.info(">>>Dynamic configuration to be fetched from ConfigBindingService");
			url = env.get("CONSUL_HOST") + ":8500/v1/catalog/service/" + env.get("CONFIG_BINDING_SERVICE");

			retString = executecurl(url);

		} else {
			log.info(">>>Static configuration to be used");
		}

	}

	public static boolean verifyConfigChange() {

		boolean areEqual = false;
		// Read current data
		try {
			File f = new File(configFile);
			if (f.exists() && !f.isDirectory()) {

				String jsonData = LoadDynamicConfig.readFile(configFile);
				JSONObject jsonObject = new JSONObject(jsonData);

				ObjectMapper mapper = new ObjectMapper();

				JsonNode tree1 = mapper.readTree(jsonObject.toString());
				JsonNode tree2 = mapper.readTree(retCBSString.toString());
				areEqual = tree1.equals(tree2);
				log.info("Comparison value:" + areEqual);
			} else {
				log.info("First time config file read: " + configFile);
				// To allow first time file creation
				areEqual = false;
			}

		} catch (IOException e) {
			log.error("Comparison with new fetched data failed" + e.getMessage());

		}

		return areEqual;

	}

	public static void getCBS() {

		env = System.getenv();
		// consul return as array
		JSONTokener temp = new JSONTokener(retString);
		JSONObject cbsjobj = (JSONObject) new JSONArray(temp).get(0);

		String urlPart1 = null;
		if (cbsjobj.has("ServiceAddress") && cbsjobj.has("ServicePort")) {
			urlPart1 = cbsjobj.getString("ServiceAddress") + ":" + cbsjobj.getInt("ServicePort");
		}

		log.info("CONFIG_BINDING_SERVICE DNS RESOLVED:" + urlPart1);

		if (env.containsKey("HOSTNAME")) {
			url = urlPart1 + "/service_component/" + env.get("HOSTNAME");
			retCBSString = executecurl(url);
		} else if (env.containsKey("SERVICE_NAME")) {
			url = urlPart1 + "/service_component/" + env.get("SERVICE_NAME");
			retCBSString = executecurl(url);
		} else {
			log.error("Service name environment variable - HOSTNAME/SERVICE_NAME not found within container ");
		}

	}

	public void writefile(String retCBSString) {
		log.info("URL to fetch configuration:" + url + " Return String:" + retCBSString);

		String indentedretstring = (new JSONObject(retCBSString)).toString(4);

		try (FileWriter file = new FileWriter(FetchDynamicConfig.configFile)) {
			file.write(indentedretstring);

			log.info("Successfully Copied JSON Object to file " + configFile);
		} catch (IOException e) {
			log.error("Error in writing configuration into file " + configFile + retString + e.getMessage());
			e.printStackTrace();
		}

	}

	public static String executecurl(String url) {

		String[] command = { "curl", "-v", url };
		ProcessBuilder process = new ProcessBuilder(command);
		Process p;
		String result = null;
		try {
			p = process.start();
			InputStreamReader ipr = new InputStreamReader(p.getInputStream());
			BufferedReader reader = new BufferedReader(ipr);
			StringBuilder builder = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			result = builder.toString();
			log.info(result);

			reader.close();
			ipr.close();
		} catch (IOException e) {
			log.error("error", e);
			e.printStackTrace();
		}
		return result;

	}

}

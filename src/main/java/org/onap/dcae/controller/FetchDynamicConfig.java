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

package org.onap.dcae.controller;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class FetchDynamicConfig {

	static String configFile = "/opt/app/KV-Configuration.json";
	static String url = null;
	static String ret_string = null;

	public FetchDynamicConfig() {

	}

	public static void main(String[] args) {
		Map<String, String> env = System.getenv();
		for (String envName : env.keySet()) {
			System.out.format("%s=%s%n", envName, env.get(envName));
		}

		if (env.containsKey("CONSUL_HOST") && env.containsKey("CONFIG_BINDING_SERVICE")
				 && env.containsKey("HOSTNAME")) {
			System.out.println(">>>Dynamic configuration to be fetched from ConfigBindingService");
			url = env.get("CONSUL_HOST") + ":8500/v1/catalog/service/" + env.get("CONFIG_BINDING_SERVICE");

			ret_string = executecurl(url);
			// consul return as array
			JSONTokener temp = new JSONTokener(ret_string);
			JSONObject cbsjobj = (JSONObject) new JSONArray(temp).get(0);

			String url_part1 = null;
			if (cbsjobj.has("ServiceAddress") && cbsjobj.has("ServicePort")) {
				url_part1 = cbsjobj.getString("ServiceAddress") + ":" + cbsjobj.getInt("ServicePort");
			}

			System.out.println("CONFIG_BINDING_SERVICE DNS RESOLVED:" + url_part1);
			url = url_part1 + "/service_component/" + env.get("HOSTNAME");
			ret_string = executecurl(url);

			JSONObject jsonObject = new JSONObject(new JSONTokener(ret_string));
			try (FileWriter file = new FileWriter(configFile)) {
				file.write(jsonObject.toString());

				System.out.println("Successfully Copied JSON Object to file /opt/app/KV-Configuration.json");
			} catch (IOException e) {
				System.out.println(
						"Error in writing configuration into file /opt/app/KV-Configuration.json " + jsonObject);
				e.printStackTrace();
			}
		}

		else {
			System.out.println(">>>Static configuration to be used");
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
			String line = null;

			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			result = builder.toString();
			System.out.println(result);

		} catch (IOException e) {
			System.out.print("error");
			e.printStackTrace();
		}
		return result;

	}

}

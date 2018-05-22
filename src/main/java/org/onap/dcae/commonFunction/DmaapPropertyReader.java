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

package org.onap.dcae.commonFunction;

import java.io.FileNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class DmaapPropertyReader {


	private static final Logger log = LoggerFactory.getLogger(DmaapPropertyReader.class);
	private static final String CAMBRIA_TOPIC_KEY = "cambria.topic";
	private static final String CAMBRIA_HOSTS_KEY = "cambria.hosts";
	private static final String CAMBRIA_URL_KEY = "cambria.url";
	private static final String[] LEGACY_CHANNEL_PARAM_NAMES = {CAMBRIA_TOPIC_KEY, CAMBRIA_HOSTS_KEY, CAMBRIA_URL_KEY, "basicAuthPassword", "basicAuthUsername"};
	private static final String PROPERTY_KEY_NAME_SEPARATOR = ".";
	private static DmaapPropertyReader instance = null;
	private final Map<String, String> dmaapProperties;

	public DmaapPropertyReader(String cambriaConfigFilePath) {
		this.dmaapProperties = DmaapPropertyReader.getProcessedDmaapProperties(cambriaConfigFilePath);
	}


	private static Map<String, String> getProcessedDmaapProperties(String configFilePath){
		Map<String, String> transformedDmaapProperties = new HashMap<>();
		try(FileReader fr = new FileReader(configFilePath)) {
			JsonElement root = new JsonParser().parse(fr);

			// Check if dmaap config is handled by legacy controller/service/manager
			JsonElement channelRoot = root.getAsJsonObject().get("channels");
			if (channelRoot != null) {
				transformedDmaapProperties = fillDmaapHashWithLegacyChannelData((JsonArray) channelRoot);
			} else {
				// Handing new format from controllergen2/config_binding_service
				transformedDmaapProperties = fillDmaapHashInNewFormat(root.getAsJsonObject());
			}

		} catch (JsonIOException | JsonSyntaxException |  FileNotFoundException e1) {
			log.error("Problem loading Dmaap configuration file (located under path: )"+configFilePath+ ") : " + e1.toString());
			e1.printStackTrace();
		} catch (IOException e) {
			log.error("Cannot read Dmaap configuration file (located under path: )"+configFilePath+ ") : " + e.toString());
			e.printStackTrace();
		}
		return transformedDmaapProperties;
	}

	private static Map<String, String> fillDmaapHashWithLegacyChannelData(JsonArray channelRoot){
		Map<String, String> dmaapProps = new HashMap<>();

		for (int i = 0; i < channelRoot.size(); i++) {
			JsonObject jsonElement = channelRoot.get(i).getAsJsonObject();
			String channelNamePrefix = jsonElement.get("name").toString().replace("\"", "");
			for(String paramName: LEGACY_CHANNEL_PARAM_NAMES){
				addElementIfPresent(dmaapProps, jsonElement, paramName, channelNamePrefix+ PROPERTY_KEY_NAME_SEPARATOR +paramName);
			}
		}
		return dmaapProps;
	}

	private static Map<String, String> fillDmaapHashInNewFormat(JsonObject root) {
		Map<String, String> dmaapProps = new HashMap<>();

		for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
			String entryKey = entry.getKey();
			JsonObject entryValue = entry.getValue().getAsJsonObject();
			JsonElement topicUrl = entryValue.get("dmaap_info").getAsJsonObject().get("topic_url");
			String[] urlParts = dmaapUrlSplit(topicUrl.toString().replace("\"", ""));

			String mrTopic = null;
			String mrUrl = null;
			String[] hostPort = null;

			try {
				if (null != urlParts) {
					mrUrl = urlParts[2];
					// DCAE internal dmaap topic convention
					if (urlParts[3].equals("events")) {
						mrTopic = urlParts[4];
					} else {
						// ONAP dmaap topic convention
						mrTopic = urlParts[3];
						hostPort = mrUrl.split(":");
					}
				}
			} catch (NullPointerException e) {
				log.error("Exception during parsing topic_url and topic convention- expected number of url parts: 4"+" but found only: "+urlParts.length);
				e.getMessage();
			}

			dmaapProps.put(entryKey+"."+CAMBRIA_TOPIC_KEY, mrTopic);
			dmaapProps.put(entryKey+"."+ CAMBRIA_URL_KEY, mrUrl);
			addElementIfPresent(dmaapProps, entryValue, "aaf_username", entryKey+".basicAuthUsername");
			addElementIfPresent(dmaapProps, entryValue, "aaf_password", entryKey+".basicAuthPassword");

			if(hostPort != null){
				dmaapProps.put(entryKey+"."+ CAMBRIA_HOSTS_KEY, hostPort[0]);
				log.debug("Initializing dmaapProperties for DmaapPropertyReader. Provided data: TOPIC:" + mrTopic + " HOST-URL:" + mrUrl + " HOSTS:" + hostPort[0] +" NAME:" + entryKey);
			}
			else {
				log.debug("Initializing dmaapProperties for DmaapPropertyReader (no host specified). Provided data:  TOPIC:" + mrTopic + " HOST-URL:" + mrUrl);
			}
		}
		return dmaapProps;
	}

	private static void addElementIfPresent(Map<String, String> dmaapProperties, JsonObject highLevelJsonObject, String inputParamName, String newKey){
		if(highLevelJsonObject.has(inputParamName)) {
			dmaapProperties.put(newKey, highLevelJsonObject.get(inputParamName).toString().replace("\"", ""));
		}
	}



	public Map<String, String> getDmaapProperties() {
		return dmaapProperties;
	}

		/***
         * Dmaap url structure pub - https://<dmaaphostname>:<port>/events/
         * <namespace>.<dmaapcluster>.<topic>, sub - https://<dmaaphostname>:
         * <port>/events/<namespace>.<dmaapcluster>.<topic>/G1/u1";
         *
         * Onap url structure pub - http://<dmaaphostname>:<port>/<unauthenticated>.
         * <topic>,
         */

	private static String[] dmaapUrlSplit(String dmUrl) {
		String[] multUrls = dmUrl.split(",");

		StringBuffer newUrls = new StringBuffer();
		String[] urlParts = null;
		for (int i = 0; i < multUrls.length; i++) {
			urlParts = multUrls[i].split("/");
			if (i == 0) {
				newUrls = newUrls.append(urlParts[2]);
			} else {
				newUrls = newUrls.append(",").append(urlParts[2]);
			}
		}
		return urlParts;
	}

	public static synchronized DmaapPropertyReader getInstance(String channelConfig) {
		if (instance == null) {
			instance = new DmaapPropertyReader(channelConfig);
		}
		return instance;
	}

	public String getKeyValue(String hashKey) {
		return dmaapProperties.get(hashKey);
	}

}



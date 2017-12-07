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
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class DmaapPropertyReader {

	private static DmaapPropertyReader instance = null;

	private static final Logger log = LoggerFactory.getLogger(DmaapPropertyReader.class);

	public HashMap<String, String> dmaap_hash = new HashMap<String, String>();

	public DmaapPropertyReader(String CambriaConfigFile) {

		FileReader fr = null;
		try {
			JsonElement root = null;
			fr = new FileReader(CambriaConfigFile);
			root = new JsonParser().parse(fr);

			// Check if dmaap config is handled by legacy controller/service
			// manager
			if (root.getAsJsonObject().has("channels")) {
				JsonArray jsonObject = (JsonArray) root.getAsJsonObject().get("channels");

				for (int i = 0; i < jsonObject.size(); i++) {
					log.debug("TOPIC:" + jsonObject.get(i).getAsJsonObject().get("cambria.topic") + " HOST-URL:"
							+ jsonObject.get(i).getAsJsonObject().get("cambria.url") + " HOSTS:"
							+ jsonObject.get(i).getAsJsonObject().get("cambria.hosts") + " PWD:"
							+ jsonObject.get(i).getAsJsonObject().get("basicAuthPassword") + " USER:"
							+ jsonObject.get(i).getAsJsonObject().get("basicAuthUsername") + " NAME:"
							+ jsonObject.get(i).getAsJsonObject().get("name"));

					String convertedname = jsonObject.get(i).getAsJsonObject().get("name").toString().replace("\"", "");
					dmaap_hash.put(convertedname + ".cambria.topic",
							jsonObject.get(i).getAsJsonObject().get("cambria.topic").toString().replace("\"", ""));

					if (jsonObject.get(i).getAsJsonObject().get("cambria.hosts") != null) {
						dmaap_hash.put(convertedname + ".cambria.hosts",
								jsonObject.get(i).getAsJsonObject().get("cambria.hosts").toString().replace("\"", ""));
					}
					if (jsonObject.get(i).getAsJsonObject().get("cambria.url") != null) {
						dmaap_hash.put(convertedname + ".cambria.url",
								jsonObject.get(i).getAsJsonObject().get("cambria.url").toString().replace("\"", ""));
					}
					if (jsonObject.get(i).getAsJsonObject().get("basicAuthPassword") != null) {
						dmaap_hash.put(convertedname + ".basicAuthPassword", jsonObject.get(i).getAsJsonObject()
								.get("basicAuthPassword").toString().replace("\"", ""));
					}
					if (jsonObject.get(i).getAsJsonObject().get("basicAuthUsername") != null) {
						dmaap_hash.put(convertedname + ".basicAuthUsername", jsonObject.get(i).getAsJsonObject()
								.get("basicAuthUsername").toString().replace("\"", ""));
					}

				}
			} else {

				// Handing new format from controllergen2/config_binding_service
				JsonObject jsonObject = root.getAsJsonObject();
				Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();

				for (Map.Entry<String, JsonElement> entry : entries) {

					JsonElement topicurl = entry.getValue().getAsJsonObject().get("dmaap_info").getAsJsonObject()
							.get("topic_url");
					String[] urlParts = dmaapUrlSplit(topicurl.toString().replace("\"", ""));

					String mrTopic = null;
					String mrUrl = null;
					String[] hostport = null;
					String username = null;
					String userpwd = null;

					try {

						if (null != urlParts) {
							mrUrl = urlParts[2];

							// DCAE internal dmaap topic convention
							if (urlParts[3].equals("events")) {
								mrTopic = urlParts[4];
							} else {
								// ONAP dmaap topic convention
								mrTopic = urlParts[3];
								hostport = mrUrl.split(":");
							}

						}
					} catch (NullPointerException e) {
						System.out.println("NullPointerException");
						e.getMessage();
					}

					if (entry.getValue().getAsJsonObject().has("aaf_username")) {
						username = entry.getValue().getAsJsonObject().get("aaf_username").toString().replace("\"", "");
					}
					if (entry.getValue().getAsJsonObject().has("aaf_password")) {
						userpwd = entry.getValue().getAsJsonObject().get("aaf_password").toString().replace("\"", "");
					}
					if (hostport == null) {
						log.debug("TOPIC:" + mrTopic + " HOST-URL:" + mrUrl + " PWD:" + userpwd + " USER:" + username);
					} else {
						log.debug("TOPIC:" + mrTopic + " HOST-URL:" + mrUrl + " HOSTS:" + hostport[0] + " PWD:"
								+ userpwd + " USER:" + username + " NAME:" + entry.getKey());
					}

					dmaap_hash.put(entry.getKey() + ".cambria.topic", mrTopic);

					if (!(hostport == null)) {
						dmaap_hash.put(entry.getKey() + ".cambria.hosts", hostport[0]);
					}

					if (!(mrUrl == null)) {
						dmaap_hash.put(entry.getKey() + ".cambria.url", mrUrl);
					}

					if (!(username == null)) {
						dmaap_hash.put(entry.getKey() + ".basicAuthUsername", username);
					}

					if (!(userpwd == null)) {
						dmaap_hash.put(entry.getKey() + ".basicAuthPassword", userpwd);
					}

				}

			}

		} catch (JsonIOException | JsonSyntaxException |

				FileNotFoundException e1) {
			e1.printStackTrace();
			log.error("Problem loading Dmaap Channel configuration file: " + e1.toString());
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e) {
					log.error("Error closing file reader stream : " + e.toString());
				}
			}
		}

	}

	/***
	 * Dmaap url structure pub - https://<dmaaphostname>:<port>/events/
	 * <namespace>.<dmaapcluster>.<topic>, sub - https://<dmaaphostname>:
	 * <port>/events/<namespace>.<dmaapcluster>.<topic>/G1/u1";
	 *
	 * Onap url structure pub - http://<dmaaphostname>:<port>/<unauthenticated>.
	 * <topic>,
	 */

	private String[] dmaapUrlSplit(String dmUrl) {
		String[] multUrls = dmUrl.split(",");

		StringBuffer newUrls = new StringBuffer();
		String urlParts[] = null;
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

	public static synchronized DmaapPropertyReader getInstance(String ChannelConfig) {
		if (instance == null) {
			instance = new DmaapPropertyReader(ChannelConfig);
		}
		return instance;
	}

	public String getKeyValue(String HashKey) {
		return this.dmaap_hash.get(HashKey);
	}
}

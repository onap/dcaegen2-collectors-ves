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

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DmaapPropertyReader {

	private static DmaapPropertyReader instance = null;

	private static final Logger log = LoggerFactory.getLogger(DmaapPropertyReader.class);

	private HashMap<String, DMaaPChannel> channelMap = new HashMap<>();

	private static final String DMAAP_INFO = "dmaap_info";
	private static final String TOPIC_URL = "topic_url";
	private static final String AAF_USERNAME = "aaf_username";
	private static final String AAF_PASSWORD = "aaf_password";
	private static final Pattern URL_PATTERN = Pattern.compile("http(?:s)?://(([^/]+):[^/]+)/(?:events/)?([^/]+)(?:/[^,]+)?");

	/**
	 * Tries to read DMaaP channel configuration from the provided file.
	 * <p>DMaaP URL structure:
	 * <ul>
	 * <li>pub - https://&lt;dmaaphostname&gt;:&lt;port&gt;/events/&lt;namespace&gt;.&lt;dmaapcluster&gt;.&lt;topic&gt;</li>
	 * <li>sub - https://&lt;dmaaphostname&gt;:&lt;port&gt;/events/&lt;namespace&gt;.&lt;dmaapcluster&gt;.&lt;topic&gt;/G1/u1</li>
	 * </ul>
	 * <p>ONAP URL structure:
	 * <ul>
	 * <li>pub - http://&lt;dmaaphostname&gt;:&lt;port&gt;/&lt;unauthenticated&gt;.&lt;topic&gt;</li>
	 * </ul>
	 * @param cambriaConfigFile The file from which DMaaP config will be read
	 */
	public DmaapPropertyReader(String cambriaConfigFile) {

		try {
			ObjectMapper mapper = new ObjectMapper();
			DMaaPChannels channels = mapper.readValue(new File(cambriaConfigFile), DMaaPChannels.class);
			if (channels.getChannels() != null) {
				log.info("DMaaP config is handled by legacy controller/service manager");
				for (DMaaPChannel channel : channels.getChannels()) {
					channelMap.put(channel.getName(), channel);
				}
			} else {
				log.info("DMaaP config is handled using controllergen2/config_binding_service");
				JsonNode tree = mapper.readTree(new File(cambriaConfigFile));
				Iterator<Map.Entry<String, JsonNode>> entries = tree.fields();
				while (entries.hasNext()) {
					Map.Entry<String, JsonNode> entry = entries.next();
					JsonNode value = entry.getValue();
					if (!value.has(DMAAP_INFO) || !value.get(DMAAP_INFO).has(TOPIC_URL)) {
						continue;
					}
					String topicurl = value.get(DMAAP_INFO).get(TOPIC_URL).asText();
					Matcher matcher = URL_PATTERN.matcher(topicurl);
					if (matcher.matches()) {
						// TODO: Read multiple URLs
						DMaaPChannel.Builder cBuilder = DMaaPChannel.builder();
						cBuilder.name(entry.getKey());
						cBuilder.cambriaUrl(matcher.group(1));
						cBuilder.cambriaHosts(matcher.group(2));
						cBuilder.cambriaTopic(matcher.group(3));
						if (value.get(DMAAP_INFO).has(AAF_USERNAME)) {
							cBuilder.basicAuthUsername(value.get(DMAAP_INFO).get(AAF_USERNAME).asText());
						}
						if (value.get(DMAAP_INFO).has(AAF_PASSWORD)) {
							cBuilder.basicAuthPassword(value.get(DMAAP_INFO).get(AAF_PASSWORD).asText());
						}
						channelMap.put(entry.getKey(), cBuilder.build());
					} else {
						log.error("Could not read DMaaP channel info from controllergen2/config_binding_service");
					}
				}
			}
			log.info("Read DMaaP channel map from {}: {}", cambriaConfigFile, channelMap);
		} catch (IOException e) {
			log.error("Failed to read DMaaP channel map from {}: {}", cambriaConfigFile, e);
		}
	}

	public static synchronized DmaapPropertyReader getInstance(String ChannelConfig) {
		if (instance == null) {
			instance = new DmaapPropertyReader(ChannelConfig);
		}
		return instance;
	}

	public DMaaPChannel getChannel(String name) {
		return channelMap.get(name);
	}
}

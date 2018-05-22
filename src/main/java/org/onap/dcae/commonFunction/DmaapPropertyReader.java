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

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    private static Map<String, String> getProcessedDmaapProperties(String configFilePath) {
        Map<String, String> transformedDmaapProperties = new HashMap<>();
        try (FileReader fr = new FileReader(configFilePath)) {
            AnyNode root = AnyNode.parse(fr);
            Optional<AnyNode> channelsJson = root.getAsOptional("channels");
            // Check if dmaap config is handled by legacy controller/service/manager ("channels" key present) if not, then use handing new format from controllergen2/config_binding_service
            transformedDmaapProperties = channelsJson.map(DmaapPropertyReader::fillDmaapHashWithLegacyChannelData).orElseGet(() -> fillDmaapHashWithInfoData(root));
        } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e1) {
            log.error("Problem loading Dmaap configuration file (located under path: {}): {}", configFilePath, e1.toString());
            e1.printStackTrace();
        } catch (IOException e) {
            log.error("Cannot read Dmaap configuration file (located under path: {}): {} ", configFilePath, e.toString());
            e.printStackTrace();
        }
        return transformedDmaapProperties;
    }

    private static Map<String, String> fillDmaapHashWithLegacyChannelData(AnyNode channels) {
        Map<String, String> dmaapProps = new HashMap<>();

        for (int i = 0; i < channels.asJsonArray().length(); i++) {
            AnyNode channelData = channels.get(i);
            String channelNamePrefix = channelData.get("name").asString().replace("\"", "");
            for (String paramName : LEGACY_CHANNEL_PARAM_NAMES) {
                addToMapIfKeyPresent(paramName, channelData, channelNamePrefix + PROPERTY_KEY_NAME_SEPARATOR + paramName, dmaapProps);
            }
        }
        return dmaapProps;
    }

    private static void addToMapIfKeyPresent(String paramName, AnyNode rootNode, String newKey, Map<String, String> resultMap) {
        rootNode.getAsOptional(paramName).ifPresent(val -> {
            String value = val.asString();
            resultMap.put(newKey, value.replace("\"", ""));
        });
    }

    private static Map<String, String> fillDmaapHashWithInfoData(AnyNode root) {
        Map<String, String> dmaapProps = new HashMap<>();

        for (Map.Entry<String, AnyNode> entry : root.asMap().entrySet()) {
            String entryKey = entry.getKey();
            AnyNode entryValue = entry.getValue();
            Map<String, AnyNode> dmaapInfoMap = entryValue.get("dmaap_info").asMap();
            String topicUrl = dmaapInfoMap.get("topic_url").asString();
            String[] urlParts = dmaapUrlSplit(topicUrl.replace("\"", ""));

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
                log.error("Exception during parsing topic_url and topic convention- expected number of url parts: 4, but found only {} ", urlParts.length);
                e.getMessage();
            }

            dmaapProps.put(entryKey + "." + CAMBRIA_TOPIC_KEY, mrTopic);
            dmaapProps.put(entryKey + "." + CAMBRIA_URL_KEY, mrUrl);
            addToMapIfKeyPresent("aaf_username", entryValue, entryKey + ".basicAuthUsername", dmaapProps);
            addToMapIfKeyPresent("aaf_password", entryValue, entryKey + ".basicAuthPassword", dmaapProps);
            if (hostPort != null) {
                dmaapProps.put(entryKey + "." + CAMBRIA_HOSTS_KEY, hostPort[0]);
                log.debug("Initializing dmaapProperties for DmaapPropertyReader. Provided data: TOPIC: {} HOST-URL: {} HOSTS: {} NAME: {}", mrTopic, mrUrl, hostPort[0], entryKey);
            } else {
                log.debug("Initializing dmaapProperties for DmaapPropertyReader (no host specified). Provided data:  TOPIC: {} HOST-URL: {}", mrTopic, mrUrl);
            }
        }
        return dmaapProps;
    }

    public Map<String, String> getDmaapProperties() {
        return dmaapProperties;
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
}
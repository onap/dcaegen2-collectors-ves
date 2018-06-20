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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DmaapPropertyReader {


    private static final Logger log = LoggerFactory.getLogger(DmaapPropertyReader.class);
    private static final String CAMBRIA_TOPIC_KEY = "cambria.topic";
    private static final String CAMBRIA_HOSTS_KEY = "cambria.hosts";
    private static final String CAMBRIA_URL_KEY = "cambria.url";
    private static final List<String> LEGACY_CHANNEL_MANDATORY_PARAMS = Lists.newArrayList(CAMBRIA_TOPIC_KEY, CAMBRIA_HOSTS_KEY, CAMBRIA_URL_KEY, "basicAuthPassword", "basicAuthUsername");
    private static DmaapPropertyReader instance = null;
    private final Map<String, String> dmaapProperties;

    public DmaapPropertyReader(String cambriaConfigFilePath) {
        this.dmaapProperties = DmaapPropertyReader.getProcessedDmaapProperties(cambriaConfigFilePath);
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

    private static Map<String, String> getProcessedDmaapProperties(String configFilePath) {
        Map<String, String> transformedDmaapProperties = new HashMap<>();
        try {
            AnyNode root = AnyNode.parse(configFilePath);
            if (root.hasKey("channels")) { // Check if dmaap config is handled by legacy controller/service/manager
                transformedDmaapProperties = getLegacyDmaapPropertiesWithChannels(root.get("channels"));
            } else {//Handing new format from controllergen2/config_binding_service
                transformedDmaapProperties = getDmaapPropertiesWithInfoData(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return transformedDmaapProperties;
    }

    private static Map<String, String> getLegacyDmaapPropertiesWithChannels(AnyNode channelsNode) {
        return channelsNode.asList().stream()
                .map(DmaapPropertyReader::getTransformedMandatoryChannelProperties)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<String, String> getTransformedMandatoryChannelProperties(AnyNode channel) {
        String prefix = channel.get("name").asString() + ".";
        return channel.asMap().entrySet().stream().filter(el -> LEGACY_CHANNEL_MANDATORY_PARAMS.contains(el.getKey()) && !Objects.equals(el.getKey(), "name"))
                .collect(Collectors.toMap(k -> prefix + k.getKey(), v -> v.getValue().asString().replace("\"", "")));
    }

    private static Map<String, String> getDmaapPropertiesWithInfoData(AnyNode root) {
        return root.asMap().entrySet().stream()
                .map(DmaapPropertyReader::getTransformedMandatoryInfoProperties).flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<String, String> getTransformedMandatoryInfoProperties(Map.Entry<String, AnyNode> el) {
        String prefix = el.getKey() + ".";
        AnyNode val = el.getValue();
        Map<String, String> map = Maps.newHashMap();
        map.put(prefix + "basicAuthUsername", val.getAsOptional("aaf_username").orElse(AnyNode.nullValue()).asString().replace("\"", ""));
        map.put(prefix + "basicAuthPassword", val.getAsOptional("aaf_password").orElse(AnyNode.nullValue()).asString().replace("\"", ""));
        map.putAll(getParamsFromDmaapInfoTopicUrl(prefix, val.get("dmaap_info").get("topic_url").asString().replace("\"", "")));
        return map;
    }

    /***
     * Dmaap url structure pub - https://<dmaaphostname>:<port>/events/
     * <namespace>.<dmaapcluster>.<topic>, sub - https://<dmaaphostname>:
     * <port>/events/<namespace>.<dmaapcluster>.<topic>/G1/u1";
     *
     * Onap url structure pub - http://<dmaaphostname>:<port>/<unauthenticated>.
     * <topic>,
     */
    private static Map<String, String> getParamsFromDmaapInfoTopicUrl(String keyPrefix, String topicUrl) {
        Map<String, String> topicUrlParts = Maps.newHashMap();
        try {
            URL url = new URL(topicUrl);
            topicUrlParts.put(keyPrefix + CAMBRIA_URL_KEY, url.getAuthority());
            String[] pathParts = url.getPath().split("/");
            if (pathParts.length > 2 && "events".equals(pathParts[1])) {
                // DCAE internal dmaap topic convention
                topicUrlParts.put(keyPrefix + CAMBRIA_TOPIC_KEY, pathParts[2]);
            } else {
                // ONAP dmaap topic convention
                topicUrlParts.put(keyPrefix + CAMBRIA_TOPIC_KEY, pathParts[2]);
                topicUrlParts.put(keyPrefix + CAMBRIA_HOSTS_KEY, url.getHost());
            }
        } catch (MalformedURLException e) {
            log.error("Invalid URL found under topic_url key!", e);
            e.printStackTrace();
        }
        return topicUrlParts;
    }
}
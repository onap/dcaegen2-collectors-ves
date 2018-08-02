/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2018 Nokia. All rights reserved.
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
package org.onap.dcae.commonFunction.event.publishing;

import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.onap.dcae.commonFunction.AnyNode;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONObject;

import static io.vavr.API.*;
import static org.onap.dcae.commonFunction.event.publishing.VavrUtils.enhanceError;
import static org.onap.dcae.commonFunction.event.publishing.VavrUtils.f;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
@SuppressWarnings("mapFailure takes a generic varargs, unchecked because of Javas type system limitation, actually safe to do")
public final class DMaaPConfigurationParser {

    public static Try<Map<String, PublisherConfig>> parseToDomainMapping(Path configLocation) {
        return readFromFile(configLocation)
                .flatMap(DMaaPConfigurationParser::toJSON)
                .flatMap(DMaaPConfigurationParser::toConfigMap);
    }

    public static Try<Map<String, PublisherConfig>> parseToDomainMapping(JSONObject config) {
        return toJSON(config.toString())
            .flatMap(DMaaPConfigurationParser::toConfigMap);
    }

    private static Try<String> readFromFile(Path configLocation) {
        return Try(() -> new String(Files.readAllBytes(configLocation)))
                .mapFailure(enhanceError(f("Could not read DMaaP configuration from location: '%s'", configLocation)));
    }

    private static Try<AnyNode> toJSON(String config) {
        return Try(() -> AnyNode.fromString(config))
                .mapFailure(enhanceError(f("DMaaP configuration '%s' is not a valid JSON document", config)));
    }

    private static Try<Map<String, PublisherConfig>> toConfigMap(AnyNode config) {
        return Try(() -> usesLegacyFormat(config) ? parseLegacyFormat(config) : parseNewFormat(config))
                .mapFailure(enhanceError(f("Parsing DMaaP configuration: '%s' failed, probably it is in unexpected format", config)));
    }

    private static boolean usesLegacyFormat(AnyNode dMaaPConfig) {
        return dMaaPConfig.has("channels");
    }

    private static Map<String, PublisherConfig> parseLegacyFormat(AnyNode root) {
        return root.get("channels").toList().toMap(
                channel -> channel.get("name").toString(),
                channel -> {
                    String destinationsStr = channel.getAsOption("cambria.url")
                            .getOrElse(channel.getAsOption("cambria.hosts").get())
                            .toString();
                    String topic = channel.get("cambria.topic").toString();
                    Option<String> maybeUser = channel.getAsOption("basicAuthUsername").map(AnyNode::toString);
                    Option<String> maybePassword = channel.getAsOption("basicAuthPassword").map(AnyNode::toString);
                    List<String> destinations = List(destinationsStr.split(","));
                    return buildBasedOnAuth(maybeUser, maybePassword, topic, destinations);
                });
    }

    private static Map<String, PublisherConfig> parseNewFormat(AnyNode root) {
        return root.keys().toMap(
                channelName -> channelName,
                channelName -> {
                    AnyNode channelConfig = root.get(channelName);
                    Option<String> maybeUser = channelConfig.getAsOption("aaf_username").map(AnyNode::toString);
                    Option<String> maybePassword = channelConfig.getAsOption("aaf_password").map(AnyNode::toString);
                    URL topicURL = unchecked(
                            () -> new URL(channelConfig.get("dmaap_info").get("topic_url").toString())).apply();
                    String[] pathSegments = topicURL.getPath().substring(1).split("/");
                    String topic = pathSegments[1];
                    String destination = "events".equals(pathSegments[0]) ? topicURL.getAuthority() : topicURL.getHost();
                    List<String> destinations = List(destination);
                    return buildBasedOnAuth(maybeUser, maybePassword, topic, destinations);
                });
    }

    private static PublisherConfig buildBasedOnAuth(Option<String> maybeUser, Option<String> maybePassword,
                                                    String topic, List<String> destinations) {
        return maybeUser.flatMap(user -> maybePassword.map(password -> Tuple(user, password)))
                .map(credentials -> new PublisherConfig(destinations, topic, credentials._1, credentials._2))
                .getOrElse(new PublisherConfig(destinations, topic));
    }
}

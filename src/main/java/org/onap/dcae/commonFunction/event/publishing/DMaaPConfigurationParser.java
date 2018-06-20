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

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.List;
import static io.vavr.API.Try;
import static io.vavr.API.Tuple;
import static io.vavr.API.unchecked;

import io.vavr.API.Match.Case;
import io.vavr.CheckedFunction1;
import io.vavr.Function1;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.onap.dcae.commonFunction.AnyNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
final class DMaaPConfigurationParser {

    private static final Logger log = LoggerFactory.getLogger(DMaaPConfigurationParser.class);
    private static final Function1<String, URL> toURL = unchecked((CheckedFunction1<String, URL>) URL::new);

    @SuppressWarnings("unchecked because of Javas type system limitation, actually safe to do")
    static Map<String, PublisherConfig> parseDMaaPConfig(Path dMaaPConfigLocation) {
        log.info(f("Building DMaaP config using configuration file: '%s'", dMaaPConfigLocation));
        return Try(() -> AnyNode.fromString(new String(Files.readAllBytes(dMaaPConfigLocation))))
            .peek(jsonNode -> log.info(f("Loading DMaaP publishers configurations based on config: '%s'", jsonNode)))
            .mapFailure(withMessage(f("Could not load DMaaPConfiguration from file: '%s'", dMaaPConfigLocation)))
            .flatMap(jsonNode -> Try(() -> usesLegacyFormat(jsonNode) ? parseLegacyFormat(jsonNode) : parseNewFormat(jsonNode))
                .mapFailure(withMessage("Parsing DMaaP configuration failed, probably it is in unexpected format")))
            .peek(e -> log.info(f("Successfully loaded following DMaaP publishers configurations: '%s'", e)))
            .get();
    }

    private static boolean usesLegacyFormat(AnyNode dMaaPConfig) {
        return dMaaPConfig.has("channels");
    }

    private static Case<Throwable, Throwable> withMessage(String msg) {
        return Case($(), e -> new RuntimeException(msg, e));
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
                URL topicURL = toURL.apply(channelConfig.get("dmaap_info").get("topic_url").toString());
                String[] pathSegments = topicURL.getPath().substring(1).split("/");
                String topic = pathSegments[1];
                String destination = "events".equals(pathSegments[0]) ? topicURL.getAuthority() : topicURL.getHost();
                List<String> destinations = List(destination);
                return buildBasedOnAuth(maybeUser, maybePassword, topic, destinations);
            });
    }

    private static PublisherConfig buildBasedOnAuth(Option<String> maybeUser,
                                                    Option<String> maybePassword,
                                                    String topic, List<String> destinations) {
        return maybeUser.flatMap(user -> maybePassword.map(password -> Tuple(user, password)))
            .map(credentials -> new PublisherConfig(destinations, topic, credentials._1, credentials._2))
            .getOrElse(new PublisherConfig(destinations, topic));
    }

    /**
     * Shortcut for 'string interpolation'
     */
    private static String f(String msg, Object... args) {
        return String.format(msg, args);
    }
}

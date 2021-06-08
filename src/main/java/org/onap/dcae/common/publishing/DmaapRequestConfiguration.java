/*-
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2021 Nokia. All rights reserved.
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
package org.onap.dcae.common.publishing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.jetbrains.annotations.NotNull;
import org.onap.dcaegen2.services.sdk.model.streams.dmaap.ImmutableMessageRouterSink;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.ContentType;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.ImmutableMessageRouterPublishRequest;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.MessageRouterPublishRequest;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.config.ImmutableDmaapConnectionPoolConfig;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.config.ImmutableDmaapRetryConfig;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.config.ImmutableDmaapTimeoutConfig;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.config.ImmutableMessageRouterPublisherConfig;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.config.MessageRouterPublisherConfig;
import reactor.core.publisher.Flux;

import java.time.Duration;

public class DmaapRequestConfiguration {

    private static final Long TIMEOUT_SECONDS = 10L;
    private static final int RETRY_INTERVAL_IN_SECONDS = 1;
    private static final int RETRY_COUNT = 1;

    private DmaapRequestConfiguration() {
    }

    static MessageRouterPublishRequest createPublishRequest(Option<PublisherConfig> publisherConfig, Long timeout) {
        String topicUrl = createUrl(publisherConfig);
        return ImmutableMessageRouterPublishRequest.builder()
                .sinkDefinition(createMessageRouterSink(topicUrl))
                .contentType(ContentType.APPLICATION_JSON)
                .timeoutConfig(timeOutConfiguration(timeout))
                .build();
    }

    static MessageRouterPublishRequest createPublishRequest(Option<PublisherConfig> publisherConfig) {
        return createPublishRequest(publisherConfig, TIMEOUT_SECONDS);
    }

    static Flux<JsonObject> jsonBatch(List<String> messages) {
        return Flux.fromIterable(getAsJsonObjects(messages));
    }

    static MessageRouterPublisherConfig retryConfiguration() {
        return ImmutableMessageRouterPublisherConfig.builder()
                .retryConfig(ImmutableDmaapRetryConfig.builder()
                        .retryIntervalInSeconds(RETRY_INTERVAL_IN_SECONDS)
                        .retryCount(RETRY_COUNT)
                        .build())
                .build();
    }

    private static String createUrl(Option<PublisherConfig> publisherConfig) {
        String hostAndPort = publisherConfig.get().getHostAndPort();
        String topicName = publisherConfig.get().topic();
        return String.format("http://%s/events/%s/",hostAndPort,topicName);
    }

    private static List<JsonObject> getAsJsonObjects(List<String> messages) {
        return getAsJsonElements(messages).map(JsonElement::getAsJsonObject);
    }

    static List<JsonElement> getAsJsonElements(List<String> messages) {
        return messages.map(JsonParser::parseString);
    }

    static ImmutableMessageRouterSink createMessageRouterSink(String topicUrl) {
        return ImmutableMessageRouterSink.builder()
                .name("the topic")
                .topicUrl(topicUrl)
                .build();
    }

    @NotNull
    private static ImmutableDmaapTimeoutConfig timeOutConfiguration(Long timeout) {
        return ImmutableDmaapTimeoutConfig.builder().timeout(Duration.ofSeconds(timeout)).build();
    }
}

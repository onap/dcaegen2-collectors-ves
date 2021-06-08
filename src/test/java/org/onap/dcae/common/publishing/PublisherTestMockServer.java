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
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.matchers.Times;
import org.mockserver.verify.VerificationTimes;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.ImmutableMessageRouterPublishResponse;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.MessageRouterPublishResponse;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.config.ImmutableDmaapConnectionPoolConfig;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.config.ImmutableMessageRouterPublisherConfig;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.config.MessageRouterPublisherConfig;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.onap.dcae.common.publishing.DmaapRequestConfiguration.createPublishRequest;
import static org.onap.dcae.common.publishing.DmaapRequestConfiguration.getAsJsonElements;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {1080, 8888})
class PublisherTestMockServer {

    private static final int MAX_IDLE_TIME = 10;
    private static final int MAX_LIFE_TIME = 20;
    private static final int CONNECTION_POOL = 1;
    private static final String TOPIC = "TOPIC10";
    private static final String PATH = String.format("/events/%s/", TOPIC);

    private static final String TIMEOUT_ERROR_MESSAGE = "408 Request Timeout\n"
            + "{"
            + "\"requestError\":"
            + "{"
            + "\"serviceException\":"
            + "{"
            + "\"messageId\":\"SVC0001\","
            + "\"text\":\"Client timeout exception occurred, Error code is %1\","
            + "\"variables\":[\"408\"]"
            + "}"
            + "}"
            + "}";

    private final ClientAndServer client;

    public PublisherTestMockServer(ClientAndServer client) {
        this.client = client;
    }

    @Test
    void publisher_shouldHandleClientTimeoutErrorWhenTimeoutDefined() {
        //given
        final Long timeoutSec = 1L;
        final Publisher publisher = new Publisher(connectionPoolConfiguration());
        final String simpleEvent = "{\"message\":\"message1\"}";
        final MessageRouterPublishResponse expectedResponse = errorPublishResponse(TIMEOUT_ERROR_MESSAGE);

        final String path = String.format("/events/%s/", TOPIC);
        client.when(request().withPath(path), Times.once())
               .respond(response().withDelay(TimeUnit.SECONDS, 2));
        List<String> events = List.of(simpleEvent);

        //when
        final Flux<MessageRouterPublishResponse> result = publisher.publishEvents(events, createPublishRequest(createPublishConfig(), timeoutSec));



        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .expectComplete()
                .verify(Duration.ofSeconds(10));

        //then
        client.verify(request().withPath(path), VerificationTimes.exactly(1));

    }

    @Test
    void publishEvent_shouldSuccessfullyPublishSingleMessage() {
        //given
        final Publisher publisher = new Publisher();
        final String simpleEvent = "{\"message\":\"message1\"}";
        final List<String> twoJsonMessages = List.of(simpleEvent);
        final MessageRouterPublishResponse expectedResponse = successPublishResponse(getAsJsonElements(twoJsonMessages));
        client.when(request().withPath(PATH), Times.once())
                .respond(response());

        //when
        final Flux<MessageRouterPublishResponse> result = publisher.publishEvents(List.of(simpleEvent), createPublishConfig());

        //then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    private Option<PublisherConfig> createPublishConfig() {
        List<String> desc = List.of("localhost:1080");
        PublisherConfig conf = new PublisherConfig(desc, TOPIC);
        return Option.of(conf);
    }

    private MessageRouterPublishResponse successPublishResponse(List<JsonElement> items) {
        return ImmutableMessageRouterPublishResponse
                .builder()
                .items(items)
                .build();
    }

    public static MessageRouterPublishResponse errorPublishResponse(String failReasonFormat, Object... formatArgs) {
        String failReason = formatArgs.length == 0 ? failReasonFormat : String.format(failReasonFormat, formatArgs);
        return ImmutableMessageRouterPublishResponse
                .builder()
                .failReason(failReason)
                .build();
    }

    public MessageRouterPublisherConfig connectionPoolConfiguration() {
        return ImmutableMessageRouterPublisherConfig.builder()
                .connectionPoolConfig(ImmutableDmaapConnectionPoolConfig.builder()
                        .connectionPool(CONNECTION_POOL)
                        .maxIdleTime(MAX_IDLE_TIME)
                        .maxLifeTime(MAX_LIFE_TIME)
                        .build())
                .build();
    }
}

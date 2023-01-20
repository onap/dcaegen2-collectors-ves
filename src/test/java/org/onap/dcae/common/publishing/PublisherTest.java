/*-
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2021 Nokia. All rights reserved.
 * Copyright (C) 2023 AT&T Intellectual Property. All rights reserved.
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
import org.junit.Assume;
import org.junit.Before;

import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.ImmutableMessageRouterPublishResponse;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.MessageRouterPublishResponse;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.onap.dcae.common.publishing.DMaapContainer.createContainerInstance;
import static org.onap.dcae.common.publishing.DmaapRequestConfiguration.getAsJsonElements;


@Testcontainers(disabledWithoutDocker = true)
public class PublisherTest  {

    @Container
    private final DockerComposeContainer CONTAINER = createContainerInstance();
    
    @Before
    public void linuxOnly() {
        Assume.assumeFalse
        (System.getProperty("os.name").toLowerCase().startsWith("win"));
    }

    @Test
    public void publishEvent_shouldSuccessfullyPublishSingleMessage() {
        //given
        final Publisher publisher = new Publisher();
        final String simpleEvent = "{\"message\":\"message1\"}";
        final List<String> twoJsonMessages = List.of(simpleEvent);
        final MessageRouterPublishResponse expectedResponse = successPublishResponse(getAsJsonElements(twoJsonMessages));

        //when
        final Flux<MessageRouterPublishResponse> result = publisher.publishEvents(twoJsonMessages, createPublishConfig());

        //then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }


    private Option<PublisherConfig> createPublishConfig() {
        List<String> desc = List.of("127.0.0.1:3904");
        PublisherConfig conf = new PublisherConfig(desc, "topic");
        return Option.of(conf);
    }

    private MessageRouterPublishResponse successPublishResponse(List<JsonElement> items) {
        return ImmutableMessageRouterPublishResponse
                .builder()
                .items(items)
                .build();
    }

}

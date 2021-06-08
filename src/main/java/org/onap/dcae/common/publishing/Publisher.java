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

import com.google.gson.JsonObject;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.api.DmaapClientFactory;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.api.MessageRouterPublisher;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.MessageRouterPublishRequest;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.MessageRouterPublishResponse;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.config.MessageRouterPublisherConfig;
import reactor.core.publisher.Flux;

import static org.onap.dcae.common.publishing.DmaapRequestConfiguration.retryConfiguration;
import static org.onap.dcae.common.publishing.DmaapRequestConfiguration.createPublishRequest;
import static org.onap.dcae.common.publishing.DmaapRequestConfiguration.jsonBatch;

public class Publisher {

    private final MessageRouterPublisher publisher;

    public Publisher() {
        this(retryConfiguration());
    }

    public Publisher(MessageRouterPublisherConfig messageRouterPublisherConfig) {
        publisher = DmaapClientFactory
                .createMessageRouterPublisher(messageRouterPublisherConfig);
    }

    /**
     * Publish event
     *
     * @param events list of ves events prepared to send
     * @param publisherConfig publisher configuration
     * @return flux containing information about the success or failure of the event publication
     */
    public Flux<MessageRouterPublishResponse> publishEvents(List<String> events, Option<PublisherConfig> publisherConfig) {
        return publishEvents(events, createPublishRequest(publisherConfig));
    }

    Flux<MessageRouterPublishResponse> publishEvents(List<String> events, MessageRouterPublishRequest publishRequest) {
        final Flux<JsonObject> jsonMessageBatch = jsonBatch(events);
        return publisher.put(publishRequest, jsonMessageBatch);
    }
}

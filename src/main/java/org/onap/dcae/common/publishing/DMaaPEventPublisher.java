/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2017,2020,2023 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2018-2021 Nokia. All rights reserved.
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

import io.vavr.collection.Map;
import org.onap.dcae.common.model.VesEvent;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.MessageRouterPublishResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

import static org.onap.dcae.common.publishing.MessageRouterHttpStatusMapper.getHttpStatus;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
public class DMaaPEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(DMaaPEventPublisher.class);
    private Map<String, PublisherConfig> dMaaPConfig;
    private final Publisher dmaapPublisher;

    public DMaaPEventPublisher(Map<String, PublisherConfig> dMaaPConfig) {
        this.dMaaPConfig = dMaaPConfig;
        dmaapPublisher = new Publisher();
    }

    /**
     * Reload Dmaap configuration
     * 
     * @param dmaapConfiguration Dmaap configuration
     */
    public void reload(Map<String, PublisherConfig> dmaapConfiguration) {
        dMaaPConfig = dmaapConfiguration;
        log.info("reload dmaap configuration");
    }

    public HttpStatus sendEvent(List<VesEvent> vesEvents, String dmaapId) {
        clearVesUniqueIdFromEvent(vesEvents);
        io.vavr.collection.List<String> events = mapListOfEventsToVavrList(vesEvents);
        HttpStatus rc = messageRouterPublishResponse(events, dmaapId);
        return rc;
    }

    HttpStatus messageRouterPublishResponse(io.vavr.collection.List<String> events, String dmaapId) {
        Flux<MessageRouterPublishResponse> messageRouterPublishFlux =
                dmaapPublisher.publishEvents(events, dMaaPConfig.get(dmaapId));
        MessageRouterPublishResponse messageRouterPublishResponse = messageRouterPublishFlux.blockFirst();
        return getHttpStatus(Objects.requireNonNull(messageRouterPublishResponse));

    }

    private io.vavr.collection.List<String> mapListOfEventsToVavrList(List<VesEvent> vesEvents) {
        return io.vavr.collection.List.ofAll(vesEvents).map(event -> event.asJsonObject().toString());
    }

    private void clearVesUniqueIdFromEvent(List<VesEvent> events) {
        events.stream().filter(event -> event.hasType(VesEvent.VES_UNIQUE_ID)).forEach(event -> {
            log.debug("Removing VESuniqueid object from event");
            event.removeElement(VesEvent.VES_UNIQUE_ID);
        });
    }
}

/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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
package org.onap.dcae.common;


import io.vavr.collection.Map;
import org.onap.dcae.common.model.VesEvent;
import org.onap.dcae.common.publishing.DMaaPEventPublisher;
import org.onap.dcae.restapi.EventValidatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.onap.dcae.restapi.ApiException.DOMAIN_NOT_DEFINED_FOR_STREAM_ID;


public class EventSender {

    private Map<String, String> streamIdToDmaapIds;
    private DMaaPEventPublisher eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(EventSender.class);

    public EventSender(DMaaPEventPublisher eventPublisher, Map<String, String> streamIdToDmaapIds) {
        this.eventPublisher = eventPublisher;
        this.streamIdToDmaapIds = streamIdToDmaapIds;
    }

    public HttpStatus send(List<VesEvent> vesEvents) {
        String topic = streamIdToDmaapIds
                .get(vesEvents.get(0).getStreamId())
                .getOrElse(() -> {
                    log.error("No StreamID defined for publish - Message dropped " + vesEvents.get(0).asJsonObject());
                    throw new EventValidatorException(DOMAIN_NOT_DEFINED_FOR_STREAM_ID);
                });
        return eventPublisher.sendEvent(vesEvents, topic);
    }
}

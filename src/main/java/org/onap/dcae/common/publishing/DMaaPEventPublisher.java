/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2017,2020 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2018,2020 Nokia. All rights reserved.
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

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import org.onap.dcae.common.VESLogger;
import org.onap.dcae.common.model.VesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.onap.dcae.common.publishing.VavrUtils.f;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
public class DMaaPEventPublisher {
    private static final int PENDING_MESSAGE_LOG_THRESHOLD = 100;
    private static final Logger log = LoggerFactory.getLogger(DMaaPEventPublisher.class);
    private final DMaaPPublishersCache publishersCache;
    private final Logger outputLogger = LoggerFactory.getLogger("org.onap.dcae.common.output");

    DMaaPEventPublisher(DMaaPPublishersCache publishersCache) {
        this.publishersCache = publishersCache;
    }

    public DMaaPEventPublisher(Map<String, PublisherConfig> dMaaPConfig) {
        this(new DMaaPPublishersCache(dMaaPConfig));
    }

    public void sendEvent(VesEvent vesEvent, String dmaapId){
        clearVesUniqueIdFromEvent(vesEvent);
        publishersCache.getPublisher(dmaapId)
                .onEmpty(() ->
                        log.warn(f("Could not find event publisher for domain: '%s', dropping message: '%s'", dmaapId, vesEvent)))
                .forEach(publisher -> sendEvent(vesEvent, dmaapId, publisher));
    }

    private void sendEvent(VesEvent event, String dmaapId, CambriaBatchingPublisher publisher) {
        Try.run(() -> uncheckedSendEvent(event, dmaapId, publisher))
                .onFailure(exc -> closePublisher(event, dmaapId, exc));
    }

    private void uncheckedSendEvent(VesEvent event, String dmaapId, CambriaBatchingPublisher publisher)
            throws IOException {

	    String pk = event.getPK();
        int pendingMsgs = publisher.send(pk, event.asJsonObject().toString());
        if (pendingMsgs > PENDING_MESSAGE_LOG_THRESHOLD) {
            log.info("Pending messages count: " + pendingMsgs);
        }
        String infoMsg = f("Event: '%s' scheduled to be send asynchronously on domain: '%s'", event, dmaapId);
        log.info(infoMsg);
        outputLogger.info(infoMsg);
    }

    private void closePublisher(VesEvent event, String dmaapId, Throwable e) {
        log.error(f("Unable to schedule event: '%s' on domain: '%s'. Closing publisher and dropping message.",
                event, dmaapId), e);
        publishersCache.closePublisherFor(dmaapId);
    }

    private void clearVesUniqueIdFromEvent(VesEvent event) {
        if (event.hasType(VesEvent.VES_UNIQUE_ID)) {
            String uuid =event.getUniqueId().toString();
            LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid);
            localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
            log.debug("Removing VESuniqueid object from event");
            event.removeElement(VesEvent.VES_UNIQUE_ID);
        }
    }
}

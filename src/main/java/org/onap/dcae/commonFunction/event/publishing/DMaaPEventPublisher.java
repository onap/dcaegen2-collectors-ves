/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

import static org.onap.dcae.commonFunction.event.publishing.VavrUtils.f;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import java.io.IOException;
import org.json.JSONObject;
import org.onap.dcae.commonFunction.VESLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
class DMaaPEventPublisher implements EventPublisher {
    private static final int PENDING_MESSAGE_LOG_THRESHOLD = 100;
    private static final String VES_UNIQUE_ID = "VESuniqueId";
    private static final Logger log = LoggerFactory.getLogger(DMaaPEventPublisher.class);
    private final DMaaPPublishersCache publishersCache;
    private final Logger outputLogger;

    DMaaPEventPublisher(DMaaPPublishersCache DMaaPPublishersCache,
                        Logger outputLogger) {
        this.publishersCache = DMaaPPublishersCache;
        this.outputLogger = outputLogger;
    }

    @Override
    public void sendEvent(JSONObject event, String domain) {
        clearVesUniqueIdFromEvent(event);
        publishersCache.getPublisher(domain)
            .onEmpty(() ->
                log.warn(f("Could not find event publisher for domain: '%s', dropping message: '%s'", domain, event)))
            .forEach(publisher -> sendEvent(event, domain, publisher));
    }

    @Override
    public void reconfigure(Map<String, PublisherConfig> dMaaPConfig) {
        publishersCache.reconfigure(dMaaPConfig);
    }

    private void sendEvent(JSONObject event, String domain, CambriaBatchingPublisher publisher) {
        Try.run(() -> uncheckedSendEvent(event, domain, publisher))
            .onFailure(exc -> closePublisher(event, domain, exc));
    }

    private void uncheckedSendEvent(JSONObject event, String domain, CambriaBatchingPublisher publisher)
        throws IOException {
        int pendingMsgs = publisher.send("MyPartitionKey", event.toString());
        if (pendingMsgs > PENDING_MESSAGE_LOG_THRESHOLD) {
            log.info("Pending messages count: " + pendingMsgs);
        }
        String infoMsg = f("Event: '%s' scheduled to be send asynchronously on domain: '%s'", event, domain);
        log.info(infoMsg);
        outputLogger.info(infoMsg);
    }

    private void closePublisher(JSONObject event, String domain, Throwable e) {
        log.error(f("Unable to schedule event: '%s' on domain: '%s'. Closing publisher and dropping message.",
            event, domain), e);
        publishersCache.closePublisherFor(domain);
    }

    private void clearVesUniqueIdFromEvent(JSONObject event) {
        if (event.has(VES_UNIQUE_ID)) {
            String uuid = event.get(VES_UNIQUE_ID).toString();
            LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid);
            localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
            log.debug("Removing VESuniqueid object from event");
            event.remove(VES_UNIQUE_ID);
        }
    }
}

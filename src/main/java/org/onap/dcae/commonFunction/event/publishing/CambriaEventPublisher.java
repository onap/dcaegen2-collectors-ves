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

import static java.lang.String.format;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import java.io.IOException;
import org.json.JSONObject;
import org.onap.dcae.commonFunction.VESLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
class CambriaEventPublisher implements EventPublisher {

    private static final String VES_UNIQUE_ID = "VESuniqueId";
    private static final Logger log = LoggerFactory.getLogger(CambriaEventPublisher.class);
    private final CambriaPublishersCache cambriaPublishersCache;
    private final Logger outputLogger;

    CambriaEventPublisher(CambriaPublishersCache cambriaPublishersCache,
                          Logger outputLogger) {
        this.cambriaPublishersCache = cambriaPublishersCache;
        this.outputLogger = outputLogger;
    }

    @Override
    public void sendEvent(JSONObject event, String streamID) {
        clearVesUniqueIdFromEvent(event);
        cambriaPublishersCache.getPublisher(streamID)
            .onEmpty(() -> log.warn(format("Could not find event publisher for streamID: %s, message %s dropped", streamID, event)))
            .forEach(publisher -> sendEvent(event, streamID, publisher));
    }

    private void sendEvent(JSONObject event, String streamID, CambriaBatchingPublisher publisher) {
        try {
            int pendingMsgs = publisher.send("MyPartitionKey", event.toString());
            if (pendingMsgs > 100) {
                log.info("Pending messages count: " + pendingMsgs);
            }
            log.info(format("Event: '%s' scheduled to be send asynchronously on streamID: '%s'", event, streamID));
            outputLogger.info(format("Event: '%s' scheduled to be send asynchronously on streamID: '%s'", event, streamID));
        } catch (IOException e) {
            log.error(format("Unable to publish event: %s on streamID: %s. " + ". Closing publisher", event, streamID), e);
            cambriaPublishersCache.closePublisherFor(streamID);
        }
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

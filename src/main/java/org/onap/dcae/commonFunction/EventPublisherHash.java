/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

package org.onap.dcae.commonFunction;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventPublisherHash {

    private static final String VES_UNIQUE_ID = "VESuniqueId";
    private static final Logger log = LoggerFactory.getLogger(EventPublisherHash.class);
    private static volatile EventPublisherHash instance = new EventPublisherHash(DmaapPublishers.create());
    private final DmaapPublishers dmaapPublishers;

    /**
     * Returns event publisher
     *
     * @return event publisher
     */
    public static EventPublisherHash getInstance() {
        return instance;
    }

    @VisibleForTesting
    EventPublisherHash(DmaapPublishers dmaapPublishers) {
        this.dmaapPublishers = dmaapPublishers;
    }

    public void sendEvent(JSONObject event, String streamid) {
        log.debug("EventPublisher.sendEvent: instance for publish is ready");
        clearVesUniqueId(event);

        try {
            sendEventUsingCachedPublisher(streamid, event);
        } catch (IOException | IllegalArgumentException e) {
            log.error("Unable to publish event: {} streamid: {}. Exception: {}", event, streamid, e);
            dmaapPublishers.closeByStreamId(streamid);
        }
    }

    private void clearVesUniqueId(JSONObject event) {
        if (event.has(VES_UNIQUE_ID)) {
            String uuid = event.get(VES_UNIQUE_ID).toString();
            LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid);
            localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
            log.debug("Removing VESuniqueid object from event");
            event.remove(VES_UNIQUE_ID);
        }
    }

    private void sendEventUsingCachedPublisher(String streamid, JSONObject event) throws IOException {
        int pendingMsgs = dmaapPublishers.getByStreamId(streamid).send("MyPartitionKey", event.toString());
        // this.wait(2000);

        if (pendingMsgs > 100) {
            log.info("Pending Message Count=" + pendingMsgs);
        }

        log.info("pub.send invoked - no error");
        //CommonStartup.oplog.info(String.format("URL:%sTOPIC:%sEvent Published:%s", ueburl, topic, event));
        CommonStartup.oplog.info(String.format("StreamID:%s Event Published:%s ", streamid, event));
    }

    @VisibleForTesting
    public CambriaBatchingPublisher getDmaapPublisher(String streamId) {
        return dmaapPublishers.getByStreamId(streamId);
    }
}

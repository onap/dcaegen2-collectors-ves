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
import com.att.nsa.cambria.client.CambriaClientBuilders;
import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class EventPublisher {

    private static final String VES_UNIQUE_ID = "VESuniqueId";

    private static EventPublisher instance;
    private static CambriaBatchingPublisher pub;
    private DMaaPChannel channel;

    private static Logger log = LoggerFactory.getLogger(EventPublisher.class);


    private EventPublisher(String streamId) {
        try {
            // TODO: Throw Exception if channel not found
            channel = DmaapPropertyReader.getInstance(CommonStartup.cambriaConfigFile).getChannel(streamId);
        } catch (Exception e) {
            log.error("CambriaClientBuilders connection reader exception : " + e.getMessage());
        }
    }

    /**
     * Returns event publisher.
     *
     * @param streamid stream id
     * @return event publisher
     */
    public static synchronized EventPublisher getInstance(String streamid) {
        if (instance == null) {
            instance = new EventPublisher(streamid);
        } else if (!instance.channel.getName().equals(streamid)) {
            instance.closePublisher();
            instance = new EventPublisher(streamid);
        }
        return instance;
    }


    /**
     *
     * @param event json object for event
     */
    public synchronized void sendEvent(JSONObject event) {

        log.debug("EventPublisher.sendEvent: instance for publish is ready");

        if (event.has(VES_UNIQUE_ID)) {
            String uuid = event.get(VES_UNIQUE_ID).toString();
            LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid);
            localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
            log.debug("Removing VESuniqueid object from event");
            event.remove(VES_UNIQUE_ID);
        }

        try {
            log.debug("URL: {}, TOPIC: {}, AuthUser: {}, Authpwd: {}",
                      channel.getCambriaUrl(), channel.getCambriaTopic(),
                      channel.getBasicAuthUsername(), channel.getBasicAuthPassword());

            CambriaClientBuilders.PublisherBuilder pBuilder =
                    new CambriaClientBuilders.PublisherBuilder()
                            .usingHosts(channel.getCambriaUrl())
                            .onTopic(channel.getCambriaTopic())
                            //.logTo(log)
                            //.limitBatch(100, 10)
                            .logSendFailuresAfter(5);

            if (channel.getBasicAuthUsername() != null) {
                pBuilder.authenticatedByHttp(channel.getBasicAuthUsername(),
                                             channel.getBasicAuthPassword());
            }

            pub = pBuilder.build();
            int pendingMsgs = pub.send("MyPartitionKey", event.toString());
            //this.wait(2000);

            if (pendingMsgs > 100) {
                log.info("Pending Message Count=" + pendingMsgs);
            }

            //closePublisher();
            log.info("pub.send invoked - no error");
            CommonStartup.oplog.info(String.format("URL:%sTOPIC:%sEvent Published:%s",
                channel.getCambriaUrl(), channel.getCambriaTopic(), event));
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            log.error("Unable to publish event: {} streamid: {}. Exception: {}", event, channel.getName(), e);
        }
    }


    public synchronized void closePublisher() {
        try {
            if (pub != null) {
                final List<?> stuck = pub.close(20, TimeUnit.SECONDS);
                if (!stuck.isEmpty()) {
                    log.error(stuck.size() + " messages unsent");
                }
            }
        } catch (InterruptedException | IOException e) {
            log.error("Caught Exception on Close event: {}", e);
        }
    }
}

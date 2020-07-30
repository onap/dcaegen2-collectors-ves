/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2018 Nokia. All rights reserved.s
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

import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import io.vavr.collection.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.publishing.DMaaPEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventSender {

  private static final Logger metriclog = LoggerFactory.getLogger("com.att.ecomp.metrics");
  private Map<String, String[]> streamidHash;
  private DMaaPEventPublisher dMaaPEventPublisher;
  private static final String VES_UNIQUE_ID = "VESuniqueId";
  private static final Logger log = LoggerFactory.getLogger(EventSender.class);
  private static final String EVENT_LITERAL = "event";
  private static final String COMMON_EVENT_HEADER = "commonEventHeader";

  public EventSender( DMaaPEventPublisher dMaaPEventPublisher, ApplicationSettings properties) {
    this.dMaaPEventPublisher = dMaaPEventPublisher;
    this.streamidHash = properties.dMaaPStreamsMapping();
  }

  public void send(JSONArray arrayOfEvents) {
    for (int i = 0; i < arrayOfEvents.length(); i++) {
      metriclog.info("EVENT_PUBLISH_START");
      JSONObject object = (JSONObject) arrayOfEvents.get(i);
      setLoggingContext(object);
      streamidHash.get(getDomain(object))
          .onEmpty(() -> log.error("No StreamID defined for publish - Message dropped" + object))
          .forEach(streamIds -> sendEventsToStreams(object, streamIds));
      log.debug("Message published" + object);
    }
    log.debug("CommonStartup.handleEvents:EVENTS has been published successfully!");
    metriclog.info("EVENT_PUBLISH_END");
  }

  private static String getDomain(JSONObject event) {
    return event.getJSONObject(EVENT_LITERAL).getJSONObject(COMMON_EVENT_HEADER).getString("domain");
  }

  private void sendEventsToStreams(JSONObject event, String[] streamIdList) {
    for (String aStreamIdList : streamIdList) {
      log.info("Invoking publisher for streamId:" + aStreamIdList);
      dMaaPEventPublisher.sendEvent(event, aStreamIdList);
    }
  }

  private void setLoggingContext(JSONObject event) {
    LoggingContext localLC = VESLogger.getLoggingContextForThread(event.get(VES_UNIQUE_ID).toString());
    localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
    log.debug("event.VESuniqueId" + event.get(VES_UNIQUE_ID) + "event.commonEventHeader.domain:" + getDomain(event));
  }
}

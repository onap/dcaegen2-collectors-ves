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
import org.onap.dcae.common.model.VesEvent;
import org.onap.dcae.common.publishing.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EventSender {

  private static final Logger metriclog = LoggerFactory.getLogger("com.att.ecomp.metrics");
  private Map<String, String[]> dmaapStreamIds;
  private EventPublisher eventPublisher;
  private static final Logger log = LoggerFactory.getLogger(EventSender.class);

  public EventSender(EventPublisher eventPublisher, Map<String, String[]> dmaapStreamIds) {
    this.eventPublisher = eventPublisher;
    this.dmaapStreamIds = dmaapStreamIds;
  }

  public void send(List<VesEvent> vesEvents) {
    for (VesEvent vesEvent : vesEvents) {
      metriclog.info("EVENT_PUBLISH_START");
      setLoggingContext(vesEvent);
      dmaapStreamIds.get(vesEvent.getDomain())
          .onEmpty(() -> log.error("No StreamID defined for publish - Message dropped" + vesEvent.asJsonObject()))
          .forEach(streamIds -> sendEventsToStreams(vesEvent, streamIds));
      log.debug("Message published" + vesEvent.asJsonObject());
    }
    log.debug("CommonStartup.handleEvents:EVENTS has been published successfully!");
    metriclog.info("EVENT_PUBLISH_END");
  }

  private void sendEventsToStreams(VesEvent vesEvent, String[] streamIdList) {
    for (String streamId : streamIdList) {
      log.info("Invoking publisher for streamId/domain:" + streamId);
      eventPublisher.sendEvent(vesEvent.asJsonObject(), streamId);
    }
  }

  private void setLoggingContext(VesEvent vesEvent) {
    LoggingContext localLC = VESLogger.getLoggingContextForThread(vesEvent.getUniqueId().toString());
    localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
    log.debug("event.VESuniqueId" + vesEvent.getUniqueId() + "event.commonEventHeader.domain:" + vesEvent.getDomain());
  }
}

/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2019 Nokia. All rights reserved.
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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import io.vavr.collection.Map;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.json.JSONObject;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.publishing.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventSender {

  private Map<String, String[]> streamidHash;
  private ApplicationSettings properties;
  private EventPublisher eventPublisher;

  static final Type EVENT_LIST_TYPE = new TypeToken<List<Event>>() {}.getType();
  private static final Logger log = LoggerFactory.getLogger(EventSender.class);
  private static final String EVENT_LITERAL = "event";
  private static final String COMMON_EVENT_HEADER = "commonEventHeader";
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MM dd yyyy hh:mm:ss z");

  public EventSender( EventPublisher eventPublisher, ApplicationSettings properties) {
    this.eventPublisher = eventPublisher;
    this.streamidHash = properties.dMaaPStreamsMapping();
    this.properties = properties;

  }

  public void send(JSONObject event) {
    streamidHash.get(getDomain(event))
        .onEmpty(() -> log.error("No StreamID defined for publish - Message dropped" + event))
        .forEach(streamIds -> sendEventsToStreams(event, streamIds));
  }

  public static String getDomain(JSONObject event) {
    return event.getJSONObject(EVENT_LITERAL).getJSONObject(COMMON_EVENT_HEADER).getString("domain");
  }

  private void sendEventsToStreams(JSONObject event, String[] streamIdList) {
    for (String aStreamIdList : streamIdList) {
      log.info("Invoking publisher for streamId:" + aStreamIdList);
      eventPublisher.sendEvent(overrideEvent(event), aStreamIdList);
    }
  }

  private JSONObject overrideEvent(JSONObject event) {
    JSONObject jsonObject = addCurrentTimeToEvent(event);
    if (properties.eventTransformingEnabled()) {
      try (FileReader fr = new FileReader("./etc/eventTransform.json")) {
        log.info("parse eventTransform.json");
        List<Event> events = new Gson().fromJson(fr, EVENT_LIST_TYPE);
        parseEventsJson(events, new ConfigProcessorAdapter(new ConfigProcessors(jsonObject)));
      } catch (IOException e) {
        log.error("Couldn't find file ./etc/eventTransform.json ", e);
      }
    }
    if (jsonObject.has("VESversion"))
      jsonObject.remove("VESversion");

    log.debug("Modified event:" + jsonObject);
    return jsonObject;
  }

  private JSONObject addCurrentTimeToEvent(JSONObject event) {
    final Date currentTime = new Date();
    JSONObject collectorTimeStamp = new JSONObject().put("collectorTimeStamp", dateFormat.format(currentTime));
    JSONObject commonEventHeaderkey = event.getJSONObject(EVENT_LITERAL).getJSONObject(COMMON_EVENT_HEADER);
    commonEventHeaderkey.put("internalHeaderFields", collectorTimeStamp);
    event.getJSONObject(EVENT_LITERAL).put(COMMON_EVENT_HEADER, commonEventHeaderkey);
    return event;
  }

  private void parseEventsJson(List<Event> eventsTransform, ConfigProcessorAdapter configProcessorAdapter) {
    for (Event eventTransform : eventsTransform) {
      JSONObject filterObj = new JSONObject(eventTransform.filter.toString());
      if (configProcessorAdapter.isFilterMet(filterObj)) {
        callProcessorsMethod(configProcessorAdapter, eventTransform.processors);
      }
    }
  }

  private void callProcessorsMethod(ConfigProcessorAdapter configProcessorAdapter, List<Processor> processors) {
    for (Processor processor : processors) {
      final String functionName = processor.functionName;
      final JSONObject args = new JSONObject(processor.args.toString());
      log.info(String.format("functionName==%s | args==%s", functionName, args));
      try {
        configProcessorAdapter.runConfigProcessorFunctionByName(functionName, args);
      } catch (ReflectiveOperationException e) {
        log.error("EventProcessor Exception" + e.getMessage() + e + e.getCause());
      }
    }
  }
}

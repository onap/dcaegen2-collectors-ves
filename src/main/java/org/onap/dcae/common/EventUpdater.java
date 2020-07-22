/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2020 Nokia. All rights reserved.s
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.model.VesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventUpdater {

  private static final String EVENT_LIST = "eventList";
  private static final String EVENT = "event";
  private static final String VES_UNIQUE_ID = "VESuniqueId";
  private static final String VES_VERSION = "VESversion";
  private static final Logger log = LoggerFactory.getLogger(EventSender.class);
  private static final String EVENT_LITERAL = "event";
  private static final String COMMON_EVENT_HEADER = "commonEventHeader";
  private ApplicationSettings settings;
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MM dd yyyy hh:mm:ss z");

  public EventUpdater(ApplicationSettings settings) {
    this.settings = settings;
  }

  public List<VesEvent> convert(JSONObject jsonObject, String version, UUID uuid, String type){
    if(type.equalsIgnoreCase(EVENT_LIST)){
      return convertEvents(jsonObject, uuid.toString(), version);
    }
    else {
      return convertEvent(jsonObject, uuid.toString(), version);
    }
  }

  private List<VesEvent> convertEvents(JSONObject jsonObject,
      String uuid, String version) {
    List<VesEvent> asArrayEvents = new ArrayList<>();

    JSONArray events = jsonObject.getJSONArray(EVENT_LIST);
    for (int i = 0; i < events.length(); i++) {
      JSONObject event = new JSONObject().put(EVENT, events.getJSONObject(i));
      event.put(VES_UNIQUE_ID, uuid + "-" + i);
      event.put(VES_VERSION, version);
      asArrayEvents.add(new VesEvent(overrideEvent(event)));
    }
    return asArrayEvents;
  }

  private List<VesEvent> convertEvent(JSONObject jsonObject, String uuid, String version) {
    jsonObject.put(VES_UNIQUE_ID, uuid);
    jsonObject.put(VES_VERSION, version);
    return List.of(new VesEvent(overrideEvent(jsonObject)));
  }

  private JSONObject overrideEvent(JSONObject event) {
    JSONObject jsonObject = addCurrentTimeToEvent(event);

    if (settings.eventTransformingEnabled()) {
      List<EventTransformation> eventTransformations = settings.getEventTransformations();
      applyMatchingTransformations(eventTransformations, new ConfigProcessorAdapter(new ConfigProcessors(jsonObject)));
    }

    if (jsonObject.has(VES_VERSION))
       jsonObject.remove(VES_VERSION);

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

  private void applyMatchingTransformations(List<EventTransformation> eventsTransforms, ConfigProcessorAdapter configProcessorAdapter) {
    for (EventTransformation eventTransform : eventsTransforms) {
      JSONObject filterObj = new JSONObject(eventTransform.filter.toString());
      if (configProcessorAdapter.isFilterMet(filterObj)) {
        callProcessorsMethod(configProcessorAdapter, eventTransform.processors);
      }
    }
  }

  private void callProcessorsMethod(ConfigProcessorAdapter configProcessorAdapter, List<Processor> processors) {
    for (Processor processor : processors) {
      //TODO try to remove refection
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

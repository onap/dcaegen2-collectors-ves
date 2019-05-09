/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2019 Nokia. All rights reserved.s
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
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.dcae.ApplicationException;
import org.onap.dcae.ApplicationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventUpdater {

  private static final String EVENT_LIST = "eventList";
  private static final String EVENT = "event";
  private static final String VES_UNIQUE_ID = "VESuniqueId";
  private static final String VES_VERSION = "VESversion";
  private static final String COULD_NOT_FIND_FILE = "Couldn't find file ./etc/eventTransform.json";
  private static final Type EVENT_LIST_TYPE = new TypeToken<List<Event>>() {}.getType();
  private static final Logger log = LoggerFactory.getLogger(EventSender.class);
  private static final String EVENT_LITERAL = "event";
  private static final String COMMON_EVENT_HEADER = "commonEventHeader";
  private ApplicationSettings settings;
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MM dd yyyy hh:mm:ss z");

  public EventUpdater(ApplicationSettings settings) {
    this.settings = settings;
  }

  public JSONArray convert(JSONObject jsonObject, String version, UUID uuid, String type){
    JSONArray arrayOfEvents;
    if(type.equalsIgnoreCase(EVENT_LIST)){
      arrayOfEvents = convertEvents(jsonObject, uuid.toString(), version);
    }
    else {
      arrayOfEvents = convertEvent(jsonObject, uuid.toString(), version);
    }
    return arrayOfEvents;
  }

  private JSONArray convertEvents(JSONObject jsonObject,
      String uuid, String version) {
    JSONArray asArrayEvents = new JSONArray();

    JSONArray events = jsonObject.getJSONArray(EVENT_LIST);
    for (int i = 0; i < events.length(); i++) {
      JSONObject event = new JSONObject().put(EVENT, events.getJSONObject(i));
      event.put(VES_UNIQUE_ID, uuid + "-" + i);
      event.put(VES_VERSION, version);
      asArrayEvents.put(overrideEvent(event));
    }
    return asArrayEvents;
  }

  private JSONArray convertEvent(JSONObject jsonObject, String uuid, String version) {
    jsonObject.put(VES_UNIQUE_ID, uuid);
    jsonObject.put(VES_VERSION, version);

    return new JSONArray().put(overrideEvent(jsonObject));
  }

  private JSONObject overrideEvent(JSONObject event) {
    JSONObject jsonObject = addCurrentTimeToEvent(event);
    if (settings.eventTransformingEnabled()) {
      try (FileReader fr = new FileReader("./etc/eventTransform.json")) {
        log.info("parse eventTransform.json");
        List<Event> events = new Gson().fromJson(fr, EVENT_LIST_TYPE);
        parseEventsJson(events, new ConfigProcessorAdapter(new ConfigProcessors(jsonObject)));
      } catch (IOException e) {
        log.error(COULD_NOT_FIND_FILE, e);
        throw new ApplicationException(COULD_NOT_FIND_FILE, e);
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

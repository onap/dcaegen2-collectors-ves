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

import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;


public class EventProcessor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);
    private static final String EVENT_LITERAL = "event";
    private static final String COMMON_EVENT_HEADER = "commonEventHeader";
    static final Type EVENT_LIST_TYPE = new TypeToken<List<Event>>() {
    }.getType();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MM dd yyyy hh:mm:ss z");

    private static HashMap<String, String[]> streamidHash = new HashMap<>();
    JSONObject event;

    public EventProcessor() {
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String[] list = CommonStartup.streamid.split("\\|");
        for (String aList : list) {
            String domain = aList.split("=")[0];

            String[] streamIdList = aList.substring(aList.indexOf('=') + 1).split(",");

            log.debug(String.format("Domain: %s streamIdList:%s", domain, Arrays.toString(streamIdList)));
            streamidHash.put(domain, streamIdList);
        }

    }

    @Override
    public void run() {

        try {

            event = CommonStartup.fProcessingInputQueue.take();

            while (event != null) {
                // As long as the producer is running we remove elements from
                // the queue.
                log.info("QueueSize:" + CommonStartup.fProcessingInputQueue.size() + "\tEventProcessor\tRemoving element: " + event);

                String uuid = event.get("VESuniqueId").toString();
                LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid);
                localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());

                String domain = event.getJSONObject(EVENT_LITERAL).getJSONObject(COMMON_EVENT_HEADER).getString("domain");
                log.debug("event.VESuniqueId" + event.get("VESuniqueId") + "event.commonEventHeader.domain:" + domain);
                String[] streamIdList = streamidHash.get(domain);
                log.debug("streamIdList:" + Arrays.toString(streamIdList));

                if (streamIdList.length == 0) {
                    log.error("No StreamID defined for publish - Message dropped" + event);
                } else {
                    sendEventsToStreams(streamIdList);
                }
                log.debug("Message published" + event);
                event = CommonStartup.fProcessingInputQueue.take();

            }
        } catch (InterruptedException e) {
            log.error("EventProcessor InterruptedException" + e.getMessage());
            Thread.currentThread().interrupt();
        }

    }

    public void overrideEvent() {
        // Set collector timestamp in event payload before publish
        addCurrentTimeToEvent(event);

        if (CommonStartup.eventTransformFlag == 1) {
            // read the mapping json file
            try (FileReader fr = new FileReader("./etc/eventTransform.json")) {
                log.info("parse eventTransform.json");
                List<Event> events = new Gson().fromJson(fr, EVENT_LIST_TYPE);
                parseEventsJson(events, new ConfigProcessorAdapter(new ConfigProcessors(event)));
            } catch (IOException e) {
                log.error("Couldn't find file ./etc/eventTransform.json" + e.toString());
            }
        }
        // Remove VESversion from event. This field is for internal use and must
        // be removed after use.
        if (event.has("VESversion"))
            event.remove("VESversion");

        log.debug("Modified event:" + event);

    }

    private void sendEventsToStreams(String[] streamIdList) {
        for (String aStreamIdList : streamIdList) {
            log.info("Invoking publisher for streamId:" + aStreamIdList);
            this.overrideEvent();
            EventPublisherHash.getInstance().sendEvent(event, aStreamIdList);

        }
    }

    private void addCurrentTimeToEvent(JSONObject event) {
        final Date currentTime = new Date();
        JSONObject collectorTimeStamp = new JSONObject().put("collectorTimeStamp", dateFormat.format(currentTime));
        JSONObject commonEventHeaderkey = event.getJSONObject(EVENT_LITERAL).getJSONObject(COMMON_EVENT_HEADER);
        commonEventHeaderkey.put("internalHeaderFields", collectorTimeStamp);
        event.getJSONObject(EVENT_LITERAL).put(COMMON_EVENT_HEADER, commonEventHeaderkey);
    }

    void parseEventsJson(List<Event> eventsTransform, ConfigProcessorAdapter configProcessorAdapter) {

        // load VESProcessors class at runtime
        for (Event eventTransform : eventsTransform) {
            JSONObject filterObj = new JSONObject(eventTransform.filter.toString());
            if (configProcessorAdapter.isFilterMet(filterObj)) {
                callProcessorsMethod(configProcessorAdapter, eventTransform.processors);
            }
        }
    }

    private void callProcessorsMethod(ConfigProcessorAdapter configProcessorAdapter, List<Processor> processors) {
        // call the processor method
        for (Processor processor : processors) {
            final String functionName = processor.functionName;
            final JSONObject args = new JSONObject(processor.args.toString());

            log.info(String.format("functionName==%s | args==%s", functionName, args));
            // reflect method call
            try {
                configProcessorAdapter.runConfigProcessorFunctionByName(functionName, args);
            } catch (ReflectiveOperationException e) {
                log.error("EventProcessor Exception" + e.getMessage() + e + e.getCause());
            }
        }
    }

    static class ConfigProcessorAdapter {
        private final ConfigProcessors configProcessors;

        ConfigProcessorAdapter(ConfigProcessors configProcessors) {
            this.configProcessors = configProcessors;
        }

        boolean isFilterMet(JSONObject parameter) {
            return configProcessors.isFilterMet(parameter);
        }

        void runConfigProcessorFunctionByName(String functionName, JSONObject parameter) throws ReflectiveOperationException {
            Method method = configProcessors.getClass().getDeclaredMethod(functionName, parameter.getClass());
            method.invoke(configProcessors, parameter);
        }
    }
}


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

package org.onap.dcae.restapi;

import static java.util.stream.StreamSupport.stream;
import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.badRequest;

import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.dcae.ApplicationException;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.EventSender;
import org.onap.dcae.common.VESLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VesRestController {

    private static final Logger log = LoggerFactory.getLogger(VesRestController.class);
    private static final String VES_EVENT_MESSAGE = "Received a VESEvent '%s', marked with unique identifier '%s', on api version '%s', from host: '%s'";
    private static final String EVENT_LIST = "eventList";
    private static final String EVENT = "event";
    private static final String VES_UNIQUE_ID = "VESuniqueId";
    private static final String VES_VERSION = "VESversion";
    private final ApplicationSettings applicationSettings;
    private final Logger metricsLog;
    private final Logger requestLogger;
    private EventSender eventSender;

    @Autowired
    VesRestController(ApplicationSettings applicationSettings,
                      @Qualifier("metricsLog") Logger metricsLog,
                      @Qualifier("incomingRequestsLogger") Logger incomingRequestsLogger,
                      @Qualifier("eventSender") EventSender eventSender) {
        this.applicationSettings = applicationSettings;
        this.metricsLog = metricsLog;
        this.requestLogger = incomingRequestsLogger;
        this.eventSender = eventSender;
    }

    @GetMapping("/")
    String mainPage() {
        return "Welcome to VESCollector";
    }

    @PostMapping(value = {"/eventListener/{version}"}, consumes = "application/json")
    ResponseEntity<String> event(@RequestBody String event, @PathVariable String version, HttpServletRequest request) {
        if (applicationSettings.isSupportedVersion(version)) {
            return process(event, version, request, EVENT);
        }
        return badRequest().contentType(MediaType.APPLICATION_JSON).body(String.format("API version %s is not supported", version));
    }


    @PostMapping(value = {"/eventListener/{version}/eventBatch"}, consumes = "application/json")
    ResponseEntity<String> events(@RequestBody String events, @PathVariable String version, HttpServletRequest request) {
        if (applicationSettings.isSupportedVersion(version)) {
            return process(events, version, request, EVENT_LIST);
        }
        return badRequest().contentType(MediaType.APPLICATION_JSON).body(String.format("API version %s is not supported", version));
    }

    private ResponseEntity<String> process(String events, String version, HttpServletRequest request, String type) {

        UUID uuid = UUID.randomUUID();
        JSONObject jsonObject = new JSONObject(events);
        setUpECOMPLoggingForRequest(uuid);

        requestLogger.info(String.format(VES_EVENT_MESSAGE, jsonObject, uuid, version, request.getRequestURI()));

        if (applicationSettings.jsonSchemaValidationEnabled()) {
            if (jsonObject.has(type)) {
                if (!conformsToSchema(jsonObject, version)) {
                    return errorResponse(ApiException.SCHEMA_VALIDATION_FAILED);
                }
            } else {
                return errorResponse(ApiException.INVALID_JSON_INPUT);
            }
        }
        send(convertToJSONArrayCommonFormat(jsonObject, request.getRequestURI(), uuid.toString(), version));
        // TODO call service and return status, replace CambriaClient, split event to single object and list of them
        return accepted().contentType(MediaType.APPLICATION_JSON).body("Accepted");
    }

    private ResponseEntity<String> errorResponse(ApiException noServerResources) {
        return ResponseEntity.status(noServerResources.httpStatusCode)
                .body(noServerResources.toJSON().toString());
    }

    private void send(JSONArray arrayOfEvents) {
        for (int i = 0; i < arrayOfEvents.length(); i++) {
            metricsLog.info("EVENT_PUBLISH_START");
            JSONObject object = (JSONObject) arrayOfEvents.get(i);
            setLoggingContext(object);
            eventSender.send(object);
            log.debug("Message published" + object);
        }
        log.debug("CommonStartup.handleEvents:EVENTS has been published successfully!");
        metricsLog.info("EVENT_PUBLISH_END");
    }

    private boolean conformsToSchema(JSONObject payload, String version) {
        try {
            JsonSchema schema = applicationSettings.jsonSchema(version);
            ProcessingReport report = schema.validate(JsonLoader.fromString(payload.toString()));
            if (report.isSuccess()) {
                return true;
            }
            log.warn("Schema validation failed for event: " + payload);
            stream(report.spliterator(), false).forEach(e -> log.warn(e.getMessage()));
            return false;
        } catch (Exception e) {
            throw new ApplicationException("Unable to validate against schema", e);
        }
    }

    private static JSONArray convertToJSONArrayCommonFormat(JSONObject jsonObject, String request,
                                                            String uuid, String version) {
        JSONArray asArrayEvents = new JSONArray();
        if (isBatchRequest(request)) {
            JSONArray events = jsonObject.getJSONArray(EVENT_LIST);
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = new JSONObject().put(EVENT, events.getJSONObject(i));
                event.put(VES_UNIQUE_ID, uuid + "-" + i);
                event.put(VES_VERSION, version);
                asArrayEvents.put(event);
            }
        } else {
            jsonObject.put(VES_UNIQUE_ID, uuid);
            jsonObject.put(VES_VERSION, version);
            asArrayEvents = new JSONArray().put(jsonObject);
        }
        return asArrayEvents;
    }

    private static void setUpECOMPLoggingForRequest(UUID uuid) {
        LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid);
        localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
    }

      private void setLoggingContext(JSONObject event) {
        LoggingContext localLC = VESLogger.getLoggingContextForThread(event.get(VES_UNIQUE_ID).toString());
        localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
        log.debug("event.VESuniqueId" + event.get(VES_UNIQUE_ID) + "event.commonEventHeader.domain:" + eventSender.getDomain(event));
    }

    private static boolean isBatchRequest(String request) {
        return request.contains("eventBatch");
    }
}
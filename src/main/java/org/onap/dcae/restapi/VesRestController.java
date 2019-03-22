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

import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VesRestController {
    private static final Logger log = LoggerFactory.getLogger(VesRestController.class);

    private final ApplicationSettings applicationSettings;
    private final Logger metricsLog;
    private final Logger errorLog;
    private final Logger incomingRequestsLogger;
    private EventSender eventSender;

    @Autowired
    VesRestController(ApplicationSettings applicationSettings,
                      @Qualifier("metricsLog") Logger metricsLog,
                      @Qualifier("errorLog") Logger errorLog,
                      @Qualifier("incomingRequestsLogger") Logger incomingRequestsLogger,
                      @Qualifier("eventSender") EventSender eventSender) {
        this.applicationSettings = applicationSettings;
        this.metricsLog = metricsLog;
        this.errorLog = errorLog;
        this.incomingRequestsLogger = incomingRequestsLogger;
        this.eventSender = eventSender;
    }

    @GetMapping("/")
    String mainPage() {
        return "Welcome to VESCollector";
    }

    @PostMapping(value = {"/eventListener/v1",
            "/eventListener/v1/eventBatch",
            "/eventListener/v2",
            "/eventListener/v2/eventBatch",
            "/eventListener/v3",
            "/eventListener/v3/eventBatch",
            "/eventListener/v4",
            "/eventListener/v4/eventBatch",
            "/eventListener/v5",
            "/eventListener/v5/eventBatch",
            "/eventListener/v7",
            "/eventListener/v7/eventBatch"}, consumes = "application/json")
    ResponseEntity<String> receiveEvent(@RequestBody String jsonPayload, HttpServletRequest httpServletRequest) {
        String request = httpServletRequest.getRequestURI();
        String version = extractVersion(request);

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonPayload);
        } catch (Exception e) {
            log.error("Invalid request cause: ", e);
            return ResponseEntity.badRequest().body(ApiException.INVALID_JSON_INPUT.toJSON().toString());
        }

        String uuid = setUpECOMPLoggingForRequest();
        incomingRequestsLogger.info(String.format(
                "Received a VESEvent '%s', marked with unique identifier '%s', on api version '%s', from host: '%s'",
                jsonObject, uuid, version, httpServletRequest.getRemoteHost()));

        if (applicationSettings.jsonSchemaValidationEnabled()) {
            if (isBatchRequest(request) && (jsonObject.has("eventList") && (!jsonObject.has("event")))) {
                if (!conformsToSchema(jsonObject, version)) {
                    return errorResponse(ApiException.SCHEMA_VALIDATION_FAILED);
                }
            } else if (!isBatchRequest(request) && (!jsonObject.has("eventList") && (jsonObject.has("event")))) {
                if (!conformsToSchema(jsonObject, version)) {
                    return errorResponse(ApiException.SCHEMA_VALIDATION_FAILED);
                }
            } else {
                return errorResponse(ApiException.INVALID_JSON_INPUT);
            }
        }

        JSONArray commonlyFormatted = convertToJSONArrayCommonFormat(jsonObject, request, uuid, version);

        if (!putEventsOnProcessingQueue(commonlyFormatted)) {
            errorLog.error("EVENT_RECEIPT_FAILURE: QueueFull " + ApiException.NO_SERVER_RESOURCES);
            return errorResponse(ApiException.NO_SERVER_RESOURCES);
        }
        return accepted()
                .contentType(MediaType.APPLICATION_JSON)
                .body("Accepted");
    }

    private String extractVersion(String httpServletRequest) {
        return httpServletRequest.split("/")[2];
    }

    private ResponseEntity<String> errorResponse(ApiException noServerResources) {
        return ResponseEntity.status(noServerResources.httpStatusCode)
                .body(noServerResources.toJSON().toString());
    }

    private boolean putEventsOnProcessingQueue(JSONArray arrayOfEvents) {
        for (int i = 0; i < arrayOfEvents.length(); i++) {
            metricsLog.info("EVENT_PUBLISH_START");
            JSONObject event = (JSONObject) arrayOfEvents.get(i);
            setLoggingContext(event);
            log.debug("event.VESuniqueId" + event.get("VESuniqueId") + "event.commonEventHeader.domain:" + eventSender.getDomain(event));
            eventSender.send(event);
        }
        log.debug("CommonStartup.handleEvents:EVENTS has been published successfully!");
        metricsLog.info("EVENT_PUBLISH_END");
        return true;
    }

    private boolean conformsToSchema(JSONObject payload, String version) {
        try {
            JsonSchema schema = applicationSettings.jsonSchema(version);
            ProcessingReport report = schema.validate(JsonLoader.fromString(payload.toString()));
            if (!report.isSuccess()) {
                log.warn("Schema validation failed for event: " + payload);
                stream(report.spliterator(), false).forEach(e -> log.warn(e.getMessage()));
                return false;
            }
            return report.isSuccess();
        } catch (Exception e) {
            throw new ApplicationException("Unable to validate against schema", e);
        }
    }

    private static JSONArray convertToJSONArrayCommonFormat(JSONObject jsonObject, String request,
                                                            String uuid, String version) {
        JSONArray asArrayEvents = new JSONArray();
        String vesUniqueIdKey = "VESuniqueId";
        String vesVersionKey = "VESversion";
        if (isBatchRequest(request)) {
            JSONArray events = jsonObject.getJSONArray("eventList");
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = new JSONObject().put("event", events.getJSONObject(i));
                event.put(vesUniqueIdKey, uuid + "-" + i);
                event.put(vesVersionKey, version);
                asArrayEvents.put(event);
            }
        } else {
            jsonObject.put(vesUniqueIdKey, uuid);
            jsonObject.put(vesVersionKey, version);
            asArrayEvents = new JSONArray().put(jsonObject);
        }
        return asArrayEvents;
    }

    private static String setUpECOMPLoggingForRequest() {
        final UUID uuid = UUID.randomUUID();
        LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid);
        localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
        return uuid.toString();
    }

    private static boolean isBatchRequest(String request) {
        return request.contains("eventBatch");
    }

      private void setLoggingContext(JSONObject event) {
        LoggingContext localLC = VESLogger.getLoggingContextForThread(event.get("VESuniqueId").toString());
        localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
    }
}
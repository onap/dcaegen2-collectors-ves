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

import static java.util.Optional.ofNullable;
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
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.CollectorSchemas;
import org.onap.dcae.commonFunction.VESLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VesRestController {

    private static final Logger LOG = LoggerFactory.getLogger(VesRestController.class);

    private static final String FALLBACK_VES_VERSION = "v5";

    @Autowired
    private ApplicationSettings collectorProperties;

    @Autowired
    private CollectorSchemas schemas;

    @Autowired
    @Qualifier("metriclog")
    private Logger metriclog;

    @Autowired
    @Qualifier("incomingRequestsLogger")
    private Logger incomingRequestsLogger;

    @Autowired
    @Qualifier("errorLog")
    private Logger errorLog;

    private LinkedBlockingQueue<JSONObject> inputQueue;
    private String version;

    @Autowired
    VesRestController(@Qualifier("incomingRequestsLogger") Logger incomingRequestsLogger,
                      @Qualifier("inputQueue") LinkedBlockingQueue<JSONObject> inputQueue) {
        this.incomingRequestsLogger = incomingRequestsLogger;
        this.inputQueue = inputQueue;
    }

    @GetMapping("/")
    String mainPage() {
        return "Welcome to VESCollector";
    }

    //refactor in next iteration
    @PostMapping(value = {"/eventListener/v1",
            "/eventListener/v1/eventBatch",
            "/eventListener/v2",
            "/eventListener/v2/eventBatch",
            "/eventListener/v3",
            "/eventListener/v3/eventBatch",
            "/eventListener/v4",
            "/eventListener/v4/eventBatch",
            "/eventListener/v5",
            "/eventListener/v5/eventBatch"}, consumes = "application/json")
    ResponseEntity<String> receiveEvent(@RequestBody String jsonPayload, HttpServletRequest httpServletRequest) {
        String request = httpServletRequest.getRequestURI();
        extractVersion(request);

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonPayload);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiException.INVALID_JSON_INPUT.toJSON().toString());
        }

        String uuid = setUpECOMPLoggingForRequest();
        incomingRequestsLogger.info(String.format(
                "Received a VESEvent '%s', marked with unique identifier '%s', on api version '%s', from host: '%s'",
                jsonObject, uuid, version, httpServletRequest.getRemoteHost()));

        if (collectorProperties.jsonSchemaValidationEnabled()) {
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
        return accepted().build();
    }

    private void extractVersion(String httpServletRequest) {
        version = httpServletRequest.split("/")[2];
    }

    private ResponseEntity<String> errorResponse(ApiException noServerResources) {
        return ResponseEntity.status(noServerResources.httpStatusCode)
                .body(noServerResources.toJSON().toString());
    }

    private boolean putEventsOnProcessingQueue(JSONArray arrayOfEvents) {
        for (int i = 0; i < arrayOfEvents.length(); i++) {
            metriclog.info("EVENT_PUBLISH_START");
            if (!inputQueue.offer((JSONObject) arrayOfEvents.get(i))) {
                return false;
            }
        }
        LOG.debug("CommonStartup.handleEvents:EVENTS has been published successfully!");
        metriclog.info("EVENT_PUBLISH_END");
        return true;
    }

    private boolean conformsToSchema(JSONObject payload, String version) {
        try {
            JsonSchema schema = ofNullable(schemas.getJSONSchemasMap(version).get(version))
                    .orElse(schemas.getJSONSchemasMap(version).get(FALLBACK_VES_VERSION));
            ProcessingReport report = schema.validate(JsonLoader.fromString(payload.toString()));
            if (!report.isSuccess()) {
                LOG.warn("Schema validation failed for event: " + payload);
                stream(report.spliterator(), false).forEach(e -> LOG.warn(e.getMessage()));
                return false;
            }
            return report.isSuccess();
        } catch (Exception e) {
            throw new RuntimeException("Unable to validate against schema", e);
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
}
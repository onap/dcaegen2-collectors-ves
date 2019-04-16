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

import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.badRequest;

import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.EventSender;
import org.onap.dcae.common.VESLogger;
import org.onap.dcae.common.EventUpdater;
import org.onap.dcae.common.HeaderUtils;
import org.onap.dcaegen2.services.sdk.standardization.header.CustomHeaderUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VesRestController {

    private static final String VES_EVENT_MESSAGE = "Received a VESEvent '%s', marked with unique identifier '%s', on api version '%s', from host: '%s'";
    private static final String EVENT_LIST = "eventList";
    private static final String EVENT = "event";
    private final ApplicationSettings settings;
    private final Logger requestLogger;
    private EventSender eventSender;
    private final HeaderUtils headerUtils;

    @Autowired
    VesRestController(ApplicationSettings settings,
        @Qualifier("incomingRequestsLogger") Logger incomingRequestsLogger,
        @Qualifier("eventSender") EventSender eventSender, HeaderUtils headerUtils) {
        this.settings = settings;
        this.requestLogger = incomingRequestsLogger;
        this.eventSender = eventSender;
        this.headerUtils = headerUtils;
    }

    @PostMapping(value = {"/eventListener/{version}"}, consumes = "application/json")
    ResponseEntity<String> event(@RequestBody String event, @PathVariable String version, HttpServletRequest request) {
        if (settings.isVersionSupported(version)) {
            return process(event, version, request, EVENT);
        }
        return badRequest().contentType(MediaType.APPLICATION_JSON).body(String.format("API version %s is not supported", version));
    }


    @PostMapping(value = {"/eventListener/{version}/eventBatch"}, consumes = "application/json")
    ResponseEntity<String> events(@RequestBody String events, @PathVariable String version, HttpServletRequest request) {
        if (settings.isVersionSupported(version)) {
            return process(events, version, request, EVENT_LIST);
        }
        return badRequest().contentType(MediaType.APPLICATION_JSON).body(String.format("API version %s is not supported", version));
    }

    private ResponseEntity<String> process(String events, String version, HttpServletRequest request, String type) {
        CustomHeaderUtils headerUtils = createHeaderUtils(version, request);
        if(headerUtils.isOkCustomHeaders()){
            JSONObject jsonObject = new JSONObject(events);

            EventValidator eventValidator = new EventValidator(settings);
            Optional<ResponseEntity<String>> validationResult = eventValidator.validate(jsonObject, type, version);

            if (validationResult.isPresent()){
                return validationResult.get();
            }
            JSONArray arrayOfEvents = new EventUpdater(settings).convert(jsonObject,version, generateUUID(version, request.getRequestURI(), jsonObject), type);
            eventSender.send(arrayOfEvents);
            // TODO call service and return status, replace CambriaClient, split event to single object and list of them
            return accepted().headers(this.headerUtils.fillHeaders(headerUtils.getRspCustomHeader()))
                .contentType(MediaType.APPLICATION_JSON).body("Accepted");
        }
        return badRequest().body(String.format(ApiException.INVALID_CUSTOM_HEADER.toString()));
    }

    private CustomHeaderUtils createHeaderUtils(String version, HttpServletRequest request){
        return  new CustomHeaderUtils(version.toLowerCase().replace("v", ""),
            headerUtils.extractHeaders(request),
            headerUtils.getApiVerFilePath("api_version_config.json"),
            headerUtils.getRestApiIdentify(request.getRequestURI()));

    }

    private UUID generateUUID(String version, String uri, JSONObject jsonObject) {
        UUID uuid = UUID.randomUUID();
        setUpECOMPLoggingForRequest(uuid);
        requestLogger.info(String.format(VES_EVENT_MESSAGE, jsonObject, uuid, version, uri));
        return uuid;
    }

    private static void setUpECOMPLoggingForRequest(UUID uuid) {
        LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid);
        localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
    }
}
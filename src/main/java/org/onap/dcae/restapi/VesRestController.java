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

import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import org.json.JSONObject;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.EventSender;
import org.onap.dcae.common.EventUpdater;
import org.onap.dcae.common.HeaderUtils;
import org.onap.dcae.common.VESLogger;
import org.onap.dcae.common.model.StndDefinedNamespaceParameterHasEmptyValueException;
import org.onap.dcae.common.model.StndDefinedNamespaceParameterNotDefinedException;
import org.onap.dcae.common.model.VesEvent;
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

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.badRequest;

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

    private ResponseEntity<String> process(String payload, String version, HttpServletRequest request, String type) {
        CustomHeaderUtils headerUtils = createHeaderUtils(version, request);
        if (headerUtils.isOkCustomHeaders()) {
            try {
                final VesEvent vesEvent = new VesEvent(new JSONObject(payload));
                validateEvent(version, type, vesEvent);
                List<VesEvent> vesEvents = transformEvent(version, request, type, vesEvent);
                eventSender.send(vesEvents);
            } catch (EventValidatorException e) {
                return ResponseEntity.status(e.getApiException().httpStatusCode)
                        .body(e.getApiException().toJSON().toString());
            } catch (StndDefinedNamespaceParameterNotDefinedException e) {
                return ResponseEntity.status(ApiException.MISSING_NAMESPACE_PARAMETER.httpStatusCode)
                        .body(ApiException.MISSING_NAMESPACE_PARAMETER.toJSON().toString());
            } catch (StndDefinedNamespaceParameterHasEmptyValueException e) {
                return ResponseEntity.status(ApiException.MISSING_NAMESPACE_PARAMETER.httpStatusCode)
                        .body(ApiException.EMPTY_NAMESPACE_PARAMETER.toJSON().toString());
            }

            // TODO call service and return status, replace CambriaClient, split event to single object and list of them
            return accepted().headers(this.headerUtils.fillHeaders(headerUtils.getRspCustomHeader()))
                    .contentType(MediaType.APPLICATION_JSON).body("Accepted");
        }
        return badRequest().body(String.format(ApiException.INVALID_CUSTOM_HEADER.toString()));
    }

    private CustomHeaderUtils createHeaderUtils(String version, HttpServletRequest request) {
        return new CustomHeaderUtils(version.toLowerCase().replace("v", ""),
                headerUtils.extractHeaders(request),
                headerUtils.getApiVerFilePath("api_version_config.json"),
                headerUtils.getRestApiIdentify(request.getRequestURI()));

    }

    private void validateEvent(String version, String type, VesEvent vesEvent) throws EventValidatorException {
        EventValidator eventValidator = new EventValidator(settings);
        eventValidator.validate(vesEvent, type, version);
    }

    private List<VesEvent> transformEvent(String version, HttpServletRequest request, String type, VesEvent vesEvent) {
        return new EventUpdater(settings).convert(
                vesEvent, version, generateUUID(version, request.getRequestURI(), vesEvent), type);
    }

    private UUID generateUUID(String version, String uri, VesEvent vesEvent) {
        UUID uuid = UUID.randomUUID();
        setUpECOMPLoggingForRequest(uuid);
        requestLogger.info(String.format(VES_EVENT_MESSAGE, vesEvent.asJsonObject(), uuid, version, uri));
        return uuid;
    }

    private static void setUpECOMPLoggingForRequest(UUID uuid) {
        LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid);
        localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
    }
}

/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2020-2021 Nokia. All rights reserved.
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

import org.json.JSONObject;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.EventSender;
import org.onap.dcae.common.EventUpdater;
import org.onap.dcae.common.HeaderUtils;
import org.onap.dcae.common.model.BackwardsCompatibilityException;
import org.onap.dcae.common.model.InternalException;
import org.onap.dcae.common.model.PayloadToLargeException;
import org.onap.dcae.common.model.StndDefinedNamespaceParameterHasEmptyValueException;
import org.onap.dcae.common.model.StndDefinedNamespaceParameterNotDefinedException;
import org.onap.dcae.common.model.VesEvent;
import org.onap.dcae.common.validator.GeneralEventValidator;
import org.onap.dcae.common.validator.StndDefinedDataValidator;
import org.onap.dcaegen2.services.sdk.standardization.header.CustomHeaderUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

import static org.onap.dcae.common.validator.BatchEventValidator.executeBatchEventValidation;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.status;

@RestController
public class VesRestController {

    private static final String VES_EVENT_MESSAGE = "Received a VESEvent '%s', marked with unique identifier '%s', on api version '%s', from host: '%s'";
    private static final String EVENT_LIST = "eventList";
    private static final String EVENT = "event";
    private final ApplicationSettings settings;
    private final Logger requestLogger;
    private final Logger logger;
    private EventSender eventSender;
    private final HeaderUtils headerUtils;
    private final GeneralEventValidator generalEventValidator;
    private final EventUpdater eventUpdater;
    private final StndDefinedDataValidator stndDefinedValidator;

    @Autowired
    VesRestController(ApplicationSettings settings, @Qualifier("incomingRequestsLogger") Logger incomingRequestsLogger,
                      @Qualifier("errorLog") Logger logger, @Qualifier("eventSender") EventSender eventSender, HeaderUtils headerUtils,
                      StndDefinedDataValidator stndDefinedDataValidator) {
        this.settings = settings;
        this.requestLogger = incomingRequestsLogger;
        this.logger = logger;
        this.eventSender = eventSender;
        this.headerUtils = headerUtils;
        this.stndDefinedValidator = stndDefinedDataValidator;
        this.generalEventValidator = new GeneralEventValidator(settings);
        this.eventUpdater = new EventUpdater(settings);
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
            final VesEvent vesEvent = new VesEvent(new JSONObject(payload));
            final String requestURI = request.getRequestURI();
            return handleEvent(vesEvent, version, type, headerUtils, requestURI);
        }
        return badRequest().body(ApiException.INVALID_CUSTOM_HEADER.toString());
    }

    private ResponseEntity<String> handleEvent(VesEvent vesEvent, String version, String type, CustomHeaderUtils headerUtils, String requestURI) {
        try {
            generalEventValidator.validate(vesEvent, type, version);
            List<VesEvent> vesEvents = transformEvent(vesEvent, type, version, requestURI);
            executeStndDefinedValidation(vesEvents);
            executeBatchEventValidation(vesEvents);
            HttpStatus httpStatus = eventSender.send(vesEvents);
            return status(httpStatus).contentType(MediaType.APPLICATION_JSON).body("Successfully send event");
        } catch (EventValidatorException e) {
            logger.error(e.getMessage());
            return status(e.getApiException().httpStatusCode)
                    .body(e.getApiException().toJSON().toString());
        } catch (StndDefinedNamespaceParameterNotDefinedException e) {
            return status(ApiException.MISSING_NAMESPACE_PARAMETER.httpStatusCode)
                    .body(ApiException.MISSING_NAMESPACE_PARAMETER.toJSON().toString());
        } catch (StndDefinedNamespaceParameterHasEmptyValueException e) {
            return status(ApiException.MISSING_NAMESPACE_PARAMETER.httpStatusCode)
                    .body(ApiException.EMPTY_NAMESPACE_PARAMETER.toJSON().toString());
        } catch (InternalException e) {
            return status(ApiException.SERVICE_UNAVAILABLE.httpStatusCode)
                    .body(e.getApiException().toJSON().toString());
        } catch (PayloadToLargeException e) {
            return status(ApiException.PAYLOAD_TO_LARGE.httpStatusCode)
                    .body(ApiException.PAYLOAD_TO_LARGE.toJSON().toString());
        } catch (BackwardsCompatibilityException e) {
            return status(ApiException.INTERNAL_SERVER_ERROR.httpStatusCode)
                    .body(ApiException.INTERNAL_SERVER_ERROR.toJSON().toString());
        }
    }

    private void executeStndDefinedValidation(List<VesEvent> vesEvents) {
        if (settings.getExternalSchemaValidationCheckflag()) {
            vesEvents.forEach(stndDefinedValidator::validate);
        }
    }

    private CustomHeaderUtils createHeaderUtils(String version, HttpServletRequest request) {
        return new CustomHeaderUtils(version.toLowerCase().replace("v", ""),
                headerUtils.extractHeaders(request),
                settings.getApiVersionDescriptionFilepath(),
                headerUtils.getRestApiIdentify(request.getRequestURI()));
    }

    private List<VesEvent> transformEvent(VesEvent vesEvent, String type, String version, String requestURI) {
        return this.eventUpdater.convert(vesEvent, version, generateUUID(vesEvent, version, requestURI), type);
    }

    private UUID generateUUID(VesEvent vesEvent, String version, String uri) {
        UUID uuid = UUID.randomUUID();
        requestLogger.info(String.format(VES_EVENT_MESSAGE, vesEvent.asJsonObject(), uuid, version, uri));
        return uuid;
    }
}

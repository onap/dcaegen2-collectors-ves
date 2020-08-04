/*
 * ============LICENSE_START=======================================================
 * VES
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
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
import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.EventSender;
import org.onap.dcae.common.EventUpdater;
import org.onap.dcae.common.HeaderUtils;
import org.onap.dcae.common.StndDefinedDataValidator;
import org.onap.dcae.common.VESLogger;
import org.onap.dcaegen2.services.sdk.standardization.header.CustomHeaderUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.accepted;
import static org.springframework.http.ResponseEntity.badRequest;

@Service
public class EventValidator {

    private static final String VES_EVENT_MESSAGE = "Received a VESEvent '%s', marked with unique identifier '%s', on api version '%s', from host: '%s'";
    private final ApplicationSettings settings;
    private final Logger incomingRequestsLogger;
    private EventSender eventSender;
    private final HeaderUtils headerUtils;
    private final StndDefinedDataValidator stndDefinedValidator;
    private  final GeneralEventValidator generalEventValidator;

    @Autowired
    public EventValidator(ApplicationSettings settings, @Qualifier("incomingRequestsLogger") Logger incomingRequestsLogger,
                          @Qualifier("eventSender") EventSender eventSender, HeaderUtils headerUtils, StndDefinedDataValidator stndDefinedValidator, GeneralEventValidator generalEventValidator) {
        this.settings = settings;
        this.incomingRequestsLogger = incomingRequestsLogger;
        this.eventSender = eventSender;
        this.headerUtils = headerUtils;
        this.stndDefinedValidator = stndDefinedValidator;
        this.generalEventValidator = generalEventValidator;
    }


    public ResponseEntity<String> process(String events, String version, HttpServletRequest request, String type) throws JsonProcessingException {
        CustomHeaderUtils headerUtils = createHeaderUtils(version, request);
        if (headerUtils.isOkCustomHeaders()) {
            JSONObject jsonObject = new JSONObject(events);
            Optional<ResponseEntity<String>> validationResult = generalEventValidator.validate(jsonObject, type, version);

            if (validationResult.isPresent()) {
                return validationResult.get();
            }
//            else {
//                if (responseEntity.getStatusCode() == HttpStatus.ACCEPTED) {
//                    validationResult = stndDefinedValidator.validate(events);
//                }
//            }
            JSONArray arrayOfEvents = new EventUpdater(settings).convert(jsonObject, version, generateUUID(version, request.getRequestURI(), jsonObject), type);
            eventSender.send(arrayOfEvents);
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

    private UUID generateUUID(String version, String uri, JSONObject jsonObject) {
        UUID uuid = UUID.randomUUID();
        setUpECOMPLoggingForRequest(uuid);
        incomingRequestsLogger.info(String.format(VES_EVENT_MESSAGE, jsonObject, uuid, version, uri));
        return uuid;
    }


    private static void setUpECOMPLoggingForRequest(UUID uuid) {
        LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid);
        localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
    }
}

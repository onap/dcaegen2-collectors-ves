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

import static org.springframework.http.ResponseEntity.badRequest;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.onap.dcae.ApplicationSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VesRestController {

    private static final String EVENT_LIST = "eventList";
    private static final String EVENT = "event";
    private final ApplicationSettings settings;
    private final EventValidator eventValidator;

    @Autowired
    VesRestController(ApplicationSettings settings, EventValidator eventValidator) {
        this.settings = settings;
        this.eventValidator = eventValidator;
    }

    @PostMapping(value = {"/eventListener/{version}"}, consumes = "application/json")
    ResponseEntity<String> event(@RequestBody String event, @PathVariable String version, HttpServletRequest request) throws JsonProcessingException {
        if (settings.isVersionSupported(version)) {
            return eventValidator.process(event, version, request, EVENT);
        }
        return badRequest().contentType(MediaType.APPLICATION_JSON).body(String.format("API version %s is not supported", version));
    }

    @PostMapping(value = {"/eventListener/{version}/eventBatch"}, consumes = "application/json")
    ResponseEntity<String> events(@RequestBody String events, @PathVariable String version, HttpServletRequest request) throws JsonProcessingException {
        if (settings.isVersionSupported(version)) {
            return eventValidator.process(events, version, request, EVENT_LIST);
        }
        return badRequest().contentType(MediaType.APPLICATION_JSON).body(String.format("API version %s is not supported", version));
    }

}
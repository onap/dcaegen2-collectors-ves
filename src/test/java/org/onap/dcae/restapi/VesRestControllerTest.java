/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.s
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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.EventSender;
import org.onap.dcae.common.EventTransformation;
import org.onap.dcae.common.HeaderUtils;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VesRestControllerTest {

    private static final String EVENT_TRANSFORM_FILE_PATH = "/eventTransform.json";

    @InjectMocks
    VesRestController vesRestController;

    @Mock
    ApplicationSettings applicationSettings;

    @Mock
    Logger logger;

    @Mock
    EventSender eventSender;

    @Mock
    HeaderUtils headerUtils;

    @Test
    public void shouldReportThatApiVersionIsNotSupported() {
        // given
        when(applicationSettings.isVersionSupported("v20")).thenReturn(false);
        MockHttpServletRequest request = givenMockHttpServletRequest();

        // when
        final ResponseEntity<String> event = vesRestController.event("", "v20", request);

        // then
        assertThat(event.getStatusCodeValue()).isEqualTo(400);
        assertThat(event.getBody()).isEqualTo("API version v20 is not supported");
        verify(eventSender, never()).send(any(JSONArray.class));
    }

    @Test
    public void shouldTransformEventAccordingToEventTransformFile() throws IOException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();

        String validEvent = new String(
                Files.readAllBytes(Paths.get(this.getClass().getResource("/ves7_valid.json").getPath()))
        );

        //when
        final ResponseEntity<String> response = vesRestController.event(validEvent, "v7", request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(202);
        assertThat(response.getBody()).isEqualTo("Accepted");
        verifyThatTransformedEventWasSend(eventSender, validEvent);
    }

    private void configureEventTransformations() throws IOException {
        final List<EventTransformation> eventTransformations = loadEventTransformations();
        when(applicationSettings.isVersionSupported("v7")).thenReturn(true);
        when(applicationSettings.eventTransformingEnabled()).thenReturn(true);
        when(applicationSettings.getEventTransformations()).thenReturn(eventTransformations);
    }

    private void configureHeadersForEventListener() {
        when(headerUtils.getRestApiIdentify(anyString())).thenReturn("eventListener");
        when(headerUtils.getApiVerFilePath(anyString())).thenReturn(
                this.getClass().getResource("/api_version_config.json").getPath()
        );
    }

    private void verifyThatTransformedEventWasSend(EventSender eventSender, String eventBeforeTransformation) {
        // event before transformation
        assertThat(eventBeforeTransformation).contains("\"version\": \"4.0.1\"");
        assertThat(eventBeforeTransformation).contains("\"faultFieldsVersion\": \"4.0\"");

        ArgumentCaptor<JSONArray> argument = ArgumentCaptor.forClass(JSONArray.class);
        verify(eventSender).send(argument.capture());

        final String transformedEvent = argument.getValue().toString();

        // event after transformation
        assertThat(transformedEvent).contains("\"priority\":\"High\",\"version\":3,");
        assertThat(transformedEvent).contains(",\"faultFieldsVersion\":3,\"specificProblem");
    }

    @NotNull
    private MockHttpServletRequest givenMockHttpServletRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    private List<EventTransformation> loadEventTransformations() throws IOException {
        Type EVENT_TRANSFORM_LIST_TYPE = new TypeToken<List<EventTransformation>>() {
        }.getType();

        try (FileReader fr = new FileReader(this.getClass().getResource(EVENT_TRANSFORM_FILE_PATH).getPath())) {
            return new Gson().fromJson(fr, EVENT_TRANSFORM_LIST_TYPE);
        }
    }
}

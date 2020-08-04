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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.networknt.schema.JsonSchema;
import io.vavr.collection.HashMap;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.JSonSchemasSupplier;
import org.onap.dcae.common.EventSender;
import org.onap.dcae.common.EventTransformation;
import org.onap.dcae.common.HeaderUtils;
import org.onap.dcae.common.JsonDataLoader;
import org.onap.dcae.common.StndDefinedDataValidator;
import org.onap.dcae.common.publishing.EventPublisher;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VesRestControllerTest {

    private static final String EVENT_TRANSFORM_FILE_PATH = "/eventTransform.json";
    private static final String ACCEPTED = "Accepted";
    private static final String VERSION_V7 = "v7";
    public static final String VES_FAULT_TOPIC = "ves-fault";
    public static final String VES_3_GPP_FAULT_SUPERVISION_TOPIC = "ves-3gpp-fault-supervision";

    private VesRestController vesRestController;

    @Mock
    private ApplicationSettings applicationSettings;

    @Mock
    private Logger logger;

    @Mock
    private HeaderUtils headerUtils;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private StndDefinedDataValidator stndDefinedDataValidator;

    @Before
    public void setUp(){

        final HashMap<String, String[]> streamIds = HashMap.of(
                "fault", new String[]{VES_FAULT_TOPIC},
                "3GPP-FaultSupervision", new String[]{VES_3_GPP_FAULT_SUPERVISION_TOPIC}
        );
        this.vesRestController = new VesRestController(
                applicationSettings, logger, new EventSender(eventPublisher, streamIds),headerUtils,
                stndDefinedDataValidator);
    }

    @Test
    public void shouldReportThatApiVersionIsNotSupported() throws JsonProcessingException {
        // given
        when(applicationSettings.isVersionSupported("v20")).thenReturn(false);
        MockHttpServletRequest request = givenMockHttpServletRequest();

        // when
        final ResponseEntity<String> event = vesRestController.event("", "v20", request);

        // then
        assertThat(event.getStatusCodeValue()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(event.getBody()).isEqualTo("API version v20 is not supported");
        verifyThatEventWasNotSend();
    }

    @Test
    public void shouldTransformEventAccordingToEventTransformFile() throws IOException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();

        String validEvent = JsonDataLoader.loadContent("/ves7_valid_30_1_1_event.json");

        //when
        final ResponseEntity<String> response = vesRestController.event(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.SC_ACCEPTED);
        assertThat(response.getBody()).isEqualTo(ACCEPTED);
        verifyThatTransformedEventWasSend(eventPublisher, validEvent);
    }


    @Test
    public void shouldSendBatchOfEvents() throws IOException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();

        String validEvent = JsonDataLoader.loadContent("/ves7_batch_valid.json");

        //when
        final ResponseEntity<String> response = vesRestController.events(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.SC_ACCEPTED);
        assertThat(response.getBody()).isEqualTo(ACCEPTED);
        verify(eventPublisher, times(2)).sendEvent(any(),any());
    }

    @Test
    public void shouldSendStndDomainEventIntoDomainStream() throws IOException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();
        configureSchemasSupplierForStndDefineEvent();

        String validEvent = JsonDataLoader.loadContent("/ves_stdnDefined_valid.json");

        //when
        final ResponseEntity<String> response = vesRestController.event(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.SC_ACCEPTED);
        assertThat(response.getBody()).isEqualTo(ACCEPTED);
        verify(eventPublisher).sendEvent(any(),eq(VES_3_GPP_FAULT_SUPERVISION_TOPIC));
    }


    @Test
    public void shouldReportThatStndDomainEventHasntGotNamespaceParameter() throws IOException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();
        configureSchemasSupplierForStndDefineEvent();

        String validEvent = JsonDataLoader.loadContent("/ves_stdnDefined_missing_namespace_invalid.json");

        //when
        final ResponseEntity<String> response = vesRestController.event(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        verifyErrorResponse(
                response,
                "SVC2006",
                "Mandatory input attribute event.commonEventHeader.stndDefinedNamespace is missing from request"
        );
        verifyThatEventWasNotSend();
    }

    @Test
    public void shouldReportThatStndDomainEventNamespaceParameterIsEmpty() throws IOException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();
        configureSchemasSupplierForStndDefineEvent();

        String validEvent = JsonDataLoader.loadContent("/ves_stdnDefined_empty_namespace_invalid.json");

        //when
        final ResponseEntity<String> response = vesRestController.event(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        verifyErrorResponse(
                response,
                "SVC2006",
                "Mandatory input attribute event.commonEventHeader.stndDefinedNamespace is empty in request"
        );
        verifyThatEventWasNotSend();
    }

    @Test
    public void shouldNotSendStndDomainEventWhenTopicCannotBeFoundInConfiguration() throws IOException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();

        String validEvent = JsonDataLoader.loadContent("/ves_stdnDefined_valid_unknown_topic.json");

        //when
        final ResponseEntity<String> response = vesRestController.event(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.SC_ACCEPTED);
        assertThat(response.getBody()).isEqualTo(ACCEPTED);
        verifyThatEventWasNotSend();
    }

    private void verifyThatEventWasNotSend() {
        verify(eventPublisher, never()).sendEvent(any(), any());
    }

    private void configureSchemasSupplierForStndDefineEvent() {
        String collectorSchemaFile = "{\"v7\":\"./etc/CommonEventFormat_30.2_ONAP.json\"}";
        final io.vavr.collection.Map<String, JsonSchema> loadedJsonSchemas = new JSonSchemasSupplier().loadJsonSchemas(collectorSchemaFile);

        when(applicationSettings.eventSchemaValidationEnabled()).thenReturn(true);
        when(applicationSettings.jsonSchema(eq(VERSION_V7))).thenReturn(loadedJsonSchemas.get(VERSION_V7).get());
    }

    private void verifyErrorResponse(ResponseEntity<String> response, String messageId, String messageText) throws com.fasterxml.jackson.core.JsonProcessingException {
        final Map<String, String> errorDetails = fetchErrorDetails(response);
        assertThat(errorDetails).containsEntry("messageId", messageId);
        assertThat(errorDetails).containsEntry("text", messageText);
    }

    private Map<String, String> fetchErrorDetails(ResponseEntity<String> response) throws com.fasterxml.jackson.core.JsonProcessingException {
        final String body = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Map<String, Map<String,String>>> map = mapper.readValue(body, Map.class);
        return map.get("requestError").get("ServiceException");
    }

    private void configureEventTransformations() throws IOException {
        final List<EventTransformation> eventTransformations = loadEventTransformations();
        when(applicationSettings.isVersionSupported(VERSION_V7)).thenReturn(true);
        when(applicationSettings.eventTransformingEnabled()).thenReturn(true);
        when(applicationSettings.getEventTransformations()).thenReturn(eventTransformations);
    }

    private void configureHeadersForEventListener() {
        when(headerUtils.getRestApiIdentify(anyString())).thenReturn("eventListener");
        when(headerUtils.getApiVerFilePath(anyString())).thenReturn(
                this.getClass().getResource("/api_version_config.json").getPath()
        );
    }

    private void verifyThatTransformedEventWasSend(EventPublisher eventPublisher, String eventBeforeTransformation) {
        // event before transformation
        assertThat(eventBeforeTransformation).contains("\"version\": \"4.0.1\"");
        assertThat(eventBeforeTransformation).contains("\"faultFieldsVersion\": \"4.0\"");

        ArgumentCaptor<JSONObject> argument = ArgumentCaptor.forClass(JSONObject.class);
        ArgumentCaptor<String> domain = ArgumentCaptor.forClass(String.class);
        verify(eventPublisher).sendEvent(argument.capture(), domain.capture());

        final String transformedEvent = argument.getValue().toString();
        final String eventSentAtTopic = domain.getValue();

        // event after transformation
        assertThat(transformedEvent).contains("\"priority\":\"High\",\"version\":3,");
        assertThat(transformedEvent).contains(",\"faultFieldsVersion\":3,\"specificProblem");
        assertThat(eventSentAtTopic).isEqualTo(VES_FAULT_TOPIC);
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

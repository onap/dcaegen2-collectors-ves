/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2020-2021 Nokia. All rights reserved.
 * Copyright (C) 2023 AT&T Intellectual Property. All rights reserved.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.networknt.schema.JsonSchema;
import io.vavr.collection.HashMap;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.JSonSchemasSupplier;
import org.onap.dcae.common.EventSender;
import org.onap.dcae.common.EventTransformation;
import org.onap.dcae.common.HeaderUtils;
import org.onap.dcae.common.JsonDataLoader;
import org.onap.dcae.common.model.InternalException;
import org.onap.dcae.common.model.PayloadToLargeException;
import org.onap.dcae.common.publishing.DMaaPEventPublisher;
import org.onap.dcae.common.validator.StndDefinedDataValidator;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;

@ExtendWith(MockitoExtension.class)
public class VesRestControllerTest {

    private static final String EVENT_TRANSFORM_FILE_PATH = "/eventTransform.json";
    private static final String ACCEPTED = "Successfully send event";
    private static final String VERSION_V7 = "v7";
    static final String VES_FAULT_TOPIC = "ves-fault";
    static final String VES_3_GPP_FAULT_SUPERVISION_TOPIC = "ves-3gpp-fault-supervision";

    private VesRestController vesRestController;

    @Mock
    private ApplicationSettings applicationSettings;

    @Mock
    private Logger logger;

    @Mock
    private Logger errorLogger;

    @Mock
    private HeaderUtils headerUtils;

    @Mock
    private DMaaPEventPublisher eventPublisher;

    @Mock
    private StndDefinedDataValidator stndDefinedDataValidator;

    @BeforeEach
    void setUp(){
        final HashMap<String, String> streamIds = HashMap.of(
                "fault", VES_FAULT_TOPIC,
                "3GPP-FaultSupervision", VES_3_GPP_FAULT_SUPERVISION_TOPIC
        );
        this.vesRestController = new VesRestController(applicationSettings, logger,
                errorLogger, new EventSender(eventPublisher, streamIds), headerUtils, stndDefinedDataValidator);
    }

    @Test
    void shouldReportThatApiVersionIsNotSupported() {
        // given
        when(applicationSettings.isVersionSupported("v20")).thenReturn(false);
        MockHttpServletRequest request = givenMockHttpServletRequest();

        // when
        final ResponseEntity<String> event = vesRestController.event("", "v20", request);

        // then
        assertThat(event.getStatusCodeValue()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(event.getBody()).isEqualTo("API version v20 is not supported");
        verifyThatEventWasNotSend();
    }

    @Test
    void shouldTransformEventAccordingToEventTransformFile() throws IOException, URISyntaxException{
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();
        String validEvent = JsonDataLoader.loadContent("/ves7_valid_30_1_1_event.json");
        when(eventPublisher.sendEvent(any(), any())).thenReturn((HttpStatus.OK));

        //when
        final ResponseEntity<String> response = vesRestController.event(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getBody()).isEqualTo(ACCEPTED);
        verifyThatTransformedEventWasSend(eventPublisher, validEvent);
    }


    @Test
    void shouldSendBatchEvent() throws IOException, URISyntaxException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();

        String validEvent = JsonDataLoader.loadContent("/ves7_batch_valid.json");
        when(eventPublisher.sendEvent(any(), any())).thenReturn(HttpStatus.OK);
        //when
        final ResponseEntity<String> response = vesRestController.events(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getBody()).isEqualTo(ACCEPTED);
        verify(eventPublisher, times(1)).sendEvent(any(),any());
    }

    @Test
    void shouldSendStndDomainEventIntoDomainStream() throws IOException, URISyntaxException{
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();
        configureSchemasSupplierForStndDefineEvent();

        String validEvent = JsonDataLoader.loadContent("/ves_stdnDefined_valid.json");
        when(eventPublisher.sendEvent(any(), any())).thenReturn(HttpStatus.OK);

        //when
        final ResponseEntity<String> response = vesRestController.event(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getBody()).isEqualTo(ACCEPTED);
        verify(eventPublisher).sendEvent(any(),eq(VES_3_GPP_FAULT_SUPERVISION_TOPIC));
    }


    @Test
    void shouldReportThatStndDomainEventHasntGotNamespaceParameter() throws IOException, URISyntaxException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();
        configureSchemasSupplierForStndDefineEvent();

        String validEvent = JsonDataLoader.loadContent("/ves_stdnDefined_missing_namespace_invalid.json");

        //when
        final ResponseEntity<String> response = vesRestController.event(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        verifyErrorResponse(
                response,
                "SVC2006",
                "Mandatory input %1 %2 is missing from request",
                List.of("attribute", "event.commonEventHeader.stndDefinedNamespace")
        );
        verifyThatEventWasNotSend();
    }

    @Test
    void shouldReportThatStndDomainEventNamespaceParameterIsEmpty() throws IOException, URISyntaxException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();
        configureSchemasSupplierForStndDefineEvent();

        String validEvent = JsonDataLoader.loadContent("/ves_stdnDefined_empty_namespace_invalid.json");

        //when
        final ResponseEntity<String> response = vesRestController.event(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        verifyErrorResponse(
                response,
                "SVC2006",
                "Mandatory input %1 %2 is empty in request",
                List.of("attribute", "event.commonEventHeader.stndDefinedNamespace")
        );
        verifyThatEventWasNotSend();
    }

    @Test
    void shouldNotSendStndDomainEventWhenTopicCannotBeFoundInConfiguration() throws IOException, URISyntaxException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();
        String validEvent = JsonDataLoader.loadContent("/ves_stdnDefined_valid_unknown_topic.json");

        //when
        final ResponseEntity<String> response = vesRestController.event(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        verifyThatEventWasNotSend();
    }

    @Test
    void shouldExecuteStndDefinedValidationWhenFlagIsOnTrue() throws IOException, URISyntaxException{
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();
        String validEvent = JsonDataLoader.loadContent("/ves7_batch_with_stndDefined_valid.json");
        when(applicationSettings.getExternalSchemaValidationCheckflag()).thenReturn(true);
        when(eventPublisher.sendEvent(any(), any())).thenReturn(HttpStatus.OK);
        //when
        final ResponseEntity<String> response = vesRestController.events(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getBody()).isEqualTo(ACCEPTED);
        verify(stndDefinedDataValidator, times(2)).validate(any());
    }

    @Test
    void shouldNotExecuteStndDefinedValidationWhenFlagIsOnFalse() throws IOException, URISyntaxException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();
        String validEvent = JsonDataLoader.loadContent("/ves7_batch_with_stndDefined_valid.json");
        when(applicationSettings.getExternalSchemaValidationCheckflag()).thenReturn(false);
        when(eventPublisher.sendEvent(any(), any())).thenReturn(HttpStatus.OK);

        //when
        final ResponseEntity<String> response = vesRestController.events(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getBody()).isEqualTo(ACCEPTED);
        verify(stndDefinedDataValidator, times(0)).validate(any());
    }

    @Test
    void shouldReturn413WhenPayloadIsTooLarge() throws IOException, URISyntaxException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();
        when(eventPublisher.sendEvent(any(), any())).thenThrow(new PayloadToLargeException());
        String validEvent = JsonDataLoader.loadContent("/ves7_valid_30_1_1_event.json");

        //when
        final ResponseEntity<String> response = vesRestController.event(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        verifyErrorResponse(
                response,
                "SVC2000",
                "The following service error occurred: %1. Error code is %2",
                List.of("Request Entity Too Large","413")
        );
    }

    @ParameterizedTest
    @MethodSource("errorsCodeAndResponseBody")
    void shouldMapErrorTo503AndReturnOriginalBody(ApiException apiException,String bodyVariable,String bodyVariable2) throws IOException, URISyntaxException {
        //given
        configureEventTransformations();
        configureHeadersForEventListener();

        MockHttpServletRequest request = givenMockHttpServletRequest();
        when(eventPublisher.sendEvent(any(), any())).thenThrow(new InternalException(apiException));
        String validEvent = JsonDataLoader.loadContent("/ves7_valid_30_1_1_event.json");

        //when
        final ResponseEntity<String> response = vesRestController.event(validEvent, VERSION_V7, request);

        //then
        assertThat(response.getStatusCodeValue()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        verifyErrorResponse(
                response,
                "SVC2000",
                "The following service error occurred: %1. Error code is %2",
                List.of(bodyVariable,bodyVariable2)
        );
    }

    private static Stream<Arguments> errorsCodeAndResponseBody() {
        return Stream.of(
                arguments(ApiException.NOT_FOUND, "Not Found","404"),
                arguments(ApiException.REQUEST_TIMEOUT, "Request Timeout","408"),
                arguments(ApiException.TOO_MANY_REQUESTS, "Too Many Requests","429"),
                arguments(ApiException.INTERNAL_SERVER_ERROR, "Internal Server Error","500"),
                arguments(ApiException.BAD_GATEWAY, "Bad Gateway","502"),
                arguments(ApiException.SERVICE_UNAVAILABLE, "Service Unavailable","503"),
                arguments(ApiException.GATEWAY_TIMEOUT, "Gateway Timeout","504")
        );
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

    private void verifyErrorResponse(ResponseEntity<String> response, String messageId, String messageText, List<String> variables) throws com.fasterxml.jackson.core.JsonProcessingException {
        final Map<String, ?> errorDetails = fetchErrorDetails(response);
        assertThat((Map<String, String>)errorDetails).containsEntry("messageId", messageId);
        assertThat((Map<String, String>)errorDetails).containsEntry("text", messageText);
        assertThat((Map<String, List<String>>)errorDetails).containsEntry("variables",  variables);
    }

    private Map<String, ?> fetchErrorDetails(ResponseEntity<String> response) throws com.fasterxml.jackson.core.JsonProcessingException {
        final String body = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Map<String, Map<String,String>>> map = mapper.readValue(body, Map.class);
        return map.get("requestError").get("ServiceException");
    }

    private void configureEventTransformations() throws IOException, URISyntaxException {
        final List<EventTransformation> eventTransformations = loadEventTransformations();
        when(applicationSettings.isVersionSupported(VERSION_V7)).thenReturn(true);
        when(applicationSettings.eventTransformingEnabled()).thenReturn(true);
        when(applicationSettings.getEventTransformations()).thenReturn((eventTransformations));
    }

    private void configureHeadersForEventListener() {
        when(headerUtils.getRestApiIdentify(anyString())).thenReturn("eventListener");
        when(applicationSettings.getApiVersionDescriptionFilepath()).thenReturn("etc/api_version_description.json");
    }

    private void verifyThatTransformedEventWasSend(DMaaPEventPublisher eventPublisher, String eventBeforeTransformation) {
        // event before transformation
        assertThat(eventBeforeTransformation).contains("\"version\": \"4.0.1\"");
        assertThat(eventBeforeTransformation).contains("\"faultFieldsVersion\": \"4.0\"");

        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
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

    private List<EventTransformation> loadEventTransformations() throws IOException, URISyntaxException {
        Type EVENT_TRANSFORM_LIST_TYPE = new TypeToken<List<EventTransformation>>() {
        }.getType();

            URI resource = this.getClass().getResource(EVENT_TRANSFORM_FILE_PATH).toURI();
            try (FileReader fr = new FileReader(resource.getPath())) {
                return new Gson().fromJson(fr, EVENT_TRANSFORM_LIST_TYPE);
            }
    }
}

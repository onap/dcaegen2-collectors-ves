/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
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

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.FileReader;
import org.onap.dcae.common.model.VesEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventValidatorTest {
    private static final String DUMMY_SCHEMA_VERSION = "v5";
    private static final String DUMMY_TYPE = "type";
    private final String newSchemaV7 = FileReader.readFileAsString("etc/CommonEventFormat_30.2_ONAP.json");
    private JSONObject sentEvent;
    private static final String V7_VERSION = "v7";
    private static JSONObject jsonObject;
    private static final String EVENT_TYPE = "event";

    @Mock
    private ApplicationSettings settings;

    private SchemaValidator schemaValidator = spy( new SchemaValidator());

    private EventValidator sut;


    @BeforeAll
    static void setupTests() {
        jsonObject = new JSONObject("{" + DUMMY_TYPE + ":dummy}");
    }

    @BeforeEach
    public void setUp(){
        this.sut = new EventValidator(settings, schemaValidator);
    }

    @Test
    void shouldNotValidateEventWhenJsonSchemaValidationDisabled() throws EventValidatorException {
        //given
        when(settings.eventSchemaValidationEnabled()).thenReturn(false);

        //when
        this.sut.validate(new VesEvent(jsonObject), DUMMY_TYPE, DUMMY_SCHEMA_VERSION);

        //then
        verify(schemaValidator, never()).conformsToSchema(any(), any());

    }

    @Test
    void shouldReturnInvalidJsonErrorOnWrongType() {
        //given
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        try {
            sut.validate(new VesEvent(jsonObject), "wrongType", DUMMY_SCHEMA_VERSION);
        } catch (EventValidatorException e) {
            //then
            assertEquals(ApiException.INVALID_JSON_INPUT, e.getApiException());
        }


    }

    @Test
    void shouldReturnSchemaValidationFailedErrorOnInvalidJsonObjectSchema() {
        //given
        String schemaRejectingEverything = "{\"not\":{}}";
        mockJsonSchema(schemaRejectingEverything);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        try {
            sut.validate(new VesEvent(jsonObject), DUMMY_TYPE, DUMMY_SCHEMA_VERSION);
        } catch (EventValidatorException e) {
            //then
            assertEquals(ApiException.SCHEMA_VALIDATION_FAILED, e.getApiException());
        }

    }

    @Test
    void shouldReturnEmptyOptionalOnValidJsonObjectSchema() {
        //given
        String schemaAcceptingEverything = "{}";
        mockJsonSchema(schemaAcceptingEverything);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        try {
            sut.validate(new VesEvent(jsonObject), DUMMY_TYPE, DUMMY_SCHEMA_VERSION);
        } catch (EventValidatorException e) {
            failWithError();
        }
    }

    @Test
    public void shouldReturnNoErrorsWhenValidating30_1_1ValidEvent() {
        //given
        sentEvent = new JSONObject(FileReader.readFileAsString("src/test/resources/ves7_valid_30_1_1_event.json"));

        mockJsonSchema(newSchemaV7);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        try {
            sut.validate(new VesEvent(sentEvent), EVENT_TYPE, V7_VERSION);
        } catch (EventValidatorException e) {
            failWithError();
        }
    }

    @Test
    void shouldReturnNoErrorsWhenValidatingValidEventWithStndDefinedFields() {
        //given
        sentEvent = new JSONObject(FileReader.readFileAsString("src/test/resources/ves7_valid_eventWithStndDefinedFields.json"));

        mockJsonSchema(newSchemaV7);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        try {
            sut.validate(new VesEvent(sentEvent), EVENT_TYPE, V7_VERSION);
        } catch (EventValidatorException e) {
            failWithError();
        }
    }

    @Test
    void shouldReturnSchemaValidationFailedWhenValidating30_1_1InvalidEvent() {
        //given
        sentEvent = new JSONObject(FileReader.readFileAsString("src/test/resources/ves7_invalid_30_1_1_event.json"));

        mockJsonSchema(newSchemaV7);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        try {
            sut.validate(new VesEvent(this.sentEvent), EVENT_TYPE, V7_VERSION);
        } catch (EventValidatorException e) {
            //then
            assertEquals(ApiException.SCHEMA_VALIDATION_FAILED, e.getApiException());
        }


    }

    private void failWithError() {
        fail("Validation should not report any error!");
    }

    private void mockJsonSchema(String jsonSchemaContent) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();

        JsonSchema schema = factory.getSchema(jsonSchemaContent);
        when(settings.jsonSchema(any())).thenReturn(schema);
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;

import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.FileReader;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class EventValidatorTest {
    private static final String DUMMY_SCHEMA_VERSION = "v5";
    private static final String DUMMY_TYPE = "type";
    private final String newSchemaV7 = FileReader.readFileAsString("etc/CommonEventFormat_30.2_ONAP.json");
    private JSONObject sentEvent;
    private static final String V7_VERSION = "v7";
    private static JSONObject jsonObject;
    private static final String EVENT_TYPE = "event";

    @Mock
    private static ApplicationSettings settings;

    @InjectMocks
    private static EventValidator sut;


    @BeforeAll
    static void setupTests() {
        jsonObject = new JSONObject("{" + DUMMY_TYPE + ":dummy}");
    }

    @Test
    public void shouldReturnEmptyOptionalOnJsonSchemaValidationDisabled() {
        //given
        when(settings.eventSchemaValidationEnabled()).thenReturn(false);

        //when
        Optional<ResponseEntity<String>> result = sut.validate(jsonObject, DUMMY_TYPE, DUMMY_SCHEMA_VERSION);

        //then
        assertEquals(Optional.empty(), result);

    }

    @Test
    public void shouldReturnInvalidJsonErrorOnWrongType() {
        //given
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        Optional<ResponseEntity<String>> result = sut.validate(jsonObject, "wrongType", DUMMY_SCHEMA_VERSION);

        //then
        assertEquals(generateResponseOptional(ApiException.INVALID_JSON_INPUT), result);
    }

    @Test
    public void shouldReturnSchemaValidationFailedErrorOnInvalidJsonObjectSchema() {
        //given
        String schemaRejectingEverything = "{\"not\":{}}";
        mockJsonSchema(schemaRejectingEverything);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        Optional<ResponseEntity<String>> result = sut.validate(jsonObject, DUMMY_TYPE, DUMMY_SCHEMA_VERSION);

        //then
        assertEquals(generateResponseOptional(ApiException.SCHEMA_VALIDATION_FAILED), result);
    }

    @Test
    public void shouldReturnEmptyOptionalOnValidJsonObjectSchema() {
        //given
        String schemaAcceptingEverything = "{}";
        mockJsonSchema(schemaAcceptingEverything);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        Optional<ResponseEntity<String>> result = sut.validate(jsonObject, DUMMY_TYPE, DUMMY_SCHEMA_VERSION);

        //then
        assertEquals(Optional.empty(), result);
    }

    @Test
    public void shouldReturnNoErrorsWhenValidating30_1_1ValidEvent() {
        //given
        sentEvent = new JSONObject(FileReader.readFileAsString("src/test/resources/ves7_valid_30_1_1_event.json"));

        mockJsonSchema(newSchemaV7);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        Optional<ResponseEntity<String>> result = sut.validate(sentEvent, EVENT_TYPE, V7_VERSION);

        //then
        assertEquals(Optional.empty(), result);
    }

    @Test
    public void shouldReturnNoErrorsWhenValidatingValidEventWithStndDefinedFields() {
        //given
        sentEvent = new JSONObject(FileReader.readFileAsString("src/test/resources/ves7_valid_eventWithStndDefinedFields.json"));

        mockJsonSchema(newSchemaV7);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        Optional<ResponseEntity<String>> result = sut.validate(sentEvent, EVENT_TYPE, V7_VERSION);

        //then
        assertEquals(Optional.empty(), result);
    }

    @Test
    public void shouldReturnSchemaValidationFailedWhenValidating30_1_1InvalidEvent() {
        //given
        sentEvent = new JSONObject(FileReader.readFileAsString("src/test/resources/ves7_invalid_30_1_1_event.json"));

        mockJsonSchema(newSchemaV7);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        Optional<ResponseEntity<String>> result = sut.validate(this.sentEvent, EVENT_TYPE, V7_VERSION);

        //then
        assertEquals(generateResponseOptional(ApiException.SCHEMA_VALIDATION_FAILED), result);
    }


    private void mockJsonSchema(String jsonSchemaContent) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();

        JsonSchema schema = factory.getSchema(jsonSchemaContent);
        when(settings.jsonSchema(any())).thenReturn(schema);
    }

    private Optional<ResponseEntity<String>> generateResponseOptional(ApiException schemaValidationFailed) {
        return Optional.of(ResponseEntity.status(schemaValidationFailed.httpStatusCode)
                .body(schemaValidationFailed.toJSON().toString()));
    }
}

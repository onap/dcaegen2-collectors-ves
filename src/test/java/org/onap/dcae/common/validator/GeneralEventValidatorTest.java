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

package org.onap.dcae.common.validator;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.FileReader;
import org.onap.dcae.common.model.VesEvent;
import org.onap.dcae.restapi.ApiException;
import org.onap.dcae.restapi.EventValidatorException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneralEventValidatorTest {
    private static final String DUMMY_SCHEMA_VERSION = "v5";
    private static final String DUMMY_TYPE = "type";
    private final String newSchemaV7 = FileReader.readFileAsString("etc/CommonEventFormat_30.2_ONAP.json");
    private final String schemaWithIP = FileReader.readFileAsString("etc/CommonEventFormat_30.2.1_ONAP.json");
    private JSONObject sentEvent;
    private static final String V7_VERSION = "v7";
    private static JSONObject jsonObject;
    private static final String EVENT_TYPE = "event";

    @Mock
    private ApplicationSettings settings;

    private final SchemaValidator schemaValidator = Mockito.spy(new SchemaValidator());

    private GeneralEventValidator sut;


    @BeforeAll
    static void setupTests() {
        jsonObject = new JSONObject("{" + DUMMY_TYPE + ":dummy}");
    }

    @BeforeEach
    public void setUp() {
        this.sut = new GeneralEventValidator(settings, schemaValidator);
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
        Executable testedMethod = () -> sut.validate(new VesEvent(jsonObject), "wrongType", DUMMY_SCHEMA_VERSION);
        EventValidatorException thrownException = assertThrows(EventValidatorException.class, testedMethod);
        assertEquals(ApiException.INVALID_JSON_INPUT, thrownException.getApiException());

    }

    @Test
    void shouldReturnSchemaValidationFailedErrorOnInvalidJsonObjectSchema() {
        //given
        String schemaRejectingEverything = "{\"not\":{}}";
        mockJsonSchema(schemaRejectingEverything);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        Executable testedMethod = () -> sut.validate(new VesEvent(jsonObject), DUMMY_TYPE, DUMMY_SCHEMA_VERSION);
        EventValidatorException thrownException = assertThrows(EventValidatorException.class, testedMethod);
        assertEquals(ApiException.SCHEMA_VALIDATION_FAILED, thrownException.getApiException());

    }


    @Test
    void shouldReturnEmptyOptionalOnValidJsonObjectSchema() {
        //given
        String schemaAcceptingEverything = "{}";
        mockJsonSchema(schemaAcceptingEverything);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        assertDoesNotThrow(() -> sut.validate(new VesEvent(jsonObject), DUMMY_TYPE, DUMMY_SCHEMA_VERSION));
    }

    @Test
    public void shouldReturnNoErrorsWhenValidating30_1_1ValidEvent() {
        //given
        sentEvent = new JSONObject(FileReader.readFileAsString("src/test/resources/ves7_valid_30_1_1_event.json"));

        mockJsonSchema(newSchemaV7);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        assertDoesNotThrow(() -> sut.validate(new VesEvent(sentEvent), EVENT_TYPE, V7_VERSION));
    }

    @Test
    void shouldReturnNoErrorsWhenValidatingValidEventWithStndDefinedFields() {
        //given
        sentEvent = new JSONObject(FileReader.readFileAsString("src/test/resources/ves_stdnDefined_valid.json"));

        mockJsonSchema(newSchemaV7);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        assertDoesNotThrow(() -> sut.validate(new VesEvent(sentEvent), EVENT_TYPE, V7_VERSION));
    }

    @Test
    void shouldReturnSchemaValidationFailedWhenValidating30_1_1InvalidEvent() {
        //given
        sentEvent = new JSONObject(FileReader.readFileAsString("src/test/resources/ves7_invalid_30_1_1_event.json"));

        mockJsonSchema(newSchemaV7);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);

        //when
        Executable testedMethod = () -> sut.validate(new VesEvent(sentEvent), EVENT_TYPE, V7_VERSION);
        EventValidatorException thrownException = assertThrows(EventValidatorException.class, testedMethod);
        assertEquals(ApiException.SCHEMA_VALIDATION_FAILED, thrownException.getApiException());
    }

    @Test
    void shouldReturnNoErrorWhenIPv4ValidInLongFrom() {
        //given
        mockJsonSchema(schemaWithIP);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);
        sentEvent = new JSONObject(FileReader.readFileAsString("src/test/resources/ves7_valid_ip_v4.json"));
        //when
        assertDoesNotThrow(() -> sut.validate(new VesEvent(sentEvent), EVENT_TYPE, V7_VERSION));

    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"ves7_invalid_ip_v4_with_ipv6_format.json", "ves7_invalid_ipv4.json"})
    void shouldReturnSchemaValidationErrorWhenIPv4Invalid(String filename) {
        //given
        mockJsonSchema(schemaWithIP);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);
        sentEvent = new JSONObject(FileReader.readFileAsString("src/test/resources/" + filename));
        //when
        Executable testedMethod = () -> sut.validate(new VesEvent(sentEvent), EVENT_TYPE, V7_VERSION);
        EventValidatorException thrownException = assertThrows(EventValidatorException.class, testedMethod);
        assertEquals(ApiException.SCHEMA_VALIDATION_FAILED, thrownException.getApiException());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"ves7_valid_ip_v6_with_zone_index.json",
            "ves7_valid_ip_v6.json", "ves7_valid_ip_v6_short_one.json",
            "ves7_valid_ip_v6_full.json", "ves7_valid_ip_v6_short_without_end.json",
            "ves7_valid_ip_v6_short_with_big_letters.json", "ves7_valid_ip_v6_multicast_example.json",
            "ves7_valid_ip_v6_ipv4_translated.json"})
    void shouldReturnNoErrorWhenIPv6Valid(String filename) {
        //given
        mockJsonSchema(schemaWithIP);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);
        sentEvent = new JSONObject(FileReader.readFileAsString("src/test/resources/" + filename));
        //when
        assertDoesNotThrow(() -> sut.validate(new VesEvent(sentEvent), EVENT_TYPE, V7_VERSION));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"ves7_invalid_ip_v6_short_with_more_than_two_colons.json",
            "ves7_invalid_ip_v6_with_ipv4_format.json",
            "ves7_invalid_ip_v6_short_with_too_many_colons.json",
            "ves7_invalid_ip_v6_with_one_colon_at_begining.json",
            "ves7_invalid_ip_v6_double_colon_more_than_once.json",
            "ves7_invalid_ip_v6_out_of_range.json"
    })
    void shouldReturnSchemaValidationErrorWhenIPv6Invalid(String filename) {
        //given
        String schema = schemaWithIP;
        mockJsonSchema(schema);
        when(settings.eventSchemaValidationEnabled()).thenReturn(true);
        sentEvent = new JSONObject(FileReader.readFileAsString("src/test/resources/" + filename));
        //when
        Executable testedMethod = () -> sut.validate(new VesEvent(sentEvent), EVENT_TYPE, V7_VERSION);
        EventValidatorException thrownException = assertThrows(EventValidatorException.class, testedMethod);
        assertEquals(ApiException.SCHEMA_VALIDATION_FAILED, thrownException.getApiException());
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
/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2019 Nokia. All rights reserved.
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

import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.dcae.ApplicationSettings;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventValidatorTest {
    private static final String DUMMY_SCHEMA_VERSION = "v5";
    private static final String DUMMY_TYPE = "type";

    private JSONObject jsonObject;

    @Mock
    private ApplicationSettings settings;

    @InjectMocks
    private EventValidator sut;

    @Before
    public void setupTests(){
        jsonObject = new JSONObject("{" + DUMMY_TYPE + ":dummy}");
        when(settings.jsonSchemaValidationEnabled()).thenReturn(true);
    }

    @Test
    public void shouldReturnEmptyOptionalOnJsonSchemaValidationDisabled() {
        //given
        doReturn(false).when(settings).jsonSchemaValidationEnabled();

        //when
        Optional<ResponseEntity<String>> result = sut.validate(jsonObject, DUMMY_TYPE, DUMMY_SCHEMA_VERSION);

        //then
        assertEquals(Optional.empty(), result);

    }

    @Test
    public void shouldReturnInvalidJsonErrorOnWrongType() {
        //when
        Optional<ResponseEntity<String>> result = sut.validate(jsonObject, "wrongType", DUMMY_SCHEMA_VERSION);

        //then
        assertEquals(generateResponseOptional(ApiException.INVALID_JSON_INPUT), result);
    }

    @Test
    public void shouldReturnSchemaValidationFailedErrorOnInvalidJsonObjectSchema() throws ProcessingException, IOException {
        //given
        String schemaRejectingEverything = "{\"not\":{}}";
        mockJsonSchema(schemaRejectingEverything);

        //when
        Optional<ResponseEntity<String>> result = sut.validate(jsonObject, DUMMY_TYPE, DUMMY_SCHEMA_VERSION);

        //then
        assertEquals(generateResponseOptional(ApiException.SCHEMA_VALIDATION_FAILED), result);
    }

    @Test
    public void shouldReturnEmptyOptionalOnValidJsonObjectSchema() throws ProcessingException, IOException {
        //given
        String schemaAcceptingEverything = "{}";
        mockJsonSchema(schemaAcceptingEverything);

        //when
        Optional<ResponseEntity<String>> result = sut.validate(jsonObject, DUMMY_TYPE, DUMMY_SCHEMA_VERSION);

        //then
        assertEquals(Optional.empty(), result);
    }

    private void mockJsonSchema(String jsonSchema) throws IOException, ProcessingException {
        when(settings.jsonSchema(any())).thenReturn(
                JsonSchemaFactory.byDefault()
                        .getJsonSchema(JsonLoader.fromString(jsonSchema)));
    }

    private Optional<ResponseEntity<String>> generateResponseOptional(ApiException schemaValidationFailed) {
        return Optional.of(ResponseEntity.status(schemaValidationFailed.httpStatusCode)
                .body(schemaValidationFailed.toJSON().toString()));
    }
}
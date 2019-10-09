/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2018 - 2019 Nokia. All rights reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.tomakehurst.wiremock.common.Json;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.dcae.ApplicationSettings;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventValidatorTest {

    @Mock
    private ApplicationSettings settings;

    @InjectMocks
    private EventValidator sut;

    @Test
    public void shouldReturnEmptyOptionalOnJsonSchemaValidationDisabled() {
        //given
        JSONObject jsonObject = new JSONObject("{type:dummy}");
        when(settings.jsonSchemaValidationEnabled()).thenReturn(false);

        //when
        Optional<ResponseEntity<String>> valid = sut.validate(jsonObject, "dummyType", "dummyVersion");

        //then
        assertEquals(Optional.empty(), valid);

    }

    @Test
    public void shouldReturnInvalidJsonErrorOnWrongType() {
        //given
        when(settings.jsonSchemaValidationEnabled()).thenReturn(true);
        JSONObject jsonObject = new JSONObject("{type:dummy}");

        //when
        Optional<ResponseEntity<String>> valid = sut.validate(jsonObject, "wrongType", "dummyVersion");

        //then
        assertEquals(Optional.of(ResponseEntity.status(ApiException.INVALID_JSON_INPUT.httpStatusCode)
                .body(ApiException.INVALID_JSON_INPUT.toJSON().toString())), valid);
    }

    @Test
    public void shouldReturnSchemaValidationFailedErrorOnInvalidJsonObjectSchema() throws ProcessingException {
        //given
        setupSchemaConfirmation(false);
        JSONObject jsonObject = new JSONObject("{rightType:dummy}");

        //when
        Optional<ResponseEntity<String>> valid = sut.validate(jsonObject, "rightType", "dummyVersion");

        //then
        assertEquals(Optional.of(ResponseEntity.status(ApiException.SCHEMA_VALIDATION_FAILED.httpStatusCode)
                .body(ApiException.SCHEMA_VALIDATION_FAILED.toJSON().toString())), valid);
    }

    @Test
    public void shouldReturnEmptyOptionalOnValidJsonObjectSchema() throws ProcessingException {
        //given
        setupSchemaConfirmation(true);
        JSONObject jsonObject = new JSONObject("{rightType:dummy}");

        //when
        Optional<ResponseEntity<String>> valid = sut.validate(jsonObject, "rightType", "dummyVersion");

        //then
        assertEquals(Optional.empty(), valid);
    }

    private void setupSchemaConfirmation(boolean isSchemaValid) throws ProcessingException {
        ProcessingReport processingReport = mock(ProcessingReport.class);
        when(processingReport.isSuccess()).thenReturn(isSchemaValid);
        when(processingReport.spliterator()).thenReturn(Spliterators.emptySpliterator());

        JsonSchema jsonSchema = mock(JsonSchema.class);
        when(jsonSchema.validate(any(JsonNode.class))).thenReturn(processingReport);

        when(settings.jsonSchema(any())).thenReturn(jsonSchema);
        when(settings.jsonSchemaValidationEnabled()).thenReturn(true);
    }


}
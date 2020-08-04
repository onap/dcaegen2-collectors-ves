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

package org.onap.dcae.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.FileReader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StndDefinedDataValidatorTest {

    @Mock
    private ApplicationSettings settings;
    private StndDefinedDataValidator stndDefinedDataValidator;

    private static final String MAPPING_FILE_LOCATION = "./src/test/resources/stndDefined/schema-map.json";
    private static final String SCHEMA_FILES_LOCATION = "src/test/resources/stndDefined";
    private static final String STND_DEFINED_DATA_PATH = "/event/stndDefinedFields/data";
    private static final String SCHEMA_REF_PATH = "/event/stndDefinedFields/schemaReference";

    @BeforeEach
    public void setUp() {
        mockStndDefinedValidationProps();
        StndDefinedValidatorResolver stndDefinedValidatorResolver = new StndDefinedValidatorResolver(settings);
        stndDefinedDataValidator = new StndDefinedDataValidator(stndDefinedValidatorResolver);
    }

    @Test
    public void shouldReturnTrueWhenValidEvent() throws JsonProcessingException {
        //given
        String fileName = "src/test/resources/ves_stdnDefined_valid.json";
        JsonNode event = getJsonNodeEvent(fileName);
        //when
        //then
        assertTrue(stndDefinedDataValidator.validate(event));
    }

    @Test
    public void shouldReturnFalseWhenInvalidEvent() throws JsonProcessingException {
        //given
        String fileName = "src/test/resources/ves_stdnDefined_invalid.json";
        JsonNode event = getJsonNodeEvent(fileName);
        //when
        //then
        assertFalse(stndDefinedDataValidator.validate(event));
    }

    private JsonNode getJsonNodeEvent(String fileName) throws JsonProcessingException {
        String validEventContent = FileReader.readFileAsString(fileName);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(validEventContent);
    }

    private void mockStndDefinedValidationProps() {
        when(settings.getExternalSchemaMappingFileLocation()).thenReturn(MAPPING_FILE_LOCATION);
        when(settings.getExternalSchemaSchemaRefPath()).thenReturn(SCHEMA_REF_PATH);
        when(settings.getExternalSchemaSchemasLocation()).thenReturn(SCHEMA_FILES_LOCATION);
        when(settings.getExternalSchemaStndDefinedDataPath()).thenReturn(STND_DEFINED_DATA_PATH);
    }
}
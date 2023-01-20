/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
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

package org.onap.dcae.common.validator;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.FileReader;
import org.onap.dcae.common.model.VesEvent;
import org.onap.dcae.restapi.ApiException;
import org.onap.dcae.restapi.EventValidatorException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;

@ExtendWith(MockitoExtension.class)
public class StndDefinedDataValidatorTest {

    @Mock
    private ApplicationSettings settings;
    private StndDefinedDataValidator stndDefinedDataValidator;

    private static final String MAPPING_FILE_LOCATION = "./src/test/resources/stndDefined/schema-map.json";
    private static final String SCHEMA_FILES_LOCATION = "./src/test/resources/stndDefined";
    private static final String STND_DEFINED_DATA_PATH = "/event/stndDefinedFields/data";
    private static final String SCHEMA_REF_PATH = "/event/stndDefinedFields/schemaReference";

    @BeforeEach
    public void setUp() {
        mockStndDefinedValidationProps();
        StndDefinedValidatorResolver stndDefinedValidatorResolver = new StndDefinedValidatorResolver(settings);
        stndDefinedDataValidator = new StndDefinedDataValidator(stndDefinedValidatorResolver.resolve());
    }

    @Test
    public void shouldReturnTrueWhenEventIsValid() throws EventValidatorException {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));
        //given
        VesEvent event = getVesEvent("src/test/resources/ves_stdnDefined_valid.json");

        //when
        //then
        assertDoesNotThrow(() -> stndDefinedDataValidator.validate(event));
    }

    @Test
    public void shouldReturnFalseWhenEventIsInvalid() throws EventValidatorException {
        //given
        VesEvent event = getVesEvent("src/test/resources/ves_stdnDefined_invalid.json");

        try {
            //when
            stndDefinedDataValidator.validate(event);
        } catch (EventValidatorException e) {
            //then
            assertEquals(ApiException.STND_DEFINED_VALIDATION_FAILED, e.getApiException());
        }
    }

    @Test
    void shouldReturnErrorWhenMissingLocalSchemaReferenceInMappingFile() {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));
        //given
        VesEvent event = getVesEvent("src/test/resources/ves_stdnDefined_missing_local_schema_reference.json");
        try {
            //when
            stndDefinedDataValidator.validate(event);
        } catch (EventValidatorException e) {
            //then
            assertEquals(ApiException.NO_LOCAL_SCHEMA_REFERENCE, e.getApiException());
        }
    }

    @Test
    void shouldReturnErrorWhenIncorrectInternalFileReference() {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().startsWith("win"));
        //given
        VesEvent event = getVesEvent("src/test/resources/ves_stdnDefined_wrong_internal_file_reference.json");
        try {
            //when
            stndDefinedDataValidator.validate(event);
        } catch (EventValidatorException e) {
            //then
            assertEquals(ApiException.INCORRECT_INTERNAL_FILE_REFERENCE, e.getApiException());
        }
    }

    @Test
    void shouldReturnErrorWhenStndDefinedFieldsDataIsEmpty() {
        //given
        VesEvent event = getVesEvent("src/test/resources/ves_stdnDefined_with_empty_stndDefined_fields_data.json");
        try {
            //when
            stndDefinedDataValidator.validate(event);
        } catch (EventValidatorException e) {
            //then
            assertEquals(ApiException.STND_DEFINED_VALIDATION_FAILED, e.getApiException());
        }
    }

    @Test
    void shouldNotReturnErrorWhenValidatingInvalidEventAndStndDefinedReferenceMissing() {
        //given
        VesEvent event = getVesEvent("src/test/resources/ves_stdnDefined_without_schema_reference.json");
        
        //when
        //then
        assertDoesNotThrow(() -> stndDefinedDataValidator.validate(event));
    }

    @NotNull
    private VesEvent getVesEvent(String filename) {
            JSONObject jsonObjectEvent = getJsonObjectEvent(filename);
            return new VesEvent(jsonObjectEvent);
    }

    private JSONObject getJsonObjectEvent(String fileName) {
        String eventContent = FileReader.readFileAsString(fileName);
        return new JSONObject(eventContent);
    }

    private void mockStndDefinedValidationProps() {
        when(settings.getExternalSchemaMappingFileLocation()).thenReturn(Paths.get(MAPPING_FILE_LOCATION).toString());
        when(settings.getExternalSchemaSchemaRefPath()).thenReturn(SCHEMA_REF_PATH);
        when(settings.getExternalSchemaSchemasLocation()).thenReturn(Paths.get(SCHEMA_FILES_LOCATION).toString());
        when(settings.getExternalSchemaStndDefinedDataPath()).thenReturn(STND_DEFINED_DATA_PATH);
    }
}

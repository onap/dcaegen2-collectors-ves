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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcae.ApplicationSettings;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StndDefinedValidatorResolverTest {

    @Mock
    private ApplicationSettings settings;

    private static final String MAPPING_FILE_LOCATION = "./src/test/resources/stndDefined/schema-map.json";
    private static final String SCHEMA_FILES_LOCATION = "src/test/resources/stndDefined";
    private static final String STND_DEFINED_DATA_PATH = "/event/stndDefinedFields/data";
    private static final String SCHEMA_REF_PATH = "/event/stndDefinedFields/schemaReference";
    StndDefinedValidatorResolver stndDefinedValidatorResolver;

    @BeforeEach
    public void setUp() {
        mockStndDefinedValidationProps();
        stndDefinedValidatorResolver = new StndDefinedValidatorResolver(settings);
    }

    @Test
    public void shouldReturnStndValidatorWithDefaultSchemaConfigurations() {
        //then
        Assertions.assertDoesNotThrow(() -> stndDefinedValidatorResolver.resolve());
    }

    private void mockStndDefinedValidationProps() {
        when(settings.getExternalSchemaMappingFileLocation()).thenReturn(MAPPING_FILE_LOCATION);
        when(settings.getExternalSchemaSchemaRefPath()).thenReturn(SCHEMA_REF_PATH);
        when(settings.getExternalSchemaSchemasLocation()).thenReturn(SCHEMA_FILES_LOCATION);
        when(settings.getExternalSchemaStndDefinedDataPath()).thenReturn(STND_DEFINED_DATA_PATH);
    }


}
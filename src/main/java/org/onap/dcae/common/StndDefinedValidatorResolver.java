/*
 * ============LICENSE_START=======================================================
 * VES
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

import org.onap.dcae.ApplicationSettings;
import org.onap.dcaegen2.services.sdk.services.external.schema.manager.service.StndDefinedValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StndDefinedValidatorResolver {

    private final ApplicationSettings settings;

    @Autowired
    public StndDefinedValidatorResolver(ApplicationSettings settings) {
        this.settings = settings;
    }

    public StndDefinedValidator resolve() {
        return new StndDefinedValidator.ValidatorBuilder()
                .mappingFilePath(settings.getExternalSchemaMappingFileLocation())
                .schemaRefPath(settings.getExternalSchemaSchemaRefPath())
                .schemasPath(settings.getExternalSchemaSchemasLocation())
                .stndDefinedDataPath(settings.getExternalSchemaStndDefinedDataPath())
                .build();
    }
}

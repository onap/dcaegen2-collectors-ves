/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.model.VesEvent;
import org.onap.dcae.restapi.ApiException;
import org.onap.dcae.restapi.EventValidatorException;

/**
 * This class is using ApplicationSetting and SchemaValidator to validate VES event.
 *
 * @author Zebek
 */
public class GeneralEventValidator {

    private final SchemaValidator schemaValidator;
    private final ApplicationSettings applicationSettings;

    public GeneralEventValidator(ApplicationSettings applicationSettings) {
        this(applicationSettings, new SchemaValidator());
    }

    GeneralEventValidator(ApplicationSettings applicationSettings, SchemaValidator schemaValidator) {
        this.applicationSettings = applicationSettings;
        this.schemaValidator = schemaValidator;
    }

    /**
     * This method is validating given event using schema adn throws exception if event is not valid
     *
     * @param vesEvent event that will be validate
     * @param type     expected type of event
     * @param version  json schema version that will be used
     * @throws EventValidatorException when event is not valid or have wrong type
     */
    public void validate(VesEvent vesEvent, String type, String version) throws EventValidatorException {
        if (applicationSettings.eventSchemaValidationEnabled()) {
            doValidation(vesEvent, type, version);
        }
    }

    private void doValidation(VesEvent vesEvent, String type, String version) throws EventValidatorException {
        if (vesEvent.hasType(type)) {
            if (!isEventMatchToSchema(vesEvent, applicationSettings.jsonSchema(version))) {
                throw new EventValidatorException(ApiException.SCHEMA_VALIDATION_FAILED);
            }
        } else {
            throw new EventValidatorException(ApiException.INVALID_JSON_INPUT);
        }
    }

    private boolean isEventMatchToSchema(VesEvent vesEvent, JsonSchema schema) {
        return schemaValidator.conformsToSchema(vesEvent.asJsonObject(), schema);
    }
}

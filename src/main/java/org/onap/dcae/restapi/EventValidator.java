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
package org.onap.dcae.restapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.schema.JsonSchema;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.StndDefinedDataValidator;
import org.onap.dcae.common.model.VesEvent;

/**
 * This class is using ApplicationSetting and SchemaValidator to validate VES event.
 *
 * @author Zebek
 */
public class EventValidator {

    private static final String STND_DEFINED_DOMAIN = "stndDefined";
    private final SchemaValidator schemaValidator;
    private final ApplicationSettings applicationSettings;
    private final StndDefinedDataValidator stndDefinedDataValidator;

    public EventValidator(ApplicationSettings applicationSettings, StndDefinedDataValidator stndDefinedDataValidator) {
        this(applicationSettings, new SchemaValidator(), stndDefinedDataValidator);
    }

    EventValidator(ApplicationSettings applicationSettings, SchemaValidator schemaValidator, StndDefinedDataValidator stndDefinedDataValidator) {
        this.applicationSettings = applicationSettings;
        this.schemaValidator = schemaValidator;
        this.stndDefinedDataValidator = stndDefinedDataValidator;
    }

    /**
     * Validates given event using schema and throws exception when event is not valid.
     *
     * @param vesEvent event that will be validate
     * @param type     expected type of event
     * @param version  json schema version that will be used
     * @throws EventValidatorException when event is not valid or have wrong type
     */
    public void validate(VesEvent vesEvent, String type, String version) throws EventValidatorException {
        if (applicationSettings.eventSchemaValidationEnabled()) {
            if (vesEvent.hasType(type)) {
                executeMainValidation(vesEvent, version);
                executeStndDefinedValidation(vesEvent);
            } else {
                throw new EventValidatorException(ApiException.INVALID_JSON_INPUT);
            }
        }
    }

    private void executeMainValidation(VesEvent vesEvent, String version) throws EventValidatorException {
        if (!doesEventMatchToSchema(vesEvent, applicationSettings.jsonSchema(version))) {
            throw new EventValidatorException(ApiException.SCHEMA_VALIDATION_FAILED);
        }
    }

    private void executeStndDefinedValidation(VesEvent vesEvent) throws EventValidatorException {
        try {
            if (shouldStndDefinedFieldsBeValidated(vesEvent) && !stndDefinedDataValidator.validate(vesEvent.asJsonNode())) {
                throw new EventValidatorException(ApiException.STND_DEFINED_VALIDATION_FAILED);
            }
        } catch (JsonProcessingException ex) {
            throw new EventValidatorException(ApiException.INVALID_JSON_INPUT);
        }
    }

    private boolean doesEventMatchToSchema(VesEvent vesEvent, JsonSchema schema) {
        return schemaValidator.conformsToSchema(vesEvent.asJsonObject(), schema);
    }

    private boolean shouldStndDefinedFieldsBeValidated(VesEvent event) {
        return applicationSettings.getExternalSchemaValidationCheckflag()
                && event.getDomain().equals(STND_DEFINED_DOMAIN)
                && !event.getSchemaReference().isEmpty();
    }
}

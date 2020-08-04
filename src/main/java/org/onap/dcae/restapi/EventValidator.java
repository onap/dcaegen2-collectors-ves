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
import org.onap.dcaegen2.services.sdk.services.external.schema.manager.exception.IncorrectInternalFileReferenceException;
import org.onap.dcaegen2.services.sdk.services.external.schema.manager.exception.NoLocalReferenceException;

public class EventValidator {

    public static final String STND_DEFINED_DOMAIN = "stndDefined";
    private final SchemaValidator schemaValidator;
    private final ApplicationSettings applicationSettings;
    private final StndDefinedDataValidator stndDefinedDataValidator;

    public EventValidator(ApplicationSettings applicationSettings, StndDefinedDataValidator stndDefinedDataValidator) {
        this(applicationSettings, new SchemaValidator(), stndDefinedDataValidator);
    }

    EventValidator(ApplicationSettings applicationSettings, SchemaValidator schemaValidator,
                   StndDefinedDataValidator stndDefinedDataValidator) {
        this.applicationSettings = applicationSettings;
        this.schemaValidator = schemaValidator;
        this.stndDefinedDataValidator = stndDefinedDataValidator;
    }

    public void validate(VesEvent vesEvent, String type, String version)
            throws EventValidatorException, JsonProcessingException {
        if (applicationSettings.eventSchemaValidationEnabled()) {
            doValidation(vesEvent, type, version);
        }
    }

    private void doValidation(VesEvent vesEvent, String type, String version)
            throws EventValidatorException, JsonProcessingException {
        if (vesEvent.hasType(type)) {

            boolean generalValidationResult = doesEventMatchToSchema(vesEvent, applicationSettings.jsonSchema(version));
            boolean stndDefinedValidationResult = true;
            if (shouldStndDefinedFieldsBeValidated(generalValidationResult, vesEvent)) {
                try {
                    stndDefinedValidationResult = stndDefinedDataValidator.validate(vesEvent.asJsonNode());
                } catch (NoLocalReferenceException e) {
                    throw new EventValidatorException(ApiException.NO_LOCAL_SCHEMA_REFERENCE);
                }
                catch (IncorrectInternalFileReferenceException e) {
                    throw new EventValidatorException(ApiException.INCORRECT_INTERNAL_FILE_REFERENCE);
                }
            }

            if (!generalValidationResult) {
                throw new EventValidatorException(ApiException.SCHEMA_VALIDATION_FAILED);
            }
            if (!stndDefinedValidationResult) {
                throw new EventValidatorException(ApiException.STND_DEFINED_VALIDATION_FAILED);
            }
        } else {
            throw new EventValidatorException(ApiException.INVALID_JSON_INPUT);
        }
    }

    private boolean shouldStndDefinedFieldsBeValidated(boolean validationResult, VesEvent event) {
        return validationResult
                && applicationSettings.getExternalSchema2ndStageValidation()
                && event.getDomain().equals(STND_DEFINED_DOMAIN)
                && !event.getSchemaReference().isEmpty();
    }

    private boolean doesEventMatchToSchema(VesEvent vesEvent, JsonSchema schema) {
        return schemaValidator.conformsToSchema(vesEvent.asJsonObject(), schema);
    }
}

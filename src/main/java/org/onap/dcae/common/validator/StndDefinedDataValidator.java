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

package org.onap.dcae.common.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONException;
import org.onap.dcae.common.model.VesEvent;
import org.onap.dcae.restapi.ApiException;
import org.onap.dcae.restapi.EventValidatorException;
import org.onap.dcaegen2.services.sdk.services.external.schema.manager.exception.IncorrectInternalFileReferenceException;
import org.onap.dcaegen2.services.sdk.services.external.schema.manager.exception.NoLocalReferenceException;
import org.onap.dcaegen2.services.sdk.services.external.schema.manager.service.StndDefinedValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StndDefinedDataValidator {

    private static final String STND_DEFINED_DOMAIN = "stndDefined";

    private final StndDefinedValidator stndDefinedValidator;

    public static final Logger log = LoggerFactory.getLogger(StndDefinedDataValidator.class);

    @Autowired
    public StndDefinedDataValidator(StndDefinedValidator validator) {
        this.stndDefinedValidator = validator;
    }

    /**
     * Validates incoming event
     *
     * @param event as JsonNode
     * @throws EventValidatorException exceptions related to failing StndDefined validation
     */
    public void validate(VesEvent event) throws EventValidatorException {
        try {
            if (shouldBeValidated(event) && !doValidation(event.asJsonNode())) {
                throw new EventValidatorException(ApiException.STND_DEFINED_VALIDATION_FAILED);
            }
        } catch (JsonProcessingException ex) {
            throw new EventValidatorException(ApiException.INVALID_JSON_INPUT, ex);
        }
    }

    private boolean doValidation(JsonNode event) throws EventValidatorException {
        try {
            return stndDefinedValidator.validate(event);
        } catch (NoLocalReferenceException e) {
            throw new EventValidatorException(ApiException.NO_LOCAL_SCHEMA_REFERENCE, e);
        } catch (IncorrectInternalFileReferenceException e) {
            throw new EventValidatorException(ApiException.INCORRECT_INTERNAL_FILE_REFERENCE, e);
        }
    }

    private boolean shouldBeValidated(VesEvent event) {
        boolean shouldBeValidated;
        try {
            shouldBeValidated = STND_DEFINED_DOMAIN.equals(event.getDomain())
                    && !event.getSchemaReference().isEmpty();
        } catch (JSONException e) {
            shouldBeValidated = false;
        }
        return shouldBeValidated;
    }
}

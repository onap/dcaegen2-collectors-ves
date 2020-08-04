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

import com.fasterxml.jackson.databind.JsonNode;
import org.onap.dcae.restapi.ApiException;
import org.onap.dcae.restapi.EventValidatorException;
import org.onap.dcaegen2.services.sdk.services.external.schema.manager.exception.IncorrectInternalFileReferenceException;
import org.onap.dcaegen2.services.sdk.services.external.schema.manager.exception.NoLocalReferenceException;
import org.onap.dcaegen2.services.sdk.services.external.schema.manager.service.StndDefinedValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StndDefinedDataValidator {

    private final StndDefinedValidator stndDefinedValidator;

    @Autowired
    public StndDefinedDataValidator(StndDefinedValidatorResolver resolver) {
        this.stndDefinedValidator = resolver.resolve();
    }

    /**
     * Validates incoming event
     *
     * @param event as JsonNode
     * @return validation result as boolean
     * @throws EventValidatorException exceptions related to failing StndDefined Validation
     */
    public boolean validate(JsonNode event) throws EventValidatorException {
        try {
            return stndDefinedValidator.validate(event);
        } catch (NoLocalReferenceException e) {
            throw new EventValidatorException(ApiException.NO_LOCAL_SCHEMA_REFERENCE);
        } catch (IncorrectInternalFileReferenceException e) {
            throw new EventValidatorException(ApiException.INCORRECT_INTERNAL_FILE_REFERENCE);
        }

    }
}
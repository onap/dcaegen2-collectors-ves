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

import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.model.VesEvent;

public class EventValidator {

  private final SchemaValidator schemaValidator;
  private final ApplicationSettings applicationSettings;

  public EventValidator(ApplicationSettings applicationSettings) {
    this(applicationSettings, new SchemaValidator());
  }

  EventValidator(ApplicationSettings applicationSettings,  SchemaValidator schemaValidator) {
    this.applicationSettings = applicationSettings;
    this.schemaValidator = schemaValidator;
  }

  public void validate(VesEvent vesEvent, String type, String version) throws EventValidatorException {
    if (applicationSettings.eventSchemaValidationEnabled()) {
      if (vesEvent.has(type)) {
        if (!schemaValidator.conformsToSchema(vesEvent.asJsonObject(), applicationSettings.jsonSchema(version))) {
          throw new EventValidatorException(ApiException.SCHEMA_VALIDATION_FAILED);
        }
      } else {
        throw new EventValidatorException(ApiException.INVALID_JSON_INPUT);
      }
    }
  }
}

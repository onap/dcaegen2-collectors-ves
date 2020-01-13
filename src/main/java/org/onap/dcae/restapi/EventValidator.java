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

import java.util.Optional;
import org.json.JSONObject;
import org.onap.dcae.ApplicationSettings;
import org.springframework.http.ResponseEntity;
public class EventValidator {

  private final SchemaValidator schemaValidator = new SchemaValidator();
  private ApplicationSettings applicationSettings;

  public EventValidator(ApplicationSettings applicationSettings) {
    this.applicationSettings = applicationSettings;
  }

  public Optional<ResponseEntity<String>> validate(JSONObject jsonObject, String type, String version){
    if (applicationSettings.eventSchemaValidationEnabled()) {
      if (jsonObject.has(type)) {
        if (!schemaValidator.conformsToSchema(jsonObject, applicationSettings.jsonSchema(version))) {
          return errorResponse(ApiException.SCHEMA_VALIDATION_FAILED);
        }
      } else {
        return errorResponse(ApiException.INVALID_JSON_INPUT);
      }
    }
    return Optional.empty();
  }

  private Optional<ResponseEntity<String>> errorResponse(ApiException noServerResources) {
    return Optional.of(ResponseEntity.status(noServerResources.httpStatusCode)
        .body(noServerResources.toJSON().toString()));
  }
}

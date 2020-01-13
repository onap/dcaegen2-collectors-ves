/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2019 Nokia. All rights reserved.s
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

import static java.util.stream.StreamSupport.stream;

import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import java.util.Optional;
import org.json.JSONObject;
import org.onap.dcae.ApplicationException;
import org.onap.dcae.ApplicationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

public class EventValidator {

  private static final Logger log = LoggerFactory.getLogger(EventValidator.class);

  private ApplicationSettings applicationSettings;

  public EventValidator(ApplicationSettings applicationSettings) {
    this.applicationSettings = applicationSettings;
  }

  public Optional<ResponseEntity<String>> validate(JSONObject jsonObject, String type, String version){
    if (applicationSettings.eventSchemaValidationEnabled()) {
      if (jsonObject.has(type)) {
        if (!conformsToSchema(jsonObject, version)) {
          return errorResponse(ApiException.SCHEMA_VALIDATION_FAILED);
        }
      } else {
        return errorResponse(ApiException.INVALID_JSON_INPUT);
      }
    }
    return Optional.empty();
  }

  private boolean conformsToSchema(JSONObject payload, String version) {
    try {
      JsonSchema schema = applicationSettings.eventSchemas(version);
      ProcessingReport report = schema.validate(JsonLoader.fromString(payload.toString()));
      if (report.isSuccess()) {
        return true;
      }
      log.warn("Schema validation failed for event: " + payload);
      stream(report.spliterator(), false).forEach(e -> log.warn(e.getMessage()));
      return false;
    } catch (Exception e) {
      throw new ApplicationException("Unable to validate against schema", e);
    }
  }

  private Optional<ResponseEntity<String>> errorResponse(ApiException noServerResources) {
    return Optional.of(ResponseEntity.status(noServerResources.httpStatusCode)
        .body(noServerResources.toJSON().toString()));
  }
}

/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2018 Nokia. All rights reserved.s
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
package org.onap.dcae;

import static java.nio.file.Files.readAllBytes;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import org.json.JSONObject;
import org.onap.dcae.restapi.VesRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CollectorSchemas {

  private static final Logger LOG = (Logger) LoggerFactory.getLogger(VesRestController.class);

  @Autowired
  private ApplicationSettings collectorProperties;

  //refactor is needed in next iteration
  public Map<String, JsonSchema> getJSONSchemasMap(String version) {
    JSONObject jsonObject = collectorProperties.jsonSchema();
    Map<String, JsonSchema> schemas = jsonObject.toMap().entrySet().stream().map(
        versionToFilePath -> {
          try {
            String schemaContent = new String(
                readAllBytes(Paths.get(versionToFilePath.getValue().toString())));
            JsonNode schemaNode = JsonLoader.fromString(schemaContent);
            JsonSchema schema = JsonSchemaFactory.byDefault().getJsonSchema(schemaNode);
            return new AbstractMap.SimpleEntry<>(versionToFilePath.getKey(), schema);
          } catch (IOException | ProcessingException e) {
            LOG.error("Could not read schema from path: " + versionToFilePath.getValue(), e);
            throw new RuntimeException(
                "Could not read schema from path: " + versionToFilePath.getValue(), e);
          }
        }
    ).collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    if (schemas.get(version) == null && collectorProperties.eventTransformingEnabled()) {
      LOG.error(String.format("Missing necessary %s JSON schema", version));
      throw new RuntimeException(String.format("Missing necessary %s JSON schema", version));
    }
    return schemas;
  }
}
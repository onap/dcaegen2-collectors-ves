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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(SchemaValidator.class);

    //refactor in next iteration
    public static String validateAgainstSchema(String jsonData, String jsonSchema) {
        ProcessingReport report;
        String result = "false";

        try {
            log.trace("Schema validation for event:" + jsonData);
            JsonNode schemaNode = JsonLoader.fromString(jsonSchema);
            JsonNode data = JsonLoader.fromString(jsonData);
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            JsonSchema schema = factory.getJsonSchema(schemaNode);
            report = schema.validate(data);
        } catch (JsonParseException e) {
            log.error("validateAgainstSchema:JsonParseException for event:" + jsonData);
            return e.getMessage();
        } catch (ProcessingException e) {
            log.error("validateAgainstSchema:Processing exception for event:" + jsonData);
            return e.getMessage();
        } catch (IOException e) {
            log.error(
                    "validateAgainstSchema:IO exception; something went wrong trying to read json data for event:" + jsonData);
            return e.getMessage();
        }
        if (report != null) {
            for (ProcessingMessage pm : report) {
                log.trace("Processing Message: " + pm.getMessage());
            }
            result = String.valueOf(report.isSuccess());
        }
        try {
            log.debug("Validation Result:" + result + " Validation report:" + report);
        } catch (NullPointerException e) {
            log.error("validateAgainstSchema:NullpointerException on report");
        }
        return result;
    }
}

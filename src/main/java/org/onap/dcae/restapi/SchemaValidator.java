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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import org.json.JSONObject;
import org.onap.dcae.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

class SchemaValidator {
    public static final Logger log = LoggerFactory.getLogger(SchemaValidator.class);

    public boolean conformsToSchema(JSONObject payload, JsonSchema schema) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            String content = payload.toString();
            JsonNode node = mapper.readTree(content);
            Set<ValidationMessage> messageSet = schema.validate(node);

            if (messageSet.isEmpty()) {
                return true;
            }

            log.warn("Schema validation failed for event: " + payload);
            messageSet.stream().forEach(it->log.warn(it.getMessage()) );

            return false;
        } catch (Exception e) {
            throw new ApplicationException("Unable to validate against schema", e);
        }
    }
}

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
package org.onap.dcae;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Paths;

import static io.vavr.API.Tuple;
import static java.nio.file.Files.readAllBytes;

public class JSonSchemasSupplier {

    public Map<String, JsonSchema> loadJsonSchemas(String collectorSchemaFile) {
        JSONObject jsonObject = new JSONObject(collectorSchemaFile);
        return jsonObject.toMap().entrySet().stream()
            .map(JSonSchemasSupplier::readSchemaForVersion)
            .collect(HashMap.collector());
    }


    private static Tuple2<String, JsonSchema> readSchemaForVersion(java.util.Map.Entry<String, Object> versionToFilePath) {
        try {
            String schemaContent = new String(
                readAllBytes(Paths.get(versionToFilePath.getValue().toString())));
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance();
            JsonSchema schema = factory.getSchema(schemaContent);

            return Tuple(versionToFilePath.getKey(), schema);
        } catch (IOException e) {
            throw new ApplicationException("Could not read schema from path: " + versionToFilePath.getValue(), e);
        }
    }
}

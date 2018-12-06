/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2018 Nokia Networks Intellectual Property. All rights reserved.
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

import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.vavr.API.Set;

/**
 * This class is a wrapper for 2 most used entities of org.json lib: JSONArray and JSONObject and comprises utility
 * methods for fast access of json structures without need to explicitly coerce between them. While using this, bear in
 * mind it does not contain exception handling - it is assumed that when using, the parsed json structure is known.
 *
 * @author koblosz
 */
public class AnyNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnyNode.class);

    private Object obj;

    private AnyNode(Object object) {
        this.obj = object;
    }

    public static AnyNode fromString(String content) {
        return new AnyNode(new JSONObject(content));
    }

    /**
     * Returns key set of underlying object. It is assumed that underlying object is of type org.json.JSONObject.
     */
    public Set<String> keys() {
        return Set(asJsonObject().keySet().toArray(new String[]{}));
    }

    /**
     * Returns value associated with specified key wrapped with AnyValue object. It is assumed that this is of type
     * org.json.JSONObject.
     */
    public AnyNode get(String key) {
        return new AnyNode(asJsonObject().get(key));
    }

    /**
     * Returns string representation of this. If it happens to have null, the value is treated as
     * org.json.JSONObject.NULL and "null" string is returned then.
     */
    public String toString() {
        return this.obj.toString();
    }

    /**
     * Returns optional of object under specified key, wrapped with AnyNode object.
     * If underlying object is not of type org.json.JSONObject
     * or underlying object has no given key
     * or given key is null
     * then Optional.empty will be returned.
     */
    public Option<AnyNode> getAsOption(String key) {
        try {
            AnyNode value = get(key);
            if ("null".equals(value.toString())) {
                return Option.none();
            }
            return Option.some(value);
        } catch (JSONException ex) {
            LOGGER.warn("Cannot create option object cause: ", ex);
            return Option.none();
        }
    }

    /**
     * Converts underlying object to map representation with map values wrapped with AnyNode object. It is assumed that
     * underlying object is of type org.json.JSONObject.
     */
    public List<AnyNode> toList() {
        return List.ofAll(StreamSupport.stream(((JSONArray) this.obj).spliterator(), false).map(AnyNode::new));
    }

    /**
     * Checks if specified key is present in this. It is assumed that this is of type JSONObject.
     */
    public boolean has(String key) {
        return !getAsOption(key).isEmpty();
    }

    private JSONObject asJsonObject() {
        return (JSONObject) this.obj;
    }

}

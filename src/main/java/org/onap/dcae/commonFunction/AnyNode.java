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
package org.onap.dcae.commonFunction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class is a wrapper for 2 most used entities of org.json lib: JSONArray and JSONObject and
 * comprises utility methods for fast access of json structures without need to explicitly coerce between them.
 * While using this, bear in mind it does not contain exception handling - it is assumed that when using, the parsed json structure is known.
 *
 * @author koblosz
 */
public class AnyNode {
    private final Object obj;
    private static final Logger log = LoggerFactory.getLogger(AnyNode.class);

    public static AnyNode parse(String filePath) throws IOException {
        try (FileReader fr = new FileReader(filePath)) {
            return new AnyNode(new JSONObject(new JSONTokener(fr)));
        } catch (FileNotFoundException | JSONException e1) {
            log.error("Could not find or parse file under path %s due to: %s", filePath, e1.toString());
            e1.printStackTrace();
            throw e1;
        }
    }

    /**
     * Returns key set of underlying object. It is assumed that underlying object is of type org.json.JSONObject.
     *
     * @return Set of string keys present in underlying JSONObject
     */
    public Set<String> getKeys() {
        return asJsonObject().keySet();
    }

    /**
     * Returns value associated with specified key wrapped with AnyValue object. It is assumed that this is of type org.json.JSONObject.
     *
     * @param key A key string
     * @return The AnyNode object associated with given key.
     */
    public AnyNode get(String key) {
        return new AnyNode(asJsonObject().get(key));
    }

    /**
     * Returns value under specified index wrapped with AnyValue object. It is assumed that this is of type org.json.JSONArray.
     *
     * @param idx An index of JSONArray
     * @return The AnyNode object associated with given index.
     */
    public AnyNode get(int idx) {
        return new AnyNode(asJsonArray().get(idx));
    }

    /**
     * Returns int assuming this can be coerced to int.
     */
    public int asInt() {
        return (int) this.obj;
    }

    /**
     * Returns string representation of this. If it happens to have null, the value is treated as org.json.JSONObject.NULL and "null" string is returned then.
     *
     * @return A String
     */
    public String asString() {
        return this.obj != JSONObject.NULL ? (String) this.obj : JSONObject.NULL.toString();
    }

    public String toString() {
        return this.obj.toString();
    }

    /**
     * Converts underlying object to String-to-Object map. It is assumed that underlying object is of type org.json.JSONObject.
     *
     * @return A map.
     */
    public Map<String, Object> asRawMap() {
        return asJsonObject().toMap();
    }

    /**
     * Returns optional of object under specified key, wrapped with AnyNode object. If underlying object is not of type org.json.JSONObject, then Optional.empty will be returned.
     *
     * @param key A key string
     */
    public Optional<AnyNode> getAsOptional(String key) {
        AnyNode result = null;
        try {
            result = get(key);
        } catch (JSONException ignored) {
        }
        return Optional.ofNullable(result);
    }

    private JSONObject asJsonObject() {
        return (JSONObject) this.obj;
    }

    /**
     * Converts underlying object to map representation with map values wrapped with AnyNode object. It is assumed that underlying object is of type org.json.JSONObject.
     */
    public Map<String, AnyNode> asMap() {
        Map<String, AnyNode> map = new HashMap<>();
        getKeys().forEach(key -> map.put(key, get(key)));
        return map;
    }

    /**
     * Converts underlying object to map representation with map values wrapped with AnyNode object. It is assumed that underlying object is of type org.json.JSONObject.
     */
    public java.util.List<AnyNode> asList() {
        return asStream().collect(Collectors.toList());
    }

    /**
     * Converts this object to stream of underlying objects wrapped with AnyNode class. It is assumed that this is of type JSONArray.
     */
    private Stream<AnyNode> asStream() {
        return StreamSupport.stream(((JSONArray) this.obj).spliterator(), false).map(AnyNode::new);
    }

    /**
     * Checks if specified key is present in this. It is assumed that this is of type JSONObject.
     */
    boolean hasKey(String key) {
        return getAsOptional(key).isPresent();
    }

    /**
     * Returns empty AnyNode (with null inside)
     */
    public static AnyNode nullValue() {
        return new AnyNode(JSONObject.NULL.toString());
    }

    private JSONArray asJsonArray() {
        return (JSONArray) this.obj;
    }

    private AnyNode(Object object) {
        this.obj = object;
    }

}

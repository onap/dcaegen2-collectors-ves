package org.onap.dcae.commonFunction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileNotFoundException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This class is a wrapper for 2 most used entities of org.json lib: JSONArray and JSONObject and
 * comprises utility methods for fast access of json structures without need to explicitly coerce between them.
 * While using this, bear in mind it does not contain exception handling - it is assumed that when using, the parsed json structure is known.
 *
 * @author koblosz
 */
public class AnyNode {
    private Object obj;

    /**
     * @param reader
     * @return
     * @throws FileNotFoundException
     */
    public static AnyNode parse(Reader reader) throws FileNotFoundException {
        JSONTokener tokener = new JSONTokener(reader);
        return new AnyNode(new JSONObject(tokener));
    }

    /**
     * Returns keyset of underlying object. It is assumed that underlying object is of type org.json.JSONObject.
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
        return this.obj == JSONObject.NULL ? JSONObject.NULL.toString() : (String) this.obj;
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
        } catch (JSONException ex) {
        }
        return Optional.ofNullable(result);
    }

    public JSONObject asJsonObject() {
        return (JSONObject) this.obj;
    }

    public JSONArray asJsonArray() {
        return (JSONArray) this.obj;
    }

    private AnyNode(Object object) {
        this.obj = object;
    }

}

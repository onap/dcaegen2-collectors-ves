/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 * 						reserved.
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


import java.text.DecimalFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigProcessors {

    private static Logger log = LoggerFactory.getLogger(ConfigProcessors.class);
    private static final String FIELD = "field";
    private static final String OLD_FIELD = "oldField";
    private static final String FILTER = "filter";
    private static final String VALUE = "value";
    private static final String REGEX = "\\[\\]";
    private static final String OBJECT_NOT_FOUND = "ObjectNotFound";
    private static final String FILTER_NOT_MET = "Filter not met";

    public ConfigProcessors(JSONObject eventJson) {
        event = eventJson;
    }

    /**
     * Gets event object value.
     * @param j json object
     */
    public void getValue(JSONObject j) {
        //log.info("addAttribute")
        final String field = j.getString(FIELD);
        //final String value = j.getString(VALUE)
        final JSONObject filter = j.optJSONObject(FILTER);
        if (filter == null || isFilterMet(filter)) {
            //log.info("field ==" + field)
            //log.info("value ==" + value)
            getEventObjectVal(field);
        } else {
            log.info(FILTER_NOT_MET);
        }
    }

    /**
     * Sets event object value.
     * @param j json object
     */
    public void setValue(JSONObject j) {
        //log.info("addAttribute")
        final String field = j.getString(FIELD);
        final String value = j.getString(VALUE);
        final JSONObject filter = j.optJSONObject(FILTER);
        if (filter == null || isFilterMet(filter)) {
            //log.info("field ==" + field)
            //log.info("value ==" + value)
            setEventObjectVal(field, value);
        } else {
            log.info(FILTER_NOT_MET);
        }
    }

    /**
     * Evaluate event object value.
     *
     * @param str value
     * @return event object value
     */
    public String evaluate(String str) {
        String value = str;
        if (str.startsWith("$")) {
            value = (String) getEventObjectVal(str.substring(1));
        }
        return value;
    }

    /**
     * { "functionName":"suppressEvent",
     "args":{}
     }
     */
    /**
     * Suppress event.
     * @param j json object
     */
    public void suppressEvent(JSONObject j) {
        //log.info("addAttribute")
        final JSONObject filter = j.optJSONObject(FILTER);

        if (filter == null || isFilterMet(filter)) {
            //log.info("field ==" + field)
            //log.info("value ==" + value)
            setEventObjectVal("suppressEvent", "true");
        } else {
            log.info(FILTER_NOT_MET);
        }
    }

    /**
     * Add attribute.
     * @param j json object
     */
    public void addAttribute(JSONObject j) {
        //log.info("addAttribute begin")
        final String field = j.getString(FIELD);
        final String value = evaluate(j.getString(VALUE));
        final JSONObject filter = j.optJSONObject(FILTER);
        final String fieldType = j.optString("fieldType", "string").toLowerCase();

        if (filter == null || isFilterMet(filter)) {
            //log.info("field ==" + field)
            //log.info("value ==" + value)
            setEventObjectVal(field, value, fieldType);
        } else {
            log.info(FILTER_NOT_MET);
        }
        //log.info("addAttribute End")
    }

    /**
     * update attribute.
     * @param j json object
     */
    public void updateAttribute(JSONObject j) {
        //log.info("updateAttribute")
        final String field = j.getString(FIELD);
        final String value = evaluate(j.getString(VALUE));
        final JSONObject filter = j.optJSONObject(FILTER);
        if (filter == null || isFilterMet(filter)) {
            //log.info("field ==" + field)
            //log.info("value ==" + value)
            setEventObjectVal(field, value);
        } else {
            log.info(FILTER_NOT_MET);
        }
    }

    /**
     * Remove attribute.
     * @param j json object
     */
    public void removeAttribute(JSONObject j) {
        //log.info("removeAttribute")
        final String field = j.getString(FIELD);
        final JSONObject filter = j.optJSONObject(FILTER);

        if (filter == null || isFilterMet(filter)) {
            removeEventKey(field);
        } else {
            log.info(FILTER_NOT_MET);
        }
    }

    /**
     * Rename field.
     * @param j json object
     */
    public void renameArrayInArray(JSONObject j) {//map
        log.info("renameArrayInArray");
        final String field = j.getString(FIELD);
        final String oldField = j.getString(OLD_FIELD);
        final JSONObject filter = j.optJSONObject(FILTER);
        //String value = ""
        if (filter == null || isFilterMet(filter)) {
            //log.info("field ==" + field)
            final String[] fsplit = field.split(REGEX, field.length());
            final String[] oldfsplit = oldField.split(REGEX, oldField.length());

            final String oldValue = getEventObjectVal(oldfsplit[0]).toString();
            if (!oldValue.equals(OBJECT_NOT_FOUND)) {
                final String oldArrayName = oldfsplit[1].substring(1);
                final String newArrayName = fsplit[1].substring(1);
                final String value = oldValue.replaceAll(oldArrayName, newArrayName);
                //log.info("oldArrayName ==" + oldArrayName)
                //log.info("newArrayName ==" + newArrayName)
                log.info("oldValue ==" + oldValue);
                log.info("value ==" + value);
                JSONArray ja = new JSONArray(value);
                removeEventKey(oldfsplit[0]);
                setEventObjectVal(fsplit[0], ja);
            }
        } else {
            log.info(FILTER_NOT_MET);
        }
    }

    /**
     * map attribute.
     * @param j json object
     */
    public void map(JSONObject j) {
        //log.info("mapAttribute")
        final String field = j.getString(FIELD);
        if (field.contains("[]")) {
            if (field.matches(".*\\[\\]\\..*\\[\\]")) {
                renameArrayInArray(j);
            } else {
                mapToJArray(j);
            }
        } else {
            mapAttribute(j);
        }
    }

    /**
     * convert MB to KB.
     * @param operation operation value
     * @param value value
     * @return converted value
     */
    public String performOperation(String operation, String value) {
        log.info("performOperation");
        if (operation != null && "convertMBtoKB".equals(operation)) {
            float kbValue = Float.parseFloat(value) * 1024;
            return String.valueOf(kbValue);
        }
        return value;
    }

    //public void mapAttributeToArrayAttribute(JSONObject j)

    /**
     * map attribute.
     * @param j json object
     */
    public void mapAttribute(JSONObject j) {
        //log.info("mapAttribute")
        final String field = j.getString(FIELD);
        final String oldField = j.getString(OLD_FIELD);
        final JSONObject filter = j.optJSONObject(FILTER);
        final String operation = j.optString("operation");
        String value = "";
        if (filter == null || isFilterMet(filter)) {
            //log.info("field ==" + field)

            value = getEventObjectVal(oldField).toString();
            if (!value.equals(OBJECT_NOT_FOUND)) {
                if (operation != null && !operation.equals("")) {
                    value = performOperation(operation, value);
                }
                //log.info("value ==" + value)
                setEventObjectVal(field, value);

                removeEventKey(oldField);
            }
        } else {
            log.info(FILTER_NOT_MET);
        }
    }

    /**
     * map attribute to json array.
     * @param j json object
     */
    public void mapToJArray(JSONObject j) {
        log.info("mapToJArray");
        String field = j.getString(FIELD);
        String oldField = j.getString(OLD_FIELD);
        final JSONObject filter = j.optJSONObject(FILTER);
        final JSONObject attrMap = j.optJSONObject("attrMap");
        oldField = oldField.replaceAll(REGEX, "");
        field = field.replaceAll(REGEX, "");

        //log.info("oldField ==" + field)
        if (filter == null || isFilterMet(filter)) {
            //log.info("oldField ==" + field)
            String value = getEventObjectVal(oldField).toString();
            if (!value.equals(OBJECT_NOT_FOUND)) {
                log.info("old value ==" + value);
                handleObjectNotFound(attrMap, value, field, oldField);
            }
        } else {
            log.info(FILTER_NOT_MET);
        }
    }

    private void handleObjectNotFound(JSONObject attrMap, String value, String field, String oldField) {
        String temp = value;
        if (attrMap != null) {
            //loop through attrMap and update attribute name to new name
            for (String key : attrMap.keySet()) {
                //log.info("attr key==" + key + " temp==" + attrMap.getString(key))
                temp = temp.replaceAll(key, attrMap.getString(key));
            }
        }

        log.info("new value ==" + temp);
        char c = temp.charAt(0);
        if (c != '[') {
            //oldfield is JsonObject
            JSONObject valueJO = new JSONObject(temp);
            // if the array already exists

            String existingValue = getEventObjectVal(field).toString();
            if (!existingValue.equals(OBJECT_NOT_FOUND)) {
                JSONArray ja = new JSONArray(existingValue);
                JSONObject jo = ja.optJSONObject(0);
                if (jo != null) {
                    for (String key : valueJO.keySet()) {
                        jo.put(key, valueJO.get(key));
                    }
                    ja.put(0, jo);
                    //log.info("jarray== " + ja.toString())
                    setEventObjectVal(field, ja);
                }
            } else { //if new array
                setEventObjectVal(field + "[0]", new JSONObject(temp), "JArray");
            }
        } else { //oldfield is jsonArray
            setEventObjectVal(field, new JSONArray(temp));
        }

        removeEventKey(oldField);
    }

    /**
     * example -
     {
     "functionName": "concatenateValue",
     "args":{
     "filter": {"event.commonEventHeader.event":"heartbeat"},
     FIELD:"event.commonEventHeader.eventName",
     "concatenate": ["event.commonEventHeader.domain","event.commonEventHeader.eventType","event.commonEventHeader.alarmCondition"],
     "delimiter":"_"
     }
     }
     **/
    /**
     * Concatenate value.
     * @param j json obj
     */
    public void concatenateValue(JSONObject j) {
        //log.info("concatenateValue")
        final String field = j.getString(FIELD);
        final String delimiter = j.getString("delimiter");
        final JSONArray values = j.getJSONArray("concatenate");
        final JSONObject filter = j.optJSONObject(FILTER);
        if (filter == null || isFilterMet(filter)) {
            StringBuilder value = new StringBuilder();
            for (int i = 0; i < values.length(); i++) {
                //log.info(values.getString(i))
                String tempVal = evaluate(values.getString(i));
                if (!tempVal.equals(OBJECT_NOT_FOUND)) {
                    if (i == 0) {
                        value.append(tempVal);
                    } else {
                        value.append(delimiter).append(tempVal);
                    }
                }
            }
            //log.info("value ==" + value)
            setEventObjectVal(field, value.toString());
        } else {
            log.info(FILTER_NOT_MET);
        }
    }

    /**
     * Subtract value.
     * @param j json obj
     */
    public void subtractValue(JSONObject j) {
        //log.info("concatenateValue")
        final String field = j.getString(FIELD);
        final JSONArray values = j.getJSONArray("subtract");
        final JSONObject filter = j.optJSONObject(FILTER);
        if (filter == null || isFilterMet(filter)) {
            float value = 0;
            for (int i = 0; i < values.length(); i++) {
                log.info(values.getString(i));
                String tempVal = evaluate(values.getString(i));
                log.info("tempVal==" + tempVal);
                if (!tempVal.equals(OBJECT_NOT_FOUND)) {
                    if (i == 0) {
                        value = value + Float.valueOf(tempVal);
                    } else {
                        value = value - Float.valueOf(tempVal);
                    }
                }
            }
            log.info("value ==" + value);
            setEventObjectVal(field, value, "number");
        } else {
            log.info(FILTER_NOT_MET);
        }
    }

    /**
     *
     */
    private void removeEventKey(String field) {
        String[] keySet = field.split("\\.", field.length());
        JSONObject keySeries = event;
        for (int i = 0; i < (keySet.length - 1); i++) {
            //log.info( i + " ==" + keySet[i])
            keySeries = keySeries.getJSONObject(keySet[i]);
        }
        //log.info(keySet[keySet.length -1])

        keySeries.remove(keySet[keySet.length - 1]);
    }

    /**
     *
     */
    private boolean checkFilter(JSONObject jo, String key, String logicKey) {
        String filterValue = jo.getString(key);
        boolean retVal = true;

        if (filterValue.contains(":")) {
            String[] splitVal = filterValue.split(":");
            //log.info(splitVal[0] + " " + splitVal[1])
            if ("matches".equals(splitVal[0]) || "contains".equals(splitVal[0])) {
                retVal = handleLogicKeyValue(splitVal[1], logicKey, filterValue, key);
            }
        } else {
            retVal = handleLogicKeyValue(filterValue, logicKey, filterValue, key);
        }
        return retVal;
    }

    private boolean handleLogicKeyValue(String val, String logicKey, String filterValue, String key) {
        if ("not".equals(logicKey)) {
            if (getEventObjectVal(key).toString().equals(val)) {
                logFilterValues(filterValue, key);
                return false;
            }
        } else {
            if (!(getEventObjectVal(key).toString().equals(val))) {
                logFilterValues(filterValue, key);
                return false;
            }
        }
        return true;
    }

    private void logFilterValues(String filterValue, String key) {
        log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + "==false");
    }

    /**
     * check if filter met
     * @param jo json obj
     * @return true/false
     */
    public boolean isFilterMet(JSONObject jo) {
        boolean retVal;
        //log.info("Filter==" + jo.toString())
        for (String key : jo.keySet()) {
            if (key.equals("not")) {
                JSONObject njo = jo.getJSONObject(key);
                for (String njoKey : njo.keySet()) {
                    //log.info(njoKey)
                    retVal = checkFilter(njo, njoKey, key);
                    if (!retVal) {
                        return retVal;
                    }
                }
            } else {
                //log.info(key)
                //final String filterKey = key
                retVal = checkFilter(jo, key, key);
                if (!retVal) {
                    return retVal;
                }
            }
        }
        return true;
    }

    /**
     * @param  keySeriesStr key series string
     * @return a string or JSONObject or JSONArray
     **/
    public Object getEventObjectVal(String keySeriesStr) {
        String tempObj = keySeriesStr;
        tempObj = tempObj.replaceAll("\\[", ".");
        tempObj = tempObj.replaceAll("\\]", ".");
        if (tempObj.contains("..")) {
            tempObj = tempObj.replaceAll("\\.\\.", ".");
        }
        //log.info(Integer.toString(tempObj.lastIndexOf(".")))
        //log.info(Integer.toString(tempObj.length() -1))
        if (tempObj.lastIndexOf('.') == tempObj.length() - 1) {
            tempObj = tempObj.substring(0, tempObj.length() - 1);
        }
        String[] keySet = tempObj.split("\\.", tempObj.length());
        Object keySeriesObj = event;
        for (String aKeySet : keySet) {
            //log.info( "getEventObject " + i + " ==" + keySet[i])
            if (keySeriesObj != null) {
                if (keySeriesObj instanceof String) {
                    //keySeriesObj =  keySeriesObj.get(keySet[i])
                    log.info("STRING==" + keySeriesObj);
                } else if (keySeriesObj instanceof JSONArray) {
                    keySeriesObj = ((JSONArray) keySeriesObj).optJSONObject(Integer.parseInt(aKeySet));
                    //log.info("ARRAY==" + keySeriesObj)
                } else if (keySeriesObj instanceof JSONObject) {
                    keySeriesObj = ((JSONObject) keySeriesObj).opt(aKeySet);
                    //log.info("JSONObject==" + keySeriesObj)
                } else {
                    log.info("unknown object==" + keySeriesObj);
                }
            }
        }

        if (keySeriesObj == null) {
            return OBJECT_NOT_FOUND;
        }
        return keySeriesObj;
    }

    /**
     * Set event object value
     * @param keySeriesStr key series string
     * @param value value
     */
    public void setEventObjectVal(String keySeriesStr, Object value) {
        setEventObjectVal(keySeriesStr, value, "string");
    }

    /**
     * @param fieldType field type
     * @param keySeriesStr key series string
     * @param value value
     **/
    public void setEventObjectVal(String keySeriesStr, Object value, String fieldType) {
        String tempObj = keySeriesStr;
        tempObj = tempObj.replaceAll("\\[", ".");
        tempObj = tempObj.replaceAll("\\]", ".");
        if (tempObj.contains("..")) {
            tempObj = tempObj.replaceAll("\\.\\.", ".");
        }
        log.info("fieldType==" + fieldType);
        //log.info(Integer.toString(tempObj.lastIndexOf(".")))
        //log.info(Integer.toString(tempObj.length() -1))
        if (tempObj.lastIndexOf('.') == tempObj.length() - 1) {
            tempObj = tempObj.substring(0, tempObj.length() - 1);
        }
        String[] keySet = tempObj.split("\\.", tempObj.length());
        Object keySeriesObj = event;
        for (int i = 0; i < (keySet.length - 1); i++) {
            //log.info( "setEventObject " + i + " ==" + keySet[i])
            if (keySeriesObj instanceof JSONArray) {
                //keySeriesObj =  ((JSONArray) keySeriesObj).optJSONObject(Integer.parseInt(keySet[i]))
                keySeriesObj = handleWhenJsonArray(keySeriesObj, keySet, i);
                //log.info("ARRAY==" + keySeriesObj)
            } else if (keySeriesObj instanceof JSONObject) {
                keySeriesObj = handleWhenJsonObject(keySeriesObj, keySet, i);
                //log.info("JSONObject==" + keySeriesObj)
            } else {
                log.info("unknown object==" + keySeriesObj);
            }
        }
        if (fieldType.equals("number")) {
            DecimalFormat df = new DecimalFormat("#.0");
            if (value instanceof String) {
                ((JSONObject) keySeriesObj)
                    .put(keySet[keySet.length - 1], Float.valueOf(df.format(Float.valueOf((String) value))));
            } else {
                ((JSONObject) keySeriesObj).put(keySet[keySet.length - 1], Float.valueOf(df.format(value)));
            }
        } else if (fieldType.equals("integer") && value instanceof String) {
            ((JSONObject) keySeriesObj).put(keySet[keySet.length - 1], Integer.valueOf((String) value));
        } else if (fieldType.equals("JArray")) {
            ((JSONArray) keySeriesObj).put(value);
        } else {
            ((JSONObject) keySeriesObj).put(keySet[keySet.length - 1], value);
        }
    }

    private Object handleWhenJsonArray(Object keySeriesObj, String[] keySet, int i) {
        if (((JSONArray) keySeriesObj).optJSONObject(Integer.parseInt(keySet[i])) == null) {
            //if the object is not there then add it
            log.info("Object is null, must add it");
            // if index then array
            if (keySet[i + 1].matches("[0-9]*")) {
                ((JSONArray) keySeriesObj).put(Integer.parseInt(keySet[i]), new JSONArray());
            } else {
                ((JSONArray) keySeriesObj).put(Integer.parseInt(keySet[i]), new JSONObject());
            }
        }
        return ((JSONArray) keySeriesObj).optJSONObject(Integer.parseInt(keySet[i]));
    }

    private Object handleWhenJsonObject(Object keySeriesObj, String[] keySet, int i) {
        if (((JSONObject) keySeriesObj).opt(keySet[i]) == null) {//if the object is not there then add it
            if (keySet[i + 1].matches("[0-9]*")) {// if index then array
                ((JSONObject) keySeriesObj).put(keySet[i], new JSONArray());
            } else {
                ((JSONObject) keySeriesObj).put(keySet[i], new JSONObject());
            }
            log.info("Object is null, must add it");
        }
        return ((JSONObject) keySeriesObj).opt(keySet[i]);
    }

    private JSONObject event = new JSONObject();
}


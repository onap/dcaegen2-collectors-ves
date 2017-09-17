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

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigProcessors {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigProcessors.class);
    private static final String FIELD = "field";
    private static final String OLD_FIELD = "oldField";
    private static final String FILTER = "filter";
    private static final String VALUE = "value";
    private static final String REGEX = "\\[\\]";
    private static final String OBJECT_NOT_FOUND = "ObjectNotFound";


    public ConfigProcessors(JSONObject eventJson) {
        event = eventJson;
    }

    /**
     *
     * @param j json object
     */
    public void getValue(JSONObject j) {
        //log.info("addAttribute");
        final String field = j.getString(FIELD);
        //final String value = J.getString(VALUE);
        final JSONObject filter = j.optJSONObject(FILTER);
        if (filter == null || isFilterMet(filter)) {
            //log.info("field ==" + field);
            //log.info("value ==" + value);
            getEventObjectVal(field);
        } else {
            LOG.info("Filter not met");
        }
    }

    /**
     *
     * @param j json object
     */
    public void setValue(JSONObject j) {
        //log.info("addAttribute");
        final String field = j.getString(FIELD);
        final String value = j.getString(VALUE);
        final JSONObject filter = j.optJSONObject(FILTER);
        if (filter == null || isFilterMet(filter)) {
            //log.info("field ==" + field);
            //log.info("value ==" + value);
            setEventObjectVal(field, value);
        } else {
            LOG.info("Filter not met");
        }
    }

    /**
     *
     * @param j json object
     */
    public void addAttribute(JSONObject j) {
        //log.info("addAttribute");
        final String field = j.getString(FIELD);
        final String value = j.getString(VALUE);
        final JSONObject filter = j.optJSONObject(FILTER);
        if (filter == null || isFilterMet(filter)) {
            //log.info("field ==" + field);
            //log.info("value ==" + value);
            setEventObjectVal(field, value);
        } else {
            LOG.info("Filter not met");
        }
    }

    /**
     *
     * @param j json object
     */
    public void updateAttribute(JSONObject j) {
        //log.info("updateAttribute");
        final String field = j.getString(FIELD);
        final String value = j.getString(VALUE);
        final JSONObject filter = j.optJSONObject(FILTER);
        if (filter == null || isFilterMet(filter)) {
            //log.info("field ==" + field);
            //log.info("value ==" + value);
            setEventObjectVal(field, value);
        } else {
            LOG.info("Filter not met");
        }
    }

    /**
     *
     * @param j json object
     */
    public void removeAttribute(JSONObject j) {
        //log.info("removeAttribute");
        final String field = j.getString(FIELD);
        final JSONObject filter = j.optJSONObject(FILTER);

        if (filter == null || isFilterMet(filter)) {
            removeEventKey(field);
        } else {
            LOG.info("Filter not met");
        }
    }

    /**
     *
     * @param j json object
     */
    public void renameArrayInArray(JSONObject j) { //map

        LOG.info("renameArrayInArray");
        final String field = j.getString(FIELD);
        final String oldField = j.getString(OLD_FIELD);
        final JSONObject filter = j.optJSONObject(FILTER);
        //String value = "";
        if (filter == null || isFilterMet(filter)) {
            //log.info("field ==" + field);
            final String[] fsplit = field.split(REGEX, field.length());
            final String[] oldfsplit = oldField.split(REGEX, oldField.length());
                /*for (int i=0; i< oldfsplit.length; i++ )
                {
		    		log.info( "renameArrayInArray " + i + " ==" + oldfsplit[i]);
		    	}*/

            final String oldValue = getEventObjectVal(oldfsplit[0]).toString();
            if (!OBJECT_NOT_FOUND.equals(oldValue)) {
                final String oldArrayName = oldfsplit[1].substring(1);
                final String newArrayName = fsplit[1].substring(1);
                final String value = oldValue.replaceAll(oldArrayName, newArrayName);
                //log.info("oldArrayName ==" + oldArrayName);
                //log.info("newArrayName ==" + newArrayName);
                LOG.info("oldValue ==" + oldValue);
                LOG.info("value ==" + value);
                JSONArray ja = new JSONArray(value);
                removeEventKey(oldfsplit[0]);
                setEventObjectVal(fsplit[0], ja);
            }
        } else {
            LOG.info("Filter not met");
        }
    }

    /**
     *
     * @param j json object
     */
    public void map(JSONObject j) {
        //log.info("mapAttribute");
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
     *
     * @param  operation operation
     * @param value kb value
     * @return value
     */
    public String performOperation(String operation, String value) {
        LOG.info("performOperation");
        if (operation != null && "convertMBtoKB".equals(operation)) {
            float kbValue = Float.parseFloat(value) * 1024;
            value = String.valueOf(kbValue);
        }
        return value;
    }

    //public void mapAttributeToArrayAttribute(JSONObject J)

    /**
     *
     * @param j json object
     */
    public void mapAttribute(JSONObject j) {
        //log.info("mapAttribute");
        final String field = j.getString(FIELD);
        final String oldField = j.getString(OLD_FIELD);
        final JSONObject filter = j.optJSONObject(FILTER);
        final String operation = j.optString("operation");
        String value = "";
        if (filter == null || isFilterMet(filter)) {
            //log.info("field ==" + field);

            value = getEventObjectVal(oldField).toString();
            if (!OBJECT_NOT_FOUND.equals(value)) {
                if (operation != null && !operation.isEmpty()) {
                    value = performOperation(operation, value);
                }
                //log.info("value ==" + value);
                setEventObjectVal(field, value);

                removeEventKey(oldField);
            }
        } else {
            LOG.info("Filter not met");
        }
    }

    /**
     *
     * @param j json object
     */
    public void mapToJArray(JSONObject j) {
        LOG.info("mapToJArray");
        String field = j.getString(FIELD);
        String oldField = j.getString(OLD_FIELD);
        final JSONObject filter = j.optJSONObject(FILTER);
        final JSONObject attrMap = j.optJSONObject("attrMap");
        oldField = oldField.replaceAll(REGEX, "");
        field = field.replaceAll(REGEX, "");

        //log.info("oldField ==" + field);
        if (filter == null || isFilterMet(filter)) {
            //log.info("oldField ==" + field);
            String value = getEventObjectVal(oldField).toString();
            if (!OBJECT_NOT_FOUND.equals(value)) {
                LOG.info("old value ==" + value);
                //update old value based on attrMap
                if (attrMap != null) {
                    //loop thru attrMap and update attribute name to new name
                    for (String key : attrMap.keySet()) {
                        //log.info("attr key==" + key + " value==" + attrMap.getString(key));
                        value = value.replaceAll(key, attrMap.getString(key));
                    }
                }

                LOG.info("new value ==" + value);
                char c = value.charAt(0);
                if (c != '[') {
                    //oldfield is JsonObject
                    JSONObject valueJO = new JSONObject(value);
                    // if the array already exists

                    String existingValue = getEventObjectVal(field).toString();
                    if (!OBJECT_NOT_FOUND.equals(existingValue)) {
                        JSONArray ja = new JSONArray(existingValue);
                        JSONObject jo = ja.optJSONObject(0);
                        if (jo != null) {
                            for (String key : valueJO.keySet()) {
                                jo.put(key, valueJO.get(key));

                            }
                            ja.put(0, jo);
                            //log.info("jarray== " + ja.toString());
                            setEventObjectVal(field, ja);
                        }
                    } else //if new array
                    {
                        setEventObjectVal(field + "[0]", new JSONObject(value));
                    }
                } else //oldfield is jsonArray
                {
                    setEventObjectVal(field, new JSONArray(value));
                }

                removeEventKey(oldField);
            }
        } else {
            LOG.info("Filter not met");
        }
    }

    /*
     * example -
     {
     "functionName": "concatenateValue",
     "args":{
     "filter": {"event.commonEventHeader.event":"heartbeat"},
     "field":"event.commonEventHeader.eventName",
     "concatenate": ["event.commonEventHeader.domain","event.commonEventHeader.eventType","event.commonEventHeader.alarmCondition"],
     "delimiter":"_"
     }
     }
     **/

    /**
     *
     * @param j json object
     */
    public void concatenateValue(JSONObject j) {
        //log.info("concatenateValue");
        final String field = j.getString(FIELD);
        final String delimiter = j.getString("delimiter");
        final JSONArray values = j.getJSONArray("concatenate");
        final JSONObject filter = j.optJSONObject(FILTER);
        if (filter == null || isFilterMet(filter)) {
            StringBuilder value = new StringBuilder("");
            for (int i = 0; i < values.length(); i++) {
                //log.info(values.getString(i));
                String tempVal = getEventObjectVal(values.getString(i)).toString();
                if (!OBJECT_NOT_FOUND.equals(tempVal)) {
                    if (i == 0) {
                        value.append(getEventObjectVal(values.getString(i)));
                    } else {
                        value.append(delimiter).append(getEventObjectVal(values.getString(i)));
                    }
                }
            }
            //log.info("value ==" + value);
            setEventObjectVal(field, value.toString());
        } else {
            LOG.info("Filter not met");
        }
    }

    /**
     *
     */
    private void removeEventKey(String field) {
        String[] keySet = field.split("\\.", field.length());
        JSONObject keySeries = event;
        for (int i = 0; i < (keySet.length - 1); i++) {
            //log.info( i + " ==" + keySet[i]);
            keySeries = keySeries.getJSONObject(keySet[i]);
        }
        //log.info(keySet[keySet.length -1]);

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
            //log.info(splitVal[0] + " " + splitVal[1]);
            if ("matches".equals(splitVal[0])) {
                if ("not".equals(logicKey)) {
                    //log.info("not");
                    //log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + "split1==" + splitVal[1]);
                    if (getEventObjectVal(key).toString().matches(splitVal[1])) {
                        loggerForCheckFilter(filterValue, key);
                        return false;
                    }
                } else {
                    if (!getEventObjectVal(key).toString().matches(splitVal[1])) {
                        loggerForCheckFilter(filterValue, key);
                        return false;
                    }
                }

            }
            if ("contains".equals(splitVal[0])) {
                if ("not".equals(logicKey)) {
                    //log.info("not");
                    //log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + "split1==" + splitVal[1]);
                    if (getEventObjectVal(key).toString().contains(splitVal[1])) {
                        loggerForCheckFilter(filterValue, key);
                        return false;
                    }
                } else {
                    if (!getEventObjectVal(key).toString().contains(splitVal[1])) {
                        loggerForCheckFilter(filterValue, key);
                        return false;
                    }
                }

            }
        } else {
            if ("not".equals(logicKey)) {
                if (getEventObjectVal(key).toString().equals(filterValue)) {
                    loggerForCheckFilter(filterValue, key);
                    return false;
                }
            } else {
                if (!getEventObjectVal(key).toString().equals(filterValue)) {
                    loggerForCheckFilter(filterValue, key);
                    return false;
                }
            }
        }
        return retVal;
    }

    /**
     *
     * @param jo json object
     * @return true/false
     */
    public boolean isFilterMet(JSONObject jo) {
        boolean retval;
        //log.info("Filter==" + jo.toString());
        for (String key : jo.keySet()) {
            if ("not".equals(key)) {
                JSONObject njo = jo.getJSONObject(key);
                for (String njoKey : njo.keySet()) {
                    //log.info(njoKey);
                    retval = checkFilter(njo, njoKey, key);
                    if (!retval) {
                        return retval;
                    }
                }
            } else {
                //log.info(key);
                //final String filterKey = key;
                retval = checkFilter(jo, key, key);
                if (!retval) {
                    return retval;
                }
            }
        }
        return true;
    }

    /**
     * returns a string or JSONObject or JSONArray
     *
     * @param keySeriesStr key series string
     * @return key string updated object
     **/
    public Object getEventObjectVal(String keySeriesStr) {
        keySeriesStr = keySeriesStr.replaceAll("\\[", ".");
        keySeriesStr = keySeriesStr.replaceAll("\\]", ".");
        if (keySeriesStr.contains("..")) {
            keySeriesStr = keySeriesStr.replaceAll("\\.\\.", ".");
        }
        //log.info(Integer.toString(keySeriesStr.lastIndexOf(".")));
        //log.info(Integer.toString(keySeriesStr.length() -1));
        if (keySeriesStr.lastIndexOf('.') == keySeriesStr.length() - 1) {
            keySeriesStr = keySeriesStr.substring(0, keySeriesStr.length() - 1);
        }
        String[] keySet = keySeriesStr.split("\\.", keySeriesStr.length());
        Object keySeriesObj = event;
        for (int i = 0; i < keySet.length; i++) {
            //log.info( "getEventObject " + i + " ==" + keySet[i]);
            if (keySeriesObj != null) {
                if (keySeriesObj instanceof String) {
                    //keySeriesObj =  keySeriesObj.get(keySet[i]);
                    LOG.info("STRING==" + keySeriesObj);
                } else if (keySeriesObj instanceof JSONArray) {
                    keySeriesObj = ((JSONArray) keySeriesObj)
                        .optJSONObject(Integer.parseInt(keySet[i]));
                    //log.info("ARRAY==" + keySeriesObj);
                } else if (keySeriesObj instanceof JSONObject) {
                    keySeriesObj = ((JSONObject) keySeriesObj).opt(keySet[i]);
                    //log.info("JSONObject==" + keySeriesObj);
                } else {
                    LOG.info("unknown object==" + keySeriesObj);
                }
            }
        }

        if (keySeriesObj == null) {
            return "ObjectNotFound";
        }
        return keySeriesObj;
    }

    /**
     * returns a string or JSONObject or JSONArray
     *
     * @param keySeriesStr key series string
     * @param value value object
     **/
    public void setEventObjectVal(String keySeriesStr, Object value) {
        keySeriesStr = keySeriesStr.replaceAll("\\[", ".");
        keySeriesStr = keySeriesStr.replaceAll("\\]", ".");
        if (keySeriesStr.contains("..")) {
            keySeriesStr = keySeriesStr.replaceAll("\\.\\.", ".");
        }
        //log.info(Integer.toString(keySeriesStr.lastIndexOf(".")));
        //log.info(Integer.toString(keySeriesStr.length() -1));
        if (keySeriesStr.lastIndexOf('.') == keySeriesStr.length() - 1) {
            keySeriesStr = keySeriesStr.substring(0, keySeriesStr.length() - 1);
        }
        String[] keySet = keySeriesStr.split("\\.", keySeriesStr.length());
        Object keySeriesObj = event;
        for (int i = 0; i < (keySet.length - 1); i++) {
            //log.info( "setEventObject " + i + " ==" + keySet[i]);
            if (keySeriesObj instanceof JSONArray) {
                //keySeriesObj =  ((JSONArray) keySeriesObj).optJSONObject(Integer.parseInt(keySet[i]));
                if (((JSONArray) keySeriesObj).optJSONObject(Integer.parseInt(keySet[i]))
                    == null) //if the object is not there then add it
                {
                    LOG.info("Object is null, must add it");
                    if (keySet[i + 1].matches("[0-9]*")) // if index then array
                    {
                        ((JSONArray) keySeriesObj)
                            .put(Integer.parseInt(keySet[i]), new JSONArray());
                    } else {
                        ((JSONArray) keySeriesObj)
                            .put(Integer.parseInt(keySet[i]), new JSONObject());
                    }
                }
                keySeriesObj = ((JSONArray) keySeriesObj)
                    .optJSONObject(Integer.parseInt(keySet[i]));
                //log.info("ARRAY==" + keySeriesObj);
            } else if (keySeriesObj instanceof JSONObject) {
                if (((JSONObject) keySeriesObj).opt(keySet[i])
                    == null) //if the object is not there then add it
                {
                    if (keySet[i + 1].matches("[0-9]*")) // if index then array
                    {
                        ((JSONObject) keySeriesObj).put(keySet[i], new JSONArray());
                    } else {
                        ((JSONObject) keySeriesObj).put(keySet[i], new JSONObject());
                    }
                    LOG.info("Object is null, must add it");
                }
                keySeriesObj = ((JSONObject) keySeriesObj).opt(keySet[i]);
                //log.info("JSONObject==" + keySeriesObj);
            } else {
                LOG.info("unknown object==" + keySeriesObj);
            }
        }

        ((JSONObject) keySeriesObj).put(keySet[keySet.length - 1], value);
    }

    private JSONObject event = new JSONObject();

    private void loggerForCheckFilter(String filterValue, String key) {
        LOG.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + "==false");
    }
}

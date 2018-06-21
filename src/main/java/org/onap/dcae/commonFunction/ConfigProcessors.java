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

	private static final Logger log = LoggerFactory.getLogger(ConfigProcessors.class);
	private static final String FIELD = "field";
	private static final String OLD_FIELD = "oldField";
	private static final String FILTER = "filter";
	private static final String VALUE = "value";
	private static final String REGEX = "\\[\\]";
	private static final String OBJECT_NOT_FOUND = "ObjectNotFound";
	private static final String FILTER_NOT_MET = "Filter not met";
	private static final String COMP_FALSE = "==false";

	private final JSONObject event;

	public ConfigProcessors(JSONObject eventJson) {
		event = eventJson;
	}

	public void getValue(JSONObject jsonObject) {

		final String field = jsonObject.getString(FIELD);
		final JSONObject filter = jsonObject.optJSONObject(FILTER);

		if (filter == null || isFilterMet(filter)) {
			getEventObjectVal(field);
		} else
			log.info(FILTER_NOT_MET);
	}


	public void setValue(JSONObject jsonObject) {
		final String field = jsonObject.getString(FIELD);
		final String value = jsonObject.getString(VALUE);
		final JSONObject filter = jsonObject.optJSONObject(FILTER);
		if (filter == null || isFilterMet(filter)) {
			setEventObjectVal(field, value);
		} else
			log.info(FILTER_NOT_MET);
	}



	private String evaluate(String str) {
		String value = str;
		if (str.startsWith("$")) {
			value = (String) getEventObjectVal(str.substring(1));

		}
		return value;
	}


	public void suppressEvent(JSONObject jsonObject) {
		final JSONObject filter = jsonObject.optJSONObject(FILTER);

		if (filter == null || isFilterMet(filter)) {
			setEventObjectVal("suppressEvent", "true");
		} else
			log.info(FILTER_NOT_MET);
	}


	public void addAttribute(JSONObject jsonObject) {

		final String field = jsonObject.getString(FIELD);
		final String value = evaluate(jsonObject.getString(VALUE));
		final JSONObject filter = jsonObject.optJSONObject(FILTER);
		final String fieldType = jsonObject.optString("fieldType", "string").toLowerCase();

		if (filter == null || isFilterMet(filter)) {
			setEventObjectVal(field, value, fieldType);
		} else
			log.info(FILTER_NOT_MET);
	}


	public void updateAttribute(JSONObject jsonObject) {

		final String field = jsonObject.getString(FIELD);
		final String value = evaluate(jsonObject.getString(VALUE));
		final JSONObject filter = jsonObject.optJSONObject(FILTER);
		if (filter == null || isFilterMet(filter)) {
			setEventObjectVal(field, value);
		} else
			log.info(FILTER_NOT_MET);
	}


	public void removeAttribute(JSONObject jsonObject) {

		final String field = jsonObject.getString(FIELD);
		final JSONObject filter = jsonObject.optJSONObject(FILTER);

		if (filter == null || isFilterMet(filter)) {
			removeEventKey(field);
		} else
			log.info(FILTER_NOT_MET);
	}


	private void renameArrayInArray(JSONObject jsonObject) // map
	{
		log.info("renameArrayInArray");
		final String field = jsonObject.getString(FIELD);
		final String oldField = jsonObject.getString(OLD_FIELD);
		final JSONObject filter = jsonObject.optJSONObject(FILTER);

		if (filter == null || isFilterMet(filter)) {

			final String[] fsplit = field.split(REGEX, field.length());
			final String[] oldfsplit = oldField.split(REGEX, oldField.length());

			final String oldValue = getEventObjectVal(oldfsplit[0]).toString();
			if (!oldValue.equals(OBJECT_NOT_FOUND)) {
				final String oldArrayName = oldfsplit[1].substring(1);
				final String newArrayName = fsplit[1].substring(1);
				final String value = oldValue.replaceAll(oldArrayName, newArrayName);

				log.info("oldValue ==" + oldValue);
				log.info("value ==" + value);
				JSONArray ja = new JSONArray(value);
				removeEventKey(oldfsplit[0]);
				setEventObjectVal(fsplit[0], ja);
			}
		} else
			log.info(FILTER_NOT_MET);
	}


	public void map(JSONObject jsonObject) {

		final String field = jsonObject.getString(FIELD);
		if (field.contains("[]")) {
			if (field.matches(".*\\[\\]\\..*\\[\\]"))
				renameArrayInArray(jsonObject);
			else
				mapToJArray(jsonObject);
		} else
			mapAttribute(jsonObject);
	}

	private String performOperation(String operation, String value) {
		log.info("performOperation");
		if ("convertMBtoKB".equals(operation)) {
			float kbValue = Float.parseFloat(value) * 1024;
			value = String.valueOf(kbValue);
		}
		return value;
	}


	public void mapAttribute(JSONObject jsonObject) {

		final String field = jsonObject.getString(FIELD);
		final String oldField = jsonObject.getString(OLD_FIELD);
		final JSONObject filter = jsonObject.optJSONObject(FILTER);
		final String operation = jsonObject.optString("operation");
		String value;
		if (filter == null || isFilterMet(filter)) {

			value = getEventObjectVal(oldField).toString();
			if (!value.equals(OBJECT_NOT_FOUND)) {
				if (operation != null && !operation.isEmpty())
					value = performOperation(operation, value);

				setEventObjectVal(field, value);

				removeEventKey(oldField);
			}
		} else
			log.info(FILTER_NOT_MET);
	}


	private void mapToJArray(JSONObject jsonObject) {
		log.info("mapToJArray");
		String field = jsonObject.getString(FIELD);
		String oldField = jsonObject.getString(OLD_FIELD);
		final JSONObject filter = jsonObject.optJSONObject(FILTER);
		final JSONObject attrMap = jsonObject.optJSONObject("attrMap");
		oldField = oldField.replaceAll(REGEX, "");
		field = field.replaceAll(REGEX, "");

		if (filter == null || isFilterMet(filter)) {

			String value = getEventObjectVal(oldField).toString();
			if (!value.equals(OBJECT_NOT_FOUND)) {
				log.info("old value ==" + value);
				// update old value based on attrMap
				if (attrMap != null) {
					// loop thru attrMap and update attribute name to new name
					for (String key : attrMap.keySet()) {
						value = value.replaceAll(key, attrMap.getString(key));
					}
				}

				log.info("new value ==" + value);
				char c = value.charAt(0);
				if (c != '[') {
					// oldfield is JsonObject
					JSONObject valueJO = new JSONObject(value);
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

							setEventObjectVal(field, ja);
						}
					} else // if new array
						setEventObjectVal(field + "[0]", new JSONObject(value), "JArray");
				} else // oldfield is jsonArray
					setEventObjectVal(field, new JSONArray(value));

				removeEventKey(oldField);
			}
		} else
			log.info(FILTER_NOT_MET);
	}

	/**
	 * example - { "functionName": "concatenateValue", "args":{ "filter":
	 * {"event.commonEventHeader.event":"heartbeat"},
	 * FIELD:"event.commonEventHeader.eventName", "concatenate":
	 * ["event.commonEventHeader.domain","event.commonEventHeader.eventType","event.commonEventHeader.alarmCondition"],
	 * "delimiter":"_" } }
	 **/
	public void concatenateValue(JSONObject jsonObject) {

		final String field = jsonObject.getString(FIELD);
		final String delimiter = jsonObject.getString("delimiter");
		final JSONArray values = jsonObject.getJSONArray("concatenate");
		final JSONObject filter = jsonObject.optJSONObject(FILTER);
		if (filter == null || isFilterMet(filter)) {
			StringBuilder value = new StringBuilder();
			for (int i = 0; i < values.length(); i++) {

				String tempVal = evaluate(values.getString(i));
				if (!tempVal.equals(OBJECT_NOT_FOUND)) {
					if (i == 0)
						value.append(tempVal);
					else
						value.append(delimiter).append(tempVal);
				}
			}

			setEventObjectVal(field, value.toString());
		} else
			log.info(FILTER_NOT_MET);
	}

	public void subtractValue(JSONObject jsonObject) {

		final String field = jsonObject.getString(FIELD);
		final JSONArray values = jsonObject.getJSONArray("subtract");
		final JSONObject filter = jsonObject.optJSONObject(FILTER);
		if (filter == null || isFilterMet(filter)) {
			float value = 0;
			for (int i = 0; i < values.length(); i++) {
				log.info(values.getString(i));
				String tempVal = evaluate(values.getString(i));
				log.info("tempVal==" + tempVal);
				if (!tempVal.equals(OBJECT_NOT_FOUND)) {
					if (i == 0)
						value = value + Float.valueOf(tempVal);
					else
						value = value - Float.valueOf(tempVal);
				}
			}
			log.info("value ==" + value);
			setEventObjectVal(field, value, "number");
		} else
			log.info(FILTER_NOT_MET);
	}


	private void removeEventKey(String field) {
		String[] keySet = field.split("\\.", field.length());
		JSONObject keySeries = event;
		for (int i = 0; i < (keySet.length - 1); i++) {

			keySeries = keySeries.getJSONObject(keySet[i]);
		}

		keySeries.remove(keySet[keySet.length - 1]);
	}


	private boolean checkFilter(JSONObject jo, String key, String logicKey) {
		String filterValue = jo.getString(key);
		if (filterValue.contains(":")) {
			String[] splitVal = filterValue.split(":");
			if ("matches".equals(splitVal[0])) {
				if ("not".equals(logicKey)) {
					if (getEventObjectVal(key).toString().matches(splitVal[1])) {
						log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + COMP_FALSE);
						return false;
					}
				} else {
					if (!(getEventObjectVal(key).toString().matches(splitVal[1]))) {
						log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + COMP_FALSE);
						return false;
					}
				}

			}
			if ("contains".equals(splitVal[0])) {
				if ("not".equals(logicKey)) {
					if (getEventObjectVal(key).toString().contains(splitVal[1])) {
						log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + COMP_FALSE);
						return false;
					}
				} else {
					if (!(getEventObjectVal(key).toString().contains(splitVal[1]))) {
						log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + COMP_FALSE);
						return false;
					}
				}

			}
		} else {
			if ("not".equals(logicKey)) {
				if (getEventObjectVal(key).toString().equals(filterValue)) {
					log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + COMP_FALSE);
					return false;
				}
			} else {
				if (!(getEventObjectVal(key).toString().equals(filterValue))) {
					log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + COMP_FALSE);
					return false;
				}
			}
		}
		return true;
	}


	public boolean isFilterMet(JSONObject jo) {
		for (String key : jo.keySet()) {
			if ("not".equals(key)) {
				JSONObject njo = jo.getJSONObject(key);
				for (String njoKey : njo.keySet()) {
					if (!checkFilter(njo, njoKey, key))
						return false;
				}
			} else {
				if (!checkFilter(jo, key, key))
					return false;
			}
		}
		return true;
	}

	/**
	 * returns a string or JSONObject or JSONArray
	 **/
	public Object getEventObjectVal(String keySeriesStr) {
		keySeriesStr = keySeriesStr.replaceAll("\\[", ".");
		keySeriesStr = keySeriesStr.replaceAll("\\]", ".");
		if (keySeriesStr.contains("..")) {
			keySeriesStr = keySeriesStr.replaceAll("\\.\\.", ".");
		}

		if (keySeriesStr.lastIndexOf(".") == keySeriesStr.length() - 1)
			keySeriesStr = keySeriesStr.substring(0, keySeriesStr.length() - 1);
		String[] keySet = keySeriesStr.split("\\.", keySeriesStr.length());
		Object keySeriesObj = event;
		for (String aKeySet : keySet) {
			if (keySeriesObj != null) {
				if (keySeriesObj instanceof String) {

					log.info("STRING==" + keySeriesObj);
				} else if (keySeriesObj instanceof JSONArray) {
					keySeriesObj = ((JSONArray) keySeriesObj).optJSONObject(Integer.parseInt(aKeySet));

				} else if (keySeriesObj instanceof JSONObject) {
					keySeriesObj = ((JSONObject) keySeriesObj).opt(aKeySet);

				} else {
					log.info("unknown object==" + keySeriesObj);
				}
			}
		}

		if (keySeriesObj == null)
			return OBJECT_NOT_FOUND;
		return keySeriesObj;
	}

	public void setEventObjectVal(String keySeriesStr, Object value) {
		setEventObjectVal(keySeriesStr, value, "string");
	}

	/**
	 * returns a string or JSONObject or JSONArray
	 **/
	public void setEventObjectVal(String keySeriesStr, Object value, String fieldType) {
		keySeriesStr = keySeriesStr.replaceAll("\\[", ".");
		keySeriesStr = keySeriesStr.replaceAll("\\]", ".");
		if (keySeriesStr.contains("..")) {
			keySeriesStr = keySeriesStr.replaceAll("\\.\\.", ".");
		}
		log.info("fieldType==" + fieldType);

		if (keySeriesStr.lastIndexOf(".") == keySeriesStr.length() - 1)
			keySeriesStr = keySeriesStr.substring(0, keySeriesStr.length() - 1);
		String[] keySet = keySeriesStr.split("\\.", keySeriesStr.length());
		Object keySeriesObj = event;
		for (int i = 0; i < (keySet.length - 1); i++) {

			if (keySeriesObj instanceof JSONArray) {

				if (((JSONArray) keySeriesObj).optJSONObject(Integer.parseInt(keySet[i])) == null) // if
																									// the
																									// object
																									// is
																									// not
																									// there
																									// then
																									// add
																									// it
				{
					log.info("Object is null, must add it");
					if (keySet[i + 1].matches("[0-9]*")) // if index then array
						((JSONArray) keySeriesObj).put(Integer.parseInt(keySet[i]), new JSONArray());
					else
						((JSONArray) keySeriesObj).put(Integer.parseInt(keySet[i]), new JSONObject());
				}
				keySeriesObj = ((JSONArray) keySeriesObj).optJSONObject(Integer.parseInt(keySet[i]));

			} else if (keySeriesObj instanceof JSONObject) {
				if (((JSONObject) keySeriesObj).opt(keySet[i]) == null) // if
																		// the
																		// object
																		// is
																		// not
																		// there
																		// then
																		// add
																		// it
				{
					if (keySet[i + 1].matches("[0-9]*")) // if index then array
						((JSONObject) keySeriesObj).put(keySet[i], new JSONArray());
					else
						((JSONObject) keySeriesObj).put(keySet[i], new JSONObject());
					log.info("Object is null, must add it");
				}
				keySeriesObj = ((JSONObject) keySeriesObj).opt(keySet[i]);
			} else {
				log.info("unknown object==" + keySeriesObj);
			}
		}
		if ("number".equals(fieldType)) {
			DecimalFormat df = new DecimalFormat("#.0");
			if (value instanceof String)
				((JSONObject) keySeriesObj).put(keySet[keySet.length - 1],
						Float.valueOf(df.format(Float.valueOf((String) value))));
			else
				((JSONObject) keySeriesObj).put(keySet[keySet.length - 1], Float.valueOf(df.format(value)));
		} else if ("integer".equals(fieldType) && value instanceof String)
			((JSONObject) keySeriesObj).put(keySet[keySet.length - 1], Integer.valueOf((String) value));
		else if ("JArray".equals(fieldType))
			((JSONArray) keySeriesObj).put(value);
		else
			((JSONObject) keySeriesObj).put(keySet[keySet.length - 1], value);

	}
}

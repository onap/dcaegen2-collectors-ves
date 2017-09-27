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
	
	    public ConfigProcessors(JSONObject eventJson) 
	    {
	    	event = eventJson;
	    }

	    /**
	     * 
	     */
	    public void getValue(JSONObject J)
	    {
	    	//log.info("addAttribute");
	    	final String field = J.getString(FIELD);
	    	//final String value = J.getString(VALUE);
	    	final JSONObject filter = J.optJSONObject(FILTER);
	    	if (filter == null || isFilterMet(filter))
	    	{
		    	//log.info("field ==" + field);
		    	//log.info("value ==" + value);
		    	getEventObjectVal(field);
	    	}
	    	else
	    		log.info("Filter not met");
	    }
	    
	    /**
	     * 
	     */
	    public void setValue(JSONObject J)
	    {
	    	//log.info("addAttribute");
	    	final String field = J.getString(FIELD);
	    	final String value = J.getString(VALUE);
	    	final JSONObject filter = J.optJSONObject(FILTER);
	    	if (filter == null || isFilterMet(filter))
	    	{
		    	//log.info("field ==" + field);
		    	//log.info("value ==" + value);
		    	setEventObjectVal(field, value);
	    	}
	    	else
	    		log.info("Filter not met");
	    }
	    
	    /**
	     * 
	     */
	    public String evaluate(String str)
	    {
	    	String value = str;
	    	if (str.startsWith("$"))
	    	{
	    		value = (String) getEventObjectVal(str.substring(1));
	    		
	    	}
	    	return value;
	    }
	    
	    /**
	     * { "functionName":"suppressEvent",
			  "args":{}
			}
	     */
	    public void suppressEvent(JSONObject J)
	    {
	    	//log.info("addAttribute");
	    	final JSONObject filter = J.optJSONObject(FILTER);
	    	
	    	if (filter == null || isFilterMet(filter))
	    	{
		    	//log.info("field ==" + field);
		    	//log.info("value ==" + value);
	    		setEventObjectVal("suppressEvent", "true");
	    	}
	    	else
	    		log.info("Filter not met");
	    }
	    
		/**
	     * 
	     */
	    public void addAttribute(JSONObject J)
	    {
	    	//log.info("addAttribute begin");
	    	final String field = J.getString(FIELD);
	    	final String value = evaluate(J.getString(VALUE));
	    	final JSONObject filter = J.optJSONObject(FILTER);
	    	final String fieldType = J.optString("fieldType", "string").toLowerCase();
	    	
	    	if (filter == null || isFilterMet(filter))
	    	{
		    	//log.info("field ==" + field);
		    	//log.info("value ==" + value);
	    		setEventObjectVal(field, value, fieldType);
	    	}
	    	else
	    		log.info("Filter not met");
	    	//log.info("addAttribute End");
	    }
	    
	    /**
	     * 
	     */
	    public void updateAttribute(JSONObject J)
	    {
	    	//log.info("updateAttribute");
	    	final String field = J.getString(FIELD);
	    	final String value = evaluate(J.getString(VALUE));
	    	final JSONObject filter = J.optJSONObject(FILTER);
	    	if (filter == null || isFilterMet(filter))
	    	{
		    	//log.info("field ==" + field);
		    	//log.info("value ==" + value);
		    	setEventObjectVal(field, value);
	    	}
	    	else
	    		log.info("Filter not met");   	
	    }
	    
	    /**
	     * 
	     */
	    public void removeAttribute(JSONObject J)
	    {
	    	//log.info("removeAttribute");
	    	final String field = J.getString(FIELD);
	    	final JSONObject filter = J.optJSONObject(FILTER);
	    	
	    	if (filter == null || isFilterMet(filter))
	    	{
		    	removeEventKey(field);
	    	}
	    	else
	    		log.info("Filter not met");
	    }
	    
	    /**
	     * 
	     */
	    public void renameArrayInArray(JSONObject J) //map
	    {
	    	log.info("renameArrayInArray");
	    	final String field = J.getString(FIELD);
	    	final String oldField = J.getString(OLD_FIELD);
	    	final JSONObject filter = J.optJSONObject(FILTER);
	    	//String value = "";
	    	if (filter == null || isFilterMet(filter))
	    	{
		    	//log.info("field ==" + field);
		    	final String[] fsplit = field.split(REGEX, field.length());
		    	final String[] oldfsplit = oldField.split(REGEX, oldField.length());
		    	/*for (int i=0; i< oldfsplit.length; i++ )
		    	{
		    		log.info( "renameArrayInArray " + i + " ==" + oldfsplit[i]);
		    	}*/
		    	
		    	final String oldValue = getEventObjectVal(oldfsplit[0]).toString();
		    	if (!oldValue.equals(OBJECT_NOT_FOUND)){
		    		final String oldArrayName = oldfsplit[1].substring(1);
		    		final String newArrayName = fsplit[1].substring(1);
		    		final String value = oldValue.replaceAll(oldArrayName, newArrayName);
		    		//log.info("oldArrayName ==" + oldArrayName);
		    		//log.info("newArrayName ==" + newArrayName);
		    		log.info("oldValue ==" + oldValue);
			    	log.info("value ==" + value);
			    	JSONArray ja = new JSONArray(value);
			    	removeEventKey(oldfsplit[0]);
			    	setEventObjectVal(fsplit[0], ja);
		    	}
	    	}
	    	else
	    		log.info("Filter not met");
	    }
	    
	    /**
	     * 
	     */
	   public void map(JSONObject J)
	    {
	    	//log.info("mapAttribute");
	    	final String field = J.getString(FIELD);
	    	if (field.contains("[]"))
	    	{
	    		if (field.matches(".*\\[\\]\\..*\\[\\]"))
	    			renameArrayInArray(J);
	    		else
	    			mapToJArray(J);
	    	}
	    	else
	    		mapAttribute(J);
	    } 
	    
	    /**
	     * 
	     */
	    public String performOperation(String operation, String value)
	    {
	    	log.info("performOperation");
	    	if (operation != null)
    		{
    			if (operation.equals("convertMBtoKB"))
    			{
    				float kbValue = Float.parseFloat(value) * 1024;
    				value = String.valueOf(kbValue);
    			}
    		}
	    	return value;
	    }
	    
	    /**
	     * 
	     */
	    //public void mapAttributeToArrayAttribute(JSONObject J)
	    public void mapAttribute(JSONObject J)
	    {
	    	//log.info("mapAttribute");
	    	final String field = J.getString(FIELD);
	    	final String oldField = J.getString(OLD_FIELD);
	    	final JSONObject filter = J.optJSONObject(FILTER);
	    	final String operation = J.optString("operation");
	    	String value = "";
	    	if (filter == null || isFilterMet(filter))
	    	{
		    	//log.info("field ==" + field);
		    	
		    	value = getEventObjectVal(oldField).toString();
		    	if (!value.equals(OBJECT_NOT_FOUND))
		    	{
		    		if (operation != null && !operation.equals(""))
		    			value = performOperation(operation, value);
		    		//log.info("value ==" + value);
		    		setEventObjectVal(field, value);
		    	
		    		removeEventKey(oldField);
		    	}
	    	}
	    	else
	    		log.info("Filter not met");
	    }
	    
	    /**
	     * 
	     */
	    public void mapToJArray(JSONObject J)
	    {
	    	log.info("mapToJArray");
	    	String field = J.getString(FIELD);
	    	String oldField = J.getString(OLD_FIELD);
	    	final JSONObject filter = J.optJSONObject(FILTER);
	    	final JSONObject attrMap = J.optJSONObject("attrMap");
	    	oldField = oldField.replaceAll(REGEX, "");
	    	field = field.replaceAll(REGEX, "");
	  
	    	//log.info("oldField ==" + field);
	    	if (filter == null || isFilterMet(filter))
	    	{
		    	//log.info("oldField ==" + field);
		    	String value = getEventObjectVal(oldField).toString();
		    	if (!value.equals(OBJECT_NOT_FOUND))
		    	{
		    		log.info("old value ==" + value.toString());
		    		//update old value based on attrMap
		    		if (attrMap != null)
		    		{
		    			//loop thru attrMap and update attribute name to new name 
		    			for (String key : attrMap.keySet())
		    			{
			    		//log.info("attr key==" + key + " value==" + attrMap.getString(key));
				    	value = value.replaceAll(key, attrMap.getString(key));
		    			}
		    		}
			    
		    		log.info("new value ==" + value);
		    		char c = value.charAt(0);
		    		if (c != '[')
		    		{
		    			//oldfield is JsonObject
		    			JSONObject valueJO = new JSONObject(value);
		    			// if the array already exists
			    	
		    			String existingValue = getEventObjectVal(field).toString();
		    			if (!existingValue.equals(OBJECT_NOT_FOUND))
		    			{
		    				JSONArray ja = new JSONArray(existingValue);
		    				JSONObject jo = ja.optJSONObject(0);
		    				if (jo != null)
		    				{
		    					for (String key : valueJO.keySet())
		    					{
		    						jo.put(key, valueJO.get(key));
				    			
		    					}
		    					ja.put(0, jo);
		    					//log.info("jarray== " + ja.toString());
		    					setEventObjectVal(field,ja);
		    				}
		    			}
		    			else //if new array
		    			setEventObjectVal(field + "[0]", new JSONObject(value), "JArray");
		    		}
		    		else //oldfield is jsonArray
		    			setEventObjectVal(field, new JSONArray(value));	
		    	
		    		removeEventKey(oldField);
		    	}
	    	}
	    	else
	    		log.info("Filter not met");
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
	    public void concatenateValue(JSONObject J)
	    {
	    	//log.info("concatenateValue");
	    	final String field = J.getString(FIELD);
	    	final String delimiter = J.getString("delimiter");
	    	final JSONArray values = J.getJSONArray("concatenate");
	    	final JSONObject filter = J.optJSONObject(FILTER);
	    	if (filter == null || isFilterMet(filter))
	    	{
		    	String value = "";
		    	for (int i=0; i < values.length(); i++)
		    	{
		    		//log.info(values.getString(i));
		    		String tempVal = evaluate(values.getString(i));
		    		if (!tempVal.equals(OBJECT_NOT_FOUND))
		    		{
		    			if (i ==0)
		    			value = value + tempVal;
		    			else
		    			value = value + delimiter + tempVal;
		    		}
		    	}
		    	//log.info("value ==" + value);
		    	setEventObjectVal(field, value);
	    	}
	    	else
	    		log.info("Filter not met");
	    }
	    
	    public void subtractValue(JSONObject J)
	    {
	    	//log.info("concatenateValue");
	    	final String field = J.getString(FIELD);
	    	final JSONArray values = J.getJSONArray("subtract");
	    	final JSONObject filter = J.optJSONObject(FILTER);
	    	if (filter == null || isFilterMet(filter))
	    	{
		    	float value = 0;
		    	for (int i=0; i < values.length(); i++)
		    	{
		    		log.info(values.getString(i));
		    		String tempVal = evaluate(values.getString(i));
		    		log.info("tempVal==" + tempVal);
		    		if (!tempVal.equals(OBJECT_NOT_FOUND))
		    		{
		    			if (i ==0)
		    			value = value + Float.valueOf(tempVal);
		    			else
		    			value = value - Float.valueOf(tempVal);
		    		}
		    	}
		    	log.info("value ==" + value );
		    	setEventObjectVal(field, value, "number");
	    	}
	    	else
	    		log.info("Filter not met");
	    }
	    
	    /**
	     * 
	     */
	    private void removeEventKey(String field)
	    {
	    	String[] keySet = field.split("\\.",field.length());
	    	JSONObject keySeries = event;
	    	for (int i=0; i<(keySet.length -1); i++ )
	    	{
	    		//log.info( i + " ==" + keySet[i]);
	    		keySeries = keySeries.getJSONObject(keySet[i]);
	    	}
	    	//log.info(keySet[keySet.length -1]);
	    	
	    	keySeries.remove(keySet[keySet.length -1]);
	    	
	    }
	    
	    /**
	     * 
	     */
	    private boolean checkFilter(JSONObject jo, String key, String logicKey)
	    {
	    	String filterValue = jo.getString(key);
	    	boolean retVal = true;
	    	
			if(filterValue.contains(":"))
			{
				String[] splitVal = filterValue.split(":");
				//log.info(splitVal[0] + " " + splitVal[1]);
				if (splitVal[0].equals("matches"))
				{
					if (logicKey.equals("not"))
					{
						//log.info("not");
						//log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + "split1==" + splitVal[1]);
						if (getEventObjectVal(key).toString().matches(splitVal[1]))
						{
							log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + "==false");
							return false;
						}
					}
					else
					{
						if (!(getEventObjectVal(key).toString().matches(splitVal[1])))
						{
							log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + "==false");
							return false;
						}
					}
						
				}
				if (splitVal[0].equals("contains"))
				{
					if (logicKey.equals("not"))
					{
						//log.info("not");
						//log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + "split1==" + splitVal[1]);
						if (getEventObjectVal(key).toString().contains(splitVal[1]))
						{
							log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + "==false");
							return false;
						}
					}
					else
					{
						if (!(getEventObjectVal(key).toString().contains(splitVal[1])))
						{
							log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + "==false");
							return false;
						}
					}
						
				}
			}
			else 
			{
				if (logicKey.equals("not"))
				{
					if(getEventObjectVal(key).toString().equals(filterValue))
					{
						log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + "==false");
						return false;
					}
				}
				else
				{
					if(!(getEventObjectVal(key).toString().equals(filterValue)))
	    			{
	    				log.info(filterValue + "==" + key + "==" + getEventObjectVal(key) + "==false");
	    				return false;
	    			}
				}
			}
			return retVal;
	    }
	    /**
	     * 
	     */
	    public boolean isFilterMet(JSONObject jo)
	    {
	    	boolean retval = true;
	    	//log.info("Filter==" + jo.toString());
	    	for (String key : jo.keySet())
	    	{		
	    		if (key.equals("not"))
	    		{
	    			JSONObject njo = jo.getJSONObject(key);
	    			for (String njoKey : njo.keySet())
	    			{
	    				//log.info(njoKey);
	    				retval = checkFilter(njo, njoKey, key);
	    				if (retval == false)
	    					return retval;
	    			}
	    		}
	    		else 
	    		{
	    			//log.info(key);
	    			//final String filterKey = key;
	    			retval = checkFilter(jo, key, key);
	    			if (retval == false)
    					return retval;
	    		}
	    	}
	    	return true;
	    }
	    
	    /**
		* returns a string or JSONObject or JSONArray
		**/
	    public Object getEventObjectVal(String keySeriesStr)
	    {
	    	keySeriesStr = keySeriesStr.replaceAll("\\[", ".");
	    	keySeriesStr = keySeriesStr.replaceAll("\\]", ".");
	    	if (keySeriesStr.contains(".."))
	    	{
	    		keySeriesStr = keySeriesStr.replaceAll("\\.\\.", ".");
	    	}
	    	//log.info(Integer.toString(keySeriesStr.lastIndexOf(".")));
	    	//log.info(Integer.toString(keySeriesStr.length() -1));
	    	if (keySeriesStr.lastIndexOf(".") == keySeriesStr.length() -1 )
	    		keySeriesStr = keySeriesStr.substring(0,keySeriesStr.length()-1 );
	    	String[] keySet = keySeriesStr.split("\\.", keySeriesStr.length());
	    	Object keySeriesObj = event;
	    	for (int i=0; i<(keySet.length); i++ )
	    	{
	    		//log.info( "getEventObject " + i + " ==" + keySet[i]);
	    		if (keySeriesObj != null)
	    		{
	    			if (keySeriesObj instanceof String)
	    			{
	    				//keySeriesObj =  keySeriesObj.get(keySet[i]);
	    				log.info("STRING==" + keySeriesObj);
	    			}
	    			else if (keySeriesObj instanceof JSONArray) {
	    				keySeriesObj =  ((JSONArray) keySeriesObj).optJSONObject(Integer.parseInt(keySet[i]));
	    				//log.info("ARRAY==" + keySeriesObj);
	    			}
	    			else if (keySeriesObj instanceof JSONObject) {
	    				keySeriesObj =  ( (JSONObject) keySeriesObj).opt(keySet[i]);
	    				//log.info("JSONObject==" + keySeriesObj);
	    			}
	    			else
	    			{
	    				log.info("unknown object==" + keySeriesObj);
	    			}
	    		}
	    	}
	    	
	    	if (keySeriesObj == null)
	    		return OBJECT_NOT_FOUND;
	    	return keySeriesObj;
	    }
	    
	    public void setEventObjectVal(String keySeriesStr, Object value)
	    {
	    	setEventObjectVal(keySeriesStr, value, "string");
	    }
	    
	    /**
		* returns a string or JSONObject or JSONArray
		**/
	    public void setEventObjectVal(String keySeriesStr, Object value, String fieldType)
	    {
	    	keySeriesStr = keySeriesStr.replaceAll("\\[", ".");
	    	keySeriesStr = keySeriesStr.replaceAll("\\]", ".");
	    	if (keySeriesStr.contains(".."))
	    	{
	    		keySeriesStr = keySeriesStr.replaceAll("\\.\\.", ".");
	    	}
	    	log.info("fieldType==" + fieldType);
	    	//log.info(Integer.toString(keySeriesStr.lastIndexOf(".")));
	    	//log.info(Integer.toString(keySeriesStr.length() -1));
	    	if (keySeriesStr.lastIndexOf(".") == keySeriesStr.length() -1 )
	    		keySeriesStr = keySeriesStr.substring(0,keySeriesStr.length()-1 );
	    	String[] keySet = keySeriesStr.split("\\.", keySeriesStr.length());
	    	Object keySeriesObj = event;
	    	for (int i=0; i<(keySet.length -1); i++ )
	    	{
	    		//log.info( "setEventObject " + i + " ==" + keySet[i]);	    	
	    		if (keySeriesObj instanceof JSONArray) {
	    			//keySeriesObj =  ((JSONArray) keySeriesObj).optJSONObject(Integer.parseInt(keySet[i]));
	    			if (((JSONArray) keySeriesObj).optJSONObject(Integer.parseInt(keySet[i])) == null) //if the object is not there then add it
	    			{
	    				log.info("Object is null, must add it");
	    				if (keySet[i+1].matches("[0-9]*")) // if index then array
	    					((JSONArray) keySeriesObj).put(Integer.parseInt(keySet[i]), new JSONArray());
	    				else
	    				((JSONArray) keySeriesObj).put(Integer.parseInt(keySet[i]), new JSONObject());
	    			}
	    			keySeriesObj =  ((JSONArray) keySeriesObj).optJSONObject(Integer.parseInt(keySet[i]));	
	    			//log.info("ARRAY==" + keySeriesObj);
	    		}
	    		else if (keySeriesObj instanceof JSONObject) {
	    			if (( (JSONObject) keySeriesObj).opt(keySet[i]) == null) //if the object is not there then add it
	    			{
	    				if (keySet[i+1].matches("[0-9]*")) // if index then array
	    					((JSONObject) keySeriesObj).put(keySet[i], new JSONArray());
	    				else
	    					((JSONObject) keySeriesObj).put(keySet[i], new JSONObject());
	    				log.info("Object is null, must add it");
	    			}
	    			keySeriesObj =  ( (JSONObject) keySeriesObj).opt(keySet[i]);
	    			//log.info("JSONObject==" + keySeriesObj);
	    		}
	    		else
	    		{
	    			log.info("unknown object==" + keySeriesObj);
	    		}
	    	}
	    	if (fieldType.equals("number") )
	    	{
	    		DecimalFormat df = new DecimalFormat("#.0");
	    		if (value instanceof String)
	    			((JSONObject)keySeriesObj).put(keySet[keySet.length -1], Float.valueOf(df.format(Float.valueOf((String) value))));
	    		else
	    			((JSONObject)keySeriesObj).put(keySet[keySet.length -1], Float.valueOf(df.format(value)));
	    	}
	    	else if (fieldType.equals("integer") && value instanceof String)
	    		((JSONObject)keySeriesObj).put(keySet[keySet.length -1], Integer.valueOf((String) value));
	    	else if (fieldType.equals("JArray"))
	    		((JSONArray)keySeriesObj).put( value);
	    	else
	    		((JSONObject)keySeriesObj).put(keySet[keySet.length -1], value);
	    	
	    }
	    private JSONObject event = new JSONObject();
}


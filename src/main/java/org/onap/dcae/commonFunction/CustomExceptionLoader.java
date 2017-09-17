/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;


public class CustomExceptionLoader {

    public static HashMap<String, JsonArray> map;
    private static final Logger log = LoggerFactory.getLogger(CustomExceptionLoader.class);

    //For standalone test
    //LoadMap Invoked from servletSetup
    /*
    public static void main(String[] args) {

		System.out.println("CustomExceptionLoader.main --> Arguments -- ExceptionConfig file: " + args[0] + "StatusCode:" + args[1]+ " Error Msg:" + args[2]);
		CommonStartup.exceptionConfig = args[0];
		
		//Read the Custom exception JSON file into map
	    LoadMap();
	    System.out.println("CustomExceptionLoader.main --> Map info post LoadMap:" + map);
	   
	     String[] str= LookupMap(args[1],args[2]);
			if (! (str==null)) {
			System.out.println("CustomExceptionLoader.main --> Return from lookup function" + str[0] + "value:" + str[1]);	
		}
	    
	}
	*/

    public static void LoadMap() {

        map = new HashMap<String, JsonArray>();
        FileReader fr = null;
        try {
            JsonElement root = null;
            fr = new FileReader(CommonStartup.exceptionConfig);
            root = new JsonParser().parse(fr);
            JsonObject jsonObject = root.getAsJsonObject().get("code").getAsJsonObject();

            for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                map.put(entry.getKey(), (JsonArray) entry.getValue());
            }

            log.debug("CustomExceptionLoader.LoadMap --> Map loaded - " + map);
            fr.close();
        } catch (JsonIOException e) {
            log.error("json IO exception occured " + e.getLocalizedMessage(), e);
        } catch (JsonSyntaxException e) {
            log.error("JsonSyntaxException occured " + e.getLocalizedMessage(), e);
        } catch (FileNotFoundException e) {
            log.error("FileNotFoundException occured " + e.getLocalizedMessage(), e);
        } catch (Exception e) {
            log.error("exception occured " + e.getLocalizedMessage(), e);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    log.error("Error closing file reader stream : " + e.getLocalizedMessage(), e);
                }
            }
        }
    }

    public static String[] LookupMap(String error, String errorMsg) {

        String[] retarray = null;

        log.debug("CustomExceptionLoader.LookupMap -->" + " HTTP StatusCode:" + error + " Msg:"
            + errorMsg);
        try {

            JsonArray jarray = map.get(error);
            for (int i = 0; i < jarray.size(); i++) {

                JsonElement val = jarray.get(i).getAsJsonObject().get("Reason");
                JsonArray ec = (JsonArray) jarray.get(i).getAsJsonObject().get("ErrorCode");
                log.trace("CustomExceptionLoader.LookupMap Parameter -> Error msg : " + errorMsg
                    + " Reason text being matched:" + val);
                if (errorMsg.contains(val.toString().replace("\"", ""))) {
                    log.trace(
                        "CustomExceptionLoader.LookupMap Successful! Exception matched to error message StatusCode:"
                            + ec.get(0) + "ErrorMessage:" + ec.get(1));
                    retarray = new String[2];
                    retarray[0] = ec.get(0).toString();
                    retarray[1] = ec.get(1).toString();
                    return retarray;
                }
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return retarray;
    }

}

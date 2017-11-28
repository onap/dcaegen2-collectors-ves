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

package org.onap.dcae.controller;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class FetchDynamicConfig {

    private static final Logger log = LoggerFactory.getLogger(FetchDynamicConfig.class);

    static String configFile = "/opt/app/KV-Configuration.json";
    static String url;
    static String retString;

    public FetchDynamicConfig() {
    }

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            log.info("%s=%s%n", entry.getKey(), entry.getValue());
        }

        if (env.containsKey("CONSUL_HOST") && env.containsKey("CONFIG_BINDING_SERVICE")
            && env.containsKey("HOSTNAME")) {
            log.info(">>>Dynamic configuration to be fetched from ConfigBindingService");
            url = env.get("CONSUL_HOST") + ":8500/v1/catalog/service/" + env
                .get("CONFIG_BINDING_SERVICE");

            retString = executecurl(url);
            // consul return as array
            JSONTokener temp = new JSONTokener(retString);
            JSONObject cbsjobj = (JSONObject) new JSONArray(temp).get(0);

            String urlPart1 = null;
            if (cbsjobj.has("ServiceAddress") && cbsjobj.has("ServicePort")) {
                urlPart1 =
                    cbsjobj.getString("ServiceAddress") + ":" + cbsjobj.getInt("ServicePort");
            }

            log.info("CONFIG_BINDING_SERVICE DNS RESOLVED:" + urlPart1);
            url = urlPart1 + "/service_component/" + env.get("HOSTNAME");
            retString = executecurl(url);

            JSONObject jsonObject = new JSONObject(new JSONTokener(retString));
            try (FileWriter file = new FileWriter(configFile)) {
                file.write(jsonObject.toString());

                log.info(
                    "Successfully Copied JSON Object to file /opt/app/KV-Configuration.json");
            } catch (IOException e) {
                log.error(
                    "Error in writing configuration into file /opt/app/KV-Configuration.json "
                        + jsonObject, e);
		e.printStackTrace();
            }
        } else {
            log.info(">>>Static configuration to be used");
        }

    }

    public static String executecurl(String url) {

        String[] command = {"curl", "-v", url};
        ProcessBuilder process = new ProcessBuilder(command);
        Process p;
        String result = null;
        try {
            p = process.start();
            InputStreamReader ipr = new InputStreamReader(p.getInputStream());
            BufferedReader reader = new BufferedReader(ipr);
            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            result = builder.toString();
            log.info(result);

            reader.close();
            ipr.close();
        } catch (IOException e) {
            log.error("error", e);
            e.printStackTrace();
        }
        return result;

    }

}

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

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;


public class LoadDynamicConfig {

    private static final Logger log = LoggerFactory.getLogger(LoadDynamicConfig.class);

    public String propFile = "collector.properties";
    public String configFile = "/opt/app/KV-Configuration.json";
    static String url;
    static String retString;

    public LoadDynamicConfig() {

    }

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();
        /*for (String envName : env.keySet()) {
            System.out.format("%s=%s%n", envName, env.get(envName));
		}*/

        //Check again to ensure new controller deployment related config
        if (env.containsKey("CONSUL_HOST") &&
            env.containsKey("CONFIG_BINDING_SERVICE") && env.containsKey("HOSTNAME")) {

            try {

		LoadDynamicConfig lc = new LoadDynamicConfig();
                String jsonData = readFile(lc.configFile);
                JSONObject jsonObject = new JSONObject(jsonData);

                PropertiesConfiguration conf;
                conf = new PropertiesConfiguration(lc.propFile);
                conf.setEncoding(null);

                // update properties based on consul dynamic configuration
                Iterator<?> keys = jsonObject.keys();

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    // check if any configuration is related to dmaap
                    // and write into dmaapconfig.json
                    if (key.startsWith("streams_publishes")) {
                        //VESCollector only have publish streams
                        try (FileWriter file = new FileWriter("./etc/DmaapConfig.json")) {
                            file.write(jsonObject.get(key).toString());
                            log.info("Successfully written JSON Object to DmaapConfig.json");
                            file.close();
                        } catch (IOException e) {
                            log.info(
                                "Error in writing dmaap configuration into DmaapConfig.json",
                                e);
                        }
                    } else {
                        conf.setProperty(key, jsonObject.get(key).toString());
                    }

                }
                conf.save();

            } catch (ConfigurationException e) {
                log.error(e.getLocalizedMessage(), e);

            }

        } else {
            log.info(">>>Static configuration to be used");
        }

    }

    public static String readFile(String filename) {
        String result = "";
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            result = sb.toString();
            br.close();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }
        return result;
    }


}

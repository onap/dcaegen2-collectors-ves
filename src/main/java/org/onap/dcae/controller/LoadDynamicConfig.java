package org.onap.dcae.controller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import org.json.JSONObject;

 

public class LoadDynamicConfig {

	static String propFile = "collector.properties";
	static String configFile = "/opt/app/KV-Configuration.json";
	static String url = null;
	static String ret_string = null;

	public LoadDynamicConfig() {

	}

	public static void main(String[] args) {
		Map<String, String> env = System.getenv();
		/*for (String envName : env.keySet()) {
			System.out.format("%s=%s%n", envName, env.get(envName));
		}*/

		//Check again to ensure new controller deployment related config
		if (env.containsKey("CONSUL_HOST") &&
		 env.containsKey("CONFIG_BINDING_SERVICE")  && env.containsKey("HOSTNAME"))
		{
		
			
			try {

			 	String jsonData = readFile(configFile);
			 	JSONObject jsonObject = new JSONObject(jsonData);
			 	
				PropertiesConfiguration conf;
				conf = new PropertiesConfiguration(propFile);
				conf.setEncoding(null);

				
				if (jsonObject != null) {
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
								System.out.println("Successfully written JSON Object to DmaapConfig.json");
							} catch (IOException e) {
								System.out.println("Error in writing dmaap configuration into DmaapConfig.json");
								e.printStackTrace();
							}
						} else {
							conf.setProperty(key, jsonObject.get(key).toString());
						}

					}
					conf.save();


				}
			} catch (ConfigurationException e) {
				e.getMessage();
				e.printStackTrace();

			}
			
		}
		else
		{
			System.out.println(">>>Static configuration to be used");
		}

	}

	public static String executecurl(String url) {

		String[] command = { "curl", "-v", url };
		ProcessBuilder process = new ProcessBuilder(command);
		Process p;
		String result = null;
		try {
			p = process.start();

			InputStreamReader ipr= new InputStreamReader(p.getInputStream());
			BufferedReader reader = new BufferedReader(ipr);
			StringBuilder builder = new StringBuilder();
			String line = null;
			
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			result = builder.toString();
			System.out.println(result);
			
		} catch (IOException e) {
			System.out.print("error");
			e.printStackTrace();
		}
		return result;

	}

	public static String readFile(String filename) {
	    String result = "";
	    try {
	        BufferedReader br = new BufferedReader(new FileReader(filename));
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();
	        while (line != null) {
	            sb.append(line);
	            line = br.readLine();
	        }
	        result = sb.toString();
	        br.close();
	    } catch(Exception e) {
	        e.printStackTrace();
	    }
	    return result;
	}


}

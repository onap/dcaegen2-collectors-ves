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



import java.io.IOException;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onap.dcae.restapi.RestfulCollectorServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.att.nsa.apiServer.ApiServer;
import com.att.nsa.apiServer.ApiServerConnector;
import com.att.nsa.apiServer.endpoints.NsaBaseEndpoint;
import com.att.nsa.cmdLine.NsaCommandLineUtil;
import com.att.nsa.drumlin.service.framework.DrumlinServlet;
import com.att.nsa.drumlin.till.nv.rrNvReadable;
import com.att.nsa.drumlin.till.nv.impl.nvPropertiesFile;
import com.att.nsa.drumlin.till.nv.impl.nvReadableStack;
import com.att.nsa.drumlin.till.nv.impl.nvReadableTable;
import com.att.nsa.drumlin.till.nv.rrNvReadable.invalidSettingValue;
import com.att.nsa.drumlin.till.nv.rrNvReadable.loadException;
import com.att.nsa.drumlin.till.nv.rrNvReadable.missingReqdSetting;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.github.fge.jsonschema.util.JsonLoader;


public class CommonStartup extends NsaBaseEndpoint implements Runnable
{
	public static final String KCONFIG = "c";

	public static final String KSETTING_PORT = "collector.service.port";
	public static final int KDEFAULT_PORT = 8080;
	
	public static final String KSETTING_SECUREPORT = "collector.service.secure.port";
	public static final int KDEFAULT_SECUREPORT = -1; 
	
	public static final String KSETTING_KEYSTOREPASSFILE = "collector.keystore.passwordfile";
	public static final String KDEFAULT_KEYSTOREPASSFILE = "../etc/passwordfile";	
	public static final String KSETTING_KEYSTOREFILE = "collector.keystore.file.location";
	public static final String KDEFAULT_KEYSTOREFILE = "../etc/keystore";
	public static final String KSETTING_KEYALIAS = "collector.keystore.alias";
	public static final String KDEFAULT_KEYALIAS = "tomcat";

	public static final String KSETTING_DMAAPCONFIGS = "collector.dmaapfile";
	protected static final String[] KDEFAULT_DMAAPCONFIGS = new String[] { "/etc/DmaapConfig.json" };

	public static final String KSETTING_MAXQUEUEDEVENTS = "collector.inputQueue.maxPending";
	public static final int KDEFAULT_MAXQUEUEDEVENTS = 1024*4;
	
	public static final String KSETTING_SCHEMAVALIDATOR = "collector.schema.checkflag";
	public static final int KDEFAULT_SCHEMAVALIDATOR = -1; 
	
	public static final String KSETTING_SCHEMAFILE = "collector.schema.file";
	public static final String KDEFAULT_SCHEMAFILE = "{\"v5\":\"./etc/CommonEventFormat_28.3.json\"}";
	public static final String KSETTING_EXCEPTIONCONFIG = "exceptionConfig";
	
	public static final String KSETTING_DMAAPSTREAMID = "collector.dmaap.streamid";
	
	public static final String KSETTING_AUTHFLAG = "header.authflag";
	public static final int KDEFAULT_AUTHFLAG = 0;
	
	public static final String KSETTING_AUTHID = "header.authid";
	public static final String KSETTING_AUTHPWD = "header.authpwd";
	public static final String KSETTING_AUTHSTORE = "header.authstore";
	public static final String KSETTING_AUTHLIST = "header.authlist";
	
	public static final String KSETTING_EVENTTRANSFORMFLAG = "event.transform.flag";
	public static final int KDEFAULT_EVENTTRANSFORMFLAG = 1;
	
	
	public static final Logger inlog = LoggerFactory.getLogger ("org.onap.dcae.commonFunction.input" );
	public static final Logger oplog = LoggerFactory.getLogger ("org.onap.dcae.commonFunction.output");
	public static final Logger eplog = LoggerFactory.getLogger ("org.onap.dcae.commonFunction.error");
	public static final Logger metriclog = LoggerFactory.getLogger ("com.att.ecomp.metrics" );
	
	public static int schemaValidatorFlag = -1;
	public static int authflag = 1;
	public static int eventTransformFlag = 1;
	public static String schemaFile = null;
	public static JSONObject schemaFileJson = null;
	public static String exceptionConfig = null;
	public static String cambriaConfigFile = null;
	private boolean listnerstatus = false;
	static String streamid = null;
	
	
	private CommonStartup(rrNvReadable settings) throws loadException, missingReqdSetting, IOException, rrNvReadable.missingReqdSetting, rrNvReadable.invalidSettingValue, ServletException, InterruptedException
	{
		final List<ApiServerConnector> connectors = new LinkedList<> ();

		if (settings.getInt ( KSETTING_PORT, KDEFAULT_PORT ) > 0)
		{
			// http service
				connectors.add (
					new ApiServerConnector.Builder ( settings.getInt ( KSETTING_PORT, KDEFAULT_PORT ) )
					.secure ( false )
					.build ()
				);
		}

		// optional https service		
		final int securePort = settings.getInt(KSETTING_SECUREPORT, KDEFAULT_SECUREPORT);
		final String keystoreFile = settings.getString(KSETTING_KEYSTOREFILE, KDEFAULT_KEYSTOREFILE);
		final String keystorePasswordFile = settings.getString(KSETTING_KEYSTOREPASSFILE, KDEFAULT_KEYSTOREPASSFILE);
		final String keyAlias = settings.getString (KSETTING_KEYALIAS, KDEFAULT_KEYALIAS);
		
		
		if (securePort > 0)
		{
			final String KSETTING_KeystorePass = readFile(keystorePasswordFile, Charset.defaultCharset());
				connectors.add(new ApiServerConnector.Builder(securePort)
					.secure(true)
					.keystorePassword(KSETTING_KeystorePass)
					.keystoreFile(keystoreFile)
					.keyAlias(keyAlias)
					.build());

		}

		//Reading other config properties
		
		schemaValidatorFlag = settings.getInt(KSETTING_SCHEMAVALIDATOR, KDEFAULT_SCHEMAVALIDATOR );
		if (schema_Validatorflag > 0){
			schemaFile =  settings.getString(KSETTING_SCHEMAFILE,KDEFAULT_SCHEMAFILE);
			//System.out.println("SchemaFile:" + schemaFile);
			schemaFileJson = new JSONObject(schemaFile);
			
		}
		exceptionConfig = settings.getString(KSETTING_EXCEPTIONCONFIG, null);
		authflag = settings.getInt(CommonStartup.KSETTING_AUTHFLAG, CommonStartup.KDEFAULT_AUTHFLAG );
		String [] currentconffile = settings.getStrings (CommonStartup.KSETTING_DMAAPCONFIGS,	CommonStartup.KDEFAULT_DMAAPCONFIGS ) ;
		cambriaConfigFile= currentconffile[0] ;
		streamid = settings.getString(KSETTING_DMAAPSTREAMID,null);
		eventTransformFlag = settings.getInt(KSETTING_EVENTTRANSFORMFLAG, KDEFAULT_EVENTTRANSFORMFLAG);

		fTomcatServer = new ApiServer.Builder(connectors, new RestfulCollectorServlet(settings))
			.encodeSlashes(true)
			.name("collector")
			.build();
		
		
		//Load override exception map 
		CustomExceptionLoader.LoadMap();
		setListnerstatus(true);
	}

	public static void main ( String[] args )
	{
		ExecutorService executor = null;
		try
		{
			// process command line arguments
			final Map<String, String> argMap = NsaCommandLineUtil.processCmdLine ( args, true );
			final String config = NsaCommandLineUtil.getSetting ( argMap, KCONFIG, "collector.properties" );
			final URL settingStream = DrumlinServlet.findStream ( config, CommonStartup.class );

			final nvReadableStack settings = new nvReadableStack ();
			settings.push ( new nvPropertiesFile ( settingStream ) );
			settings.push ( new nvReadableTable ( argMap ) );
			
			fProcessingInputQueue = new LinkedBlockingQueue<> (CommonStartup.KDEFAULT_MAXQUEUEDEVENTS);
			
			VESLogger.setUpEcompLogging();
			
			CommonStartup cs= new CommonStartup ( settings );
			
			Thread csmain = new Thread(cs);
			csmain.start();
			
			
			EventProcessor ep = new EventProcessor ();
			//Thread epThread=new Thread(ep);
			//epThread.start();
			executor = Executors.newFixedThreadPool(20);
			executor.execute(ep);
		    
		}
		catch ( loadException | missingReqdSetting  | IOException | invalidSettingValue | ServletException | InterruptedException e )
		{
			CommonStartup.eplog.error("FATAL_STARTUP_ERROR" + e.getMessage() );
			throw new RuntimeException ( e );
		}
		finally
		{
			// This will make the executor accept no new threads
            // and finish all existing threads in the queue
			if (executor != null){
				executor.shutdown();	
			}
           
		}
	}

	public void run() {
		try {
			fTomcatServer.start ();
		} catch (LifecycleException | IOException e) {

                    LOG.error("lifecycle or IO: ",  e);
		}
		fTomcatServer.await ();
	}

    public boolean isListnerstatus() {
		return listnerstatus;
	}

	public void setListnerstatus(boolean listnerstatus) {
		this.listnerstatus = listnerstatus;
	}
		public static Queue<JSONObject> getProcessingInputQueue ()
	{
		return fProcessingInputQueue;
	}
	
	public static class QueueFullException extends Exception
	{
		private static final long serialVersionUID = 1L;
	}
	
	
	public static void handleEvents ( JSONArray a ) throws QueueFullException, JSONException, IOException
	{
		final Queue<JSONObject> queue = getProcessingInputQueue ();
		try
		{
			
			CommonStartup.metriclog.info("EVENT_PUBLISH_START" );
			for (int i = 0; i < a.length(); i++) {
					if ( !queue.offer ( a.getJSONObject(i) ) )	{
						throw new QueueFullException ();
					}
					
			} 
			LOG.debug("CommonStartup.handleEvents:EVENTS has been published successfully!");
			CommonStartup.metriclog.info("EVENT_PUBLISH_END");
			//ecomplogger.debug(secloggerMessageEnum.SEC_COLLECT_AND_PULIBISH_SUCCESS);

		}
		catch ( JSONException e ){
			throw e;
			
		}
	}

		
	static String readFile(String path, Charset encoding) 
			  throws IOException 
			{
			    byte[] encoded = Files.readAllBytes(Paths.get(path));
			    String pwd = new String(encoded);
			    return pwd.substring(0,pwd.length()-1);
			}

	
	public static String schemavalidate( String jsonData, String jsonSchema) {
	    ProcessingReport report = null;
	    String result = "false";
	    
	    try {
	        //System.out.println("Applying schema: @<@<"+jsonSchema+">@>@ to data: #<#<"+jsonData+">#>#");
	    	LOG.trace("Schema validation for event:" + jsonData);
	        JsonNode schemaNode = JsonLoader.fromString(jsonSchema);
	        JsonNode data = JsonLoader.fromString(jsonData);         
	        JsonSchemaFactory factory = JsonSchemaFactory.byDefault(); 
	        JsonSchema schema = factory.getJsonSchema(schemaNode);
	        report = schema.validate(data);
	    } catch (JsonParseException e) {
	    	LOG.error("schemavalidate:JsonParseException for event:" + jsonData );
	        return e.getMessage().toString();
	    } catch (ProcessingException e) {  
	    	LOG.error("schemavalidate:Processing exception for event:" + jsonData );
	    	return e.getMessage().toString();
	    } catch (IOException e) {
	        LOG.error("schemavalidate:IO exception; something went wrong trying to read json data for event:" + jsonData);
	        return e.getMessage().toString();
	    }
	    if (report != null) {
	        Iterator<ProcessingMessage> iter = report.iterator();
	        while (iter.hasNext()) {
	            ProcessingMessage pm = iter.next();
	            LOG.trace("Processing Message: "+pm.getMessage());
	        }
	        result = String.valueOf(report.isSuccess());
	    }
	    try {
	    LOG.debug("Validation Result:" +result + " Validation report:" + report);
	    }
	    catch (NullPointerException e){
	    	LOG.error("schemavalidate:NullpointerException on report");
	    }
	    return result;
	}

	

	static LinkedBlockingQueue<JSONObject> fProcessingInputQueue;
	private static ApiServer fTomcatServer = null;
	private static final Logger LOG = LoggerFactory.getLogger ( CommonStartup.class );

}

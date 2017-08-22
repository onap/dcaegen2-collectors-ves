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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.TimeUnit;

import java.security.GeneralSecurityException;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.cambria.client.CambriaClientBuilders;
import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;


public class EventPublisher {

	private static EventPublisher instance = null;
	private static CambriaBatchingPublisher pub = null;
	
	private String streamid = "";
	private String ueburl="";
	private String topic="";
	private String authuser="";
	private String authpwd="";
	
	private static Logger log = LoggerFactory.getLogger(EventPublisher.class);

	
	private EventPublisher( String newstreamid) {
		
		this.streamid = newstreamid;
		try { 
				ueburl=DmaapPropertyReader.getInstance(CommonStartup.cambriaConfigFile).dmaap_hash.get(streamid+".cambria.url");
				
				if (ueburl==null){
					ueburl= DmaapPropertyReader.getInstance(CommonStartup.cambriaConfigFile).dmaap_hash.get(streamid+".cambria.hosts");
				}
				topic= DmaapPropertyReader.getInstance(CommonStartup.cambriaConfigFile).getKeyValue(streamid+".cambria.topic");
				authuser = DmaapPropertyReader.getInstance(CommonStartup.cambriaConfigFile).getKeyValue(streamid+".basicAuthUsername");
				
				
				if (authuser != null) {
							authpwd= DmaapPropertyReader.getInstance(CommonStartup.cambriaConfigFile).dmaap_hash.get(streamid+".basicAuthPassword");
				}			
		} 
		catch(Exception e) {
			log.error("CambriaClientBuilders connection reader exception : " + e.getMessage());
			
		}
		         
	}
	
	
	public static synchronized EventPublisher getInstance( String streamid){
	       if (instance == null) {
	           instance = new EventPublisher(streamid);
	       }
	       if (!instance.streamid.equals(streamid)){
	    	   instance.closePublisher();
	    	   instance = new EventPublisher(streamid);
	       }
	       return instance;
	      
		}
	
	
	public synchronized void sendEvent(JSONObject event) {
		
		log.debug("EventPublisher.sendEvent: instance for publish is ready");
		
		
		if (event.has("VESuniqueId"))
		{
			String uuid = event.get("VESuniqueId").toString();
			LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid.toString());
			localLC .put ( EcompFields.kBeginTimestampMs, SaClock.now () );
			log.debug("Removing VESuniqueid object from event");
			event.remove("VESuniqueId");
		}
		
		
		

		try {
		
				if (authuser != null)
				{
					log.debug("URL:" + ueburl + "TOPIC:" + topic +  "AuthUser:" + authuser +  "Authpwd:" + authpwd);
					pub = new CambriaClientBuilders.PublisherBuilder ()
					 .usingHosts (ueburl)
					 .onTopic (topic)
					 .usingHttps()
					 .authenticatedByHttp (authuser, authpwd )
					 .logSendFailuresAfter(5)
				//	 .logTo(log)
				//	 .limitBatch(100, 10)
					 .build ();
				} 
				else
				{
			
					log.debug("URL:" + ueburl + "TOPIC:" + topic );
					pub = new CambriaClientBuilders.PublisherBuilder ()
							.usingHosts (ueburl)
							 .onTopic (topic)
					//		 .logTo(log)
							 .logSendFailuresAfter(5)
					//		 .limitBatch(100, 10)
							 .build ();
								
				}
			
			int pendingMsgs = pub.send("MyPartitionKey", event.toString());
			//this.wait(2000);
			
			if(pendingMsgs > 100) {
				log.info("Pending Message Count="+pendingMsgs);
			}
			
			//closePublisher();
			log.info("pub.send invoked - no error");
			CommonStartup.oplog.info ("URL:" + ueburl + "TOPIC:" + topic + "Event Published:" + event);
			
		} catch(IOException e) {
			log.error("IOException:Unable to publish event:" + event + " streamid:" + this.streamid + " Exception:" + e.toString()); 
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			log.error("GeneralSecurityException:Unable to publish event:" + event + " streamid:" + this.streamid + " Exception:" + e.toString());
		} 
		catch (IllegalArgumentException e)
		{
			log.error("IllegalArgumentException:Unable to publish event:" + event + " streamid:" + this.streamid + " Exception:" + e.toString());
		}
			
	}


    public synchronized void closePublisher() {
		
		try { 
			if (pub!= null)
			{
				final List<?> stuck = pub.close(20, TimeUnit.SECONDS);
				if ( stuck.size () > 0 ) { 
					log.error(stuck.size() + " messages unsent" ); 
				}
			}
		}
		catch(InterruptedException ie) {
			log.error("Caught an Interrupted Exception on Close event");
		}catch(IOException ioe) {
			log.error("Caught IO Exception: " + ioe.toString()); 
		}
		
	}
}

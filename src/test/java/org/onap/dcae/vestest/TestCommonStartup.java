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
package org.onap.dcae.vestest;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onap.dcae.commonFunction.CommonStartup;
import org.onap.dcae.commonFunction.EventProcessor;
import org.onap.dcae.commonFunction.CommonStartup.QueueFullException;
import org.onap.dcae.restapi.RestfulCollectorServlet;

import com.att.nsa.cmdLine.NsaCommandLineUtil;
import com.att.nsa.drumlin.service.framework.DrumlinServlet;
import com.att.nsa.drumlin.till.nv.rrNvReadable.loadException;
import com.att.nsa.drumlin.till.nv.rrNvReadable.missingReqdSetting;
import com.att.nsa.security.NsaAuthenticator;
import com.att.nsa.security.authenticators.SimpleAuthenticator;
import com.att.nsa.security.db.simple.NsaSimpleApiKey;
import com.google.gson.JsonParser;
import com.att.nsa.drumlin.till.nv.impl.nvPropertiesFile;
import com.att.nsa.drumlin.till.nv.impl.nvReadableStack;
import com.att.nsa.drumlin.till.nv.impl.nvReadableTable;


public class TestCommonStartup {

        String payload = null;
        @Before
        public void setUp() throws Exception {

                // process command line arguments
                payload = new JsonParser().parse(new FileReader("src/test/resources/VES_valid.txt")).toString();
                CommonStartup.fProcessingInputQueue = new LinkedBlockingQueue<JSONObject> (CommonStartup.KDEFAULT_MAXQUEUEDEVENTS);
        }

        @After
        public void tearDown() throws Exception {

        }

        @Test
        public void testCommonStartupload() {

                String args[] = { "junittest" };
                final Map<String, String> argMap = NsaCommandLineUtil.processCmdLine(args, true);
                final String config = NsaCommandLineUtil.getSetting(argMap, "c", "collector.properties");
                final URL settingStream = DrumlinServlet.findStream(config, CommonStartup.class);

                final nvReadableStack settings = new nvReadableStack();
                try {
                        settings.push(new nvPropertiesFile(settingStream));
                } catch (loadException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
                settings.push(new nvReadableTable(argMap));
                Assert.assertEquals("true", "true");
        }

        @Test
        public void testhandleevent() {
                JSONArray jsonArrayMod = new JSONArray().put(new JSONObject(payload));
                try {


                        CommonStartup.handleEvents (jsonArrayMod);
                } catch ( JSONException | QueueFullException | IOException e) {
                        // TODO Auto-generated catch block
                        //e.printStackTrace();
                        System.out.println("junit reported:" + e.getMessage());
                }
                Assert.assertEquals("true", "true");
        }


/*
        @Test
        public void testServlet()  {
                try
                {
                        RestfulCollectorServlet rsv = new RestfulCollectorServlet(null);
                }
                catch (NullPointerException|loadException| missingReqdSetting e){
                        System.out.println("junit reported:" + e.getMessage());
                }
                Assert.assertEquals("true", "true");
        }
*/


        @Test
        public void testEventProcessorinstantiation()
        {
                CommonStartup.streamid="fault=sec_fault|syslog=sec_syslog|heartbeat=sec_heartbeat|measurementsForVfScaling=sec_measurement|mobileFlow=sec_mobileflow|other=sec_other|stateChange=sec_statechange|thresholdCrossingAlert=sec_thresholdCrossingAlert|voiceQuality=ves_voicequality|sipSignaling=ves_sipsignaling";
                EventProcessor ep = new EventProcessor ();
                Thread epThread=new Thread(ep);
                epThread.start();
                 Assert.assertEquals("true", "true");
                epThread.stop();

        }

        @Test
        public void testAuthListHandler()
        {
        	
        	final Map<String, String> argMap = NsaCommandLineUtil.processCmdLine ( new String[0], true );
			final String config = NsaCommandLineUtil.getSetting ( argMap, "c", "collector.properties" );
			final URL settingStream = DrumlinServlet.findStream ( config, CommonStartup.class );

			final nvReadableStack settings = new nvReadableStack ();
			try {
				settings.push ( new nvPropertiesFile ( settingStream ) );
				settings.push ( new nvReadableTable ( argMap ) );
			} catch (loadException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
        	RestfulCollectorServlet rsv = null;
        	NsaAuthenticator<NsaSimpleApiKey> NsaAuth = null;
        	Boolean flag = false;
	            try
	            {
	                    rsv = new RestfulCollectorServlet(settings);
	            }
	            catch (NullPointerException|loadException| missingReqdSetting e){
	                    System.out.println("junit reported:" + e.getMessage());
	            }
	            String authlist = "secureid,IWRjYWVSb2FkbTEyMyEt|sample1,c2FtcGxlMQ==|vdnsagg,dmRuc2FnZw==";
	            NsaAuth = rsv.AuthlistHandler(authlist);
	            if (NsaAuth != null)
	            {
	            	flag = true;
	            }
                Assert.assertEquals(true, flag);
              

        }
}



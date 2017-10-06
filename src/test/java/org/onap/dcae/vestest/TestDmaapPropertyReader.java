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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.onap.dcae.commonFunction.DMaaPChannel;
import org.onap.dcae.commonFunction.DmaapPropertyReader;

public class TestDmaapPropertyReader {

	DmaapPropertyReader dr;
	DmaapPropertyReader newDr;
	String testinput = "src/test/resources/testDmaapConfig.json";
	// Configuration following new format
	String newConfig = "src/test/resources/test-new-dmaap-config.json";

	private static final String SEC_FAULT_UEB = "sec_fault_ueb";
	private static final String SEC_FAULT = "sec_fault";
	private static final String SEC_MEAS = "sec_measurement";
	private static final String SEC_FAULT_UNAUTH = "sec_fault_unauth";
	private static final String SEC_MEAS_UNAUTH = "sec_measurement_unauth";
	private static final String FAULT_TOPIC = "onap-dmaap.dmaapcluster.SEC_FAULT_OUTPUT";
	private static final String MEAS_TOPIC = "onap-dmaap.dmaapcluster.SEC_MEASUREMENT_OUTPUT";
	private static final String UNAUTH_FAULT_TOPIC = "unauthenticated.SEC_FAULT_OUTPUT";
	private static final String UNAUTH_MEAS_TOPIC = "unauthenticated.SEC_MEASUREMENT_OUTPUT";
	private static final String HOST = "uebsb91kcdc.it.att.com";
	private static final String UNAUTH_PORT = "3904";
	private static final String PORT = "3905";
	private static final String COLON = ":";
	private static final String USER = "user";
	private static final String PASS = "pass";
	private static final DMaaPChannel EXP_CH =
			DMaaPChannel.builder()
					.name(SEC_FAULT_UEB)
					.type("out")
					.streamClass("HpCambriaOutputStream")
					.cambriaHosts("uebsb91kcdc.it.att.com,uebsb92kcdc.it.att.com,uebsb93kcdc.it.att.com")
					.cambriaUrl("uebsb91kcdc.it.att.com,uebsb92kcdc.it.att.com,uebsb93kcdc.it.att.com")
					.cambriaTopic("DCAE-SE-COLLECTOR-EVENTS-DEV")
					.stripHpId("true")
					.build();
	private static final DMaaPChannel.Builder NEW_UNAUTH_CH_BUILDER =
			DMaaPChannel.builder()
					.cambriaUrl(HOST + COLON + UNAUTH_PORT)
					.cambriaHosts(HOST);
	private static final DMaaPChannel.Builder NEW_CH_BUILDER =
			DMaaPChannel.builder()
					.cambriaUrl(HOST + COLON + PORT)
					.cambriaHosts(HOST)
					.basicAuthUsername(USER)
					.basicAuthPassword(PASS);
	private static final DMaaPChannel EXP_NEW_SEC_FAULT_UNAUTH_CH =
			NEW_UNAUTH_CH_BUILDER
					.name(SEC_FAULT_UNAUTH)
					.cambriaTopic(UNAUTH_FAULT_TOPIC)
					.build();
	private static final DMaaPChannel EXP_NEW_SEC_MEAS_UNAUTH_CH =
			NEW_UNAUTH_CH_BUILDER
					.name(SEC_MEAS_UNAUTH)
					.cambriaTopic(UNAUTH_MEAS_TOPIC)
					.build();
	private static final DMaaPChannel EXP_NEW_SEC_FAULT_CH =
			NEW_CH_BUILDER
					.name(SEC_FAULT)
					.cambriaTopic(FAULT_TOPIC)
					.build();
	private static final DMaaPChannel EXP_NEW_SEC_MEAS_CH =
			NEW_CH_BUILDER
					.name(SEC_MEAS)
					.cambriaTopic(MEAS_TOPIC)
					.build();

	@Before
	public void setUp() throws Exception {

		// process command line arguments

		dr = new DmaapPropertyReader(testinput);
		newDr = new DmaapPropertyReader(newConfig);
	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void testDmaapPropertyReader() {
		Assert.assertEquals(EXP_CH, dr.getChannel(SEC_FAULT_UEB));
		Assert.assertEquals(EXP_NEW_SEC_FAULT_CH, newDr.getChannel(SEC_FAULT));
		Assert.assertEquals(EXP_NEW_SEC_MEAS_CH, newDr.getChannel(SEC_MEAS));
		Assert.assertEquals(EXP_NEW_SEC_FAULT_UNAUTH_CH, newDr.getChannel(SEC_FAULT_UNAUTH));
		Assert.assertEquals(EXP_NEW_SEC_MEAS_UNAUTH_CH, newDr.getChannel(SEC_MEAS_UNAUTH));
	}
}


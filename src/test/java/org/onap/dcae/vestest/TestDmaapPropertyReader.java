/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;

import org.onap.dcae.commonFunction.DmaapPropertyReader;

import java.util.*;

import static org.junit.Assert.*;

public class TestDmaapPropertyReader {


	private static final String legacyConfigFilePath = "src/test/resources/testDmaapConfig_ip.json";
	private static final String dmaapInputConfigFilePath = "src/test/resources/testDmaapConfig_gen2.json";
	private static final String fullDmaapConfigWithChannels = "src/test/resources/testFullDmaapConfig_channels.json";
	private static final String fullGen2DmaapConfig = "src/test/resources/testFullDmaapConfig_gen2.json";

	private static final String FAULT_UEB_KEY_PREFIX = "sec_fault_ueb";
	private static final String VES_ALERT_SND_KEY_PREFIX = "ves-thresholdCrossingAlert-secondary";
	private static final String VES_FAULT_SECONDARY = "ves-fault-secondary";

	private static final String FAULT_BASIC_AUTH_USERNAME_KEY = VES_FAULT_SECONDARY + ".basicAuthUsername";
	private static final String ALERT_BASIC_AUTH_PWD_KEY = VES_ALERT_SND_KEY_PREFIX + ".basicAuthPassword";

	private static final String VES_ALERT_CAMBRIA_TOPIC_KEY = VES_ALERT_SND_KEY_PREFIX + ".cambria.topic";
	private static final String VES_ALERT_CAMBRIA_URL_KEY = VES_ALERT_SND_KEY_PREFIX + ".cambria.url";
	private static final String VES_FAULT_SND_CAMBRIA_URL_KEY = VES_FAULT_SECONDARY + ".cambria.url";
	private static final String VES_FAULT_SND_AUTH_PWD_KEY = VES_FAULT_SECONDARY + ".basicAuthPassword";
	private static final String VES_FAULT_SND_CAMBRIA_TOPIC_KEY = VES_FAULT_SECONDARY + ".cambria.topic";
	private static final String FAULT_UEB_CAMBRIA_HOSTS_KEY = FAULT_UEB_KEY_PREFIX + ".cambria.hosts";
	private static final String FAULT_UEB_CAMBRIA_TOPIC_KEY = FAULT_UEB_KEY_PREFIX + ".cambria.topic";
	private static final String VES_ALERT_SND_AUTH_USERNAME_KEY = VES_ALERT_SND_KEY_PREFIX + ".basicAuthUsername";

	private static final Map<String, String> expectedCompleteGen2DmaapConfig = ImmutableMap.<String, String> builder()
			.put(ALERT_BASIC_AUTH_PWD_KEY, "SamplePassWD2")
			.put(VES_ALERT_CAMBRIA_TOPIC_KEY, "DCAE-SE-COLLECTOR-EVENTS-DEV")
			.put(FAULT_BASIC_AUTH_USERNAME_KEY, "sampleUsername")
			.put(VES_ALERT_CAMBRIA_URL_KEY, "UEBHOST:3904")
			.put(VES_FAULT_SND_CAMBRIA_URL_KEY, "UEBHOST:3904")
			.put(VES_FAULT_SND_AUTH_PWD_KEY, "SamplePasswd")
			.put(VES_FAULT_SND_CAMBRIA_TOPIC_KEY, "DCAE-SE-COLLECTOR-EVENTS-DEV")
			.put(VES_ALERT_SND_AUTH_USERNAME_KEY, "sampleUsername2")
			.build();

	private static final Map<String, String> expectedIncompleteGen2DmaapConfig = ImmutableMap.<String, String> builder()
			.put(VES_ALERT_SND_AUTH_USERNAME_KEY, "null")
			.put(FAULT_BASIC_AUTH_USERNAME_KEY, "null")
			.put(VES_ALERT_CAMBRIA_TOPIC_KEY, "DCAE-SE-COLLECTOR-EVENTS-DEV")
			.put(VES_ALERT_CAMBRIA_URL_KEY, "UEBHOST:3904")
			.put(VES_FAULT_SND_CAMBRIA_URL_KEY, "UEBHOST:3904")
			.put(ALERT_BASIC_AUTH_PWD_KEY, "null")
			.put(VES_FAULT_SND_AUTH_PWD_KEY, "null")
			.put(VES_FAULT_SND_CAMBRIA_TOPIC_KEY, "DCAE-SE-COLLECTOR-EVENTS-DEV")
			.build();

	private static final Map<String, String> expectedCompleteChannelsDmaapConfig = ImmutableMap.<String, String> builder()
			.put(FAULT_UEB_CAMBRIA_HOSTS_KEY, "uebsb91kcdc.it.att.com,uebsb92kcdc.it.att.com,uebsb93kcdc.it.att.com")
			.put(FAULT_UEB_CAMBRIA_TOPIC_KEY, "DCAE-SE-COLLECTOR-EVENTS-DEV")
			.put(FAULT_UEB_KEY_PREFIX +".basicAuthPassword", "S0mEPassWD")
			.put(FAULT_UEB_KEY_PREFIX +".basicAuthUsername","sampleUser")
			.put(FAULT_UEB_KEY_PREFIX +".cambria.url","127.0.0.1:3904")
			.build();

	private static final Map<String, String> expectedIncompleteChannelsDmaapConfig = ImmutableMap.<String, String> builder()
			.put(FAULT_UEB_CAMBRIA_HOSTS_KEY,"uebsb91kcdc.it.att.com,uebsb92kcdc.it.att.com,uebsb93kcdc.it.att.com")
			.put(FAULT_UEB_CAMBRIA_TOPIC_KEY, "DCAE-SE-COLLECTOR-EVENTS-DEV")
			.build();

	@Test
	public void testShouldCreateReaderWithAbsentParamsOmittedBasedOnChannelDmaapConfig(){
		assertReaderPreservedAllEntriesAfterTransformation(legacyConfigFilePath, expectedIncompleteChannelsDmaapConfig);
	}

	@Test
	public void testShouldCreateReaderWithAbsentParamsOmittedBasedOnGen2DmaapConfig(){
		assertReaderPreservedAllEntriesAfterTransformation(dmaapInputConfigFilePath, expectedIncompleteGen2DmaapConfig);
	}

	@Test
	public void shouldCreateReaderWithCompleteChannelDmaapConfig() {
		assertReaderPreservedAllEntriesAfterTransformation(fullDmaapConfigWithChannels, expectedCompleteChannelsDmaapConfig);
	}

	@Test
	public void shouldCreateReaderWithCompleteGen2DmaapConfig(){
		assertReaderPreservedAllEntriesAfterTransformation(fullGen2DmaapConfig, expectedCompleteGen2DmaapConfig);
	}

	private void assertReaderPreservedAllEntriesAfterTransformation(String dmaapConfigFilePath, Map<String, String> expectedMap){
		DmaapPropertyReader reader = new DmaapPropertyReader(dmaapConfigFilePath);
		assertTrue(CollectionUtils.isEqualCollection(reader.getDmaapProperties().entrySet(), expectedMap.entrySet()));
	}

}


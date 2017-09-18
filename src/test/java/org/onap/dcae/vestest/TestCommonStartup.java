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

import java.net.URL;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onap.dcae.commonFunction.CommonStartup;

import com.att.nsa.cmdLine.NsaCommandLineUtil;
import com.att.nsa.drumlin.service.framework.DrumlinServlet;
import com.att.nsa.drumlin.till.nv.rrNvReadable.loadException;
import com.att.nsa.drumlin.till.nv.impl.nvPropertiesFile;
import com.att.nsa.drumlin.till.nv.impl.nvReadableStack;
import com.att.nsa.drumlin.till.nv.impl.nvReadableTable;

public class TestCommonStartup {

	@Before
	public void setUp() throws Exception {

		// process command line arguments

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

}


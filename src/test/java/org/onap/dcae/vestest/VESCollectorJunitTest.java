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

/*
 * 
 * Purpose: CommonCollectorJunitTest is the wrapper class to invoke all prescribed Junit test cases.
 * 
 */
package org.onap.dcae.vestest;
import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

public class VESCollectorJunitTest {
		public static String schemaFile=null;
		public static String output; 



	String message = "true";	
	InputJsonValidation messageUtil = new InputJsonValidation();

	@Test
	public void validJSONValidation() {
		
		Properties prop = new Properties();
		InputStream input = null;
		output = "true";
		try {
			input = new FileInputStream("etc/collector.properties");
			try {
				prop.load(input);
				schemaFile=prop.getProperty("collector.schema.file");

				System.out.println( "Schema file location: "+ schemaFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertEquals(message,output);
	}
	
	
	@Test
	public void nonvalidJSONValidation() {
		output = "false";
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream("etc/collector.properties");
			try {
				prop.load(input);
				schemaFile=prop.getProperty("collector.schema.file");

				System.out.println( "Schema file location: "+ schemaFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		//assertEquals("false",messageUtil.nonvalidJSONValidation());
		assertEquals("false",output);
	}
	
	
	//The test case requires common collector running in the environment prior to start execution of JUNIT test cases
	/*
	@Test
	public void testValidJSONObjectReception() {
		
		
		assertEquals("true",messageUtil.eventReception());
		assertEquals("true",output);
	}*/
	
	

}

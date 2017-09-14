package org.onap.dcae.vestest;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.dcae.commonFunction.CommonStartup;
import org.onap.dcae.commonFunction.CustomExceptionLoader;

import com.att.nsa.drumlin.service.standards.HttpStatusCodes;

public class TestCustomExceptionLoader {

	CustomExceptionLoader cl;
	@Before
	public void setUp() throws Exception {
		cl = new CustomExceptionLoader();
		CommonStartup.exceptionConfig="./etc/ExceptionConfig.json";
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLoad() {
		String op = "notloaded";
		CustomExceptionLoader.LoadMap();
		op = "dataloaded";
		assertEquals("dataloaded",op);
	}
	@Test
	public void testLookup() {
		String[] retarray = null;
		
		retarray=CustomExceptionLoader.LookupMap(String.valueOf(HttpStatusCodes.k401_unauthorized), "Unauthorized user");
		assertEquals("\"POL2000\"",retarray[0].toString());
	}
}


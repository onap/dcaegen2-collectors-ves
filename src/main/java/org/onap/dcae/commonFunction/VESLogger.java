/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 * 						reserved.
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

import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.LoggingContextFactory;
import com.att.nsa.logging.log4j.EcompFields;
import jline.internal.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class VESLogger {

	public static final String VES_AGENT = "VES_AGENT";
	public static final String REQUEST_ID = "requestId";
	private static final String IP_ADDRESS = "127.0.0.1";
	private static final String HOST_NAME = "localhost";

	public static Logger auditLog;
	public static Logger metricsLog;
	public static Logger errorLog;
	public static Logger debugLog;

	// Common LoggingContext
	private static LoggingContext commonLC;
	// Thread-specific LoggingContext
	private static LoggingContext threadLC;
	public LoggingContext lc;

	/**
	 * Returns the common LoggingContext instance that is the base context for
	 * all subsequent instances.
	 *
	 * @return the common LoggingContext
	 */
	public static LoggingContext getCommonLoggingContext() {
		if (commonLC == null) {
			commonLC = new LoggingContextFactory.Builder().build();
			final UUID uuid = UUID.randomUUID();

			commonLC.put(REQUEST_ID, uuid.toString());
		}
		return commonLC;
	}

	/**
	 * Get a logging context for the current thread that's based on the common
	 * logging context. Populate the context with context-specific values.
	 *
	 * @param aUuid
	 *            uuid for request id
	 * @return a LoggingContext for the current thread
	 */
	public static LoggingContext getLoggingContextForThread(UUID aUuid) {
		// note that this operation requires everything from the common context
		// to be (re)copied into the target context. That seems slow, but it
		// actually
		// helps prevent the thread from overwriting supposedly common data. It
		// also
		// should be fairly quick compared with the overhead of handling the
		// actual
		// service call.

		threadLC = new LoggingContextFactory.Builder().withBaseContext(getCommonLoggingContext()).build();
		// Establish the request-specific UUID, as long as we are here...
		threadLC.put(REQUEST_ID, aUuid.toString());
		threadLC.put(EcompFields.kEndTimestamp, SaClock.now());

		return threadLC;
	}

	/**
	 * Get a logging context for the current thread that's based on the common
	 * logging context. Populate the context with context-specific values.
	 *
	 * @param aUuid
	 *            uuid for request id
	 * @return a LoggingContext for the current thread
	 */
	public static LoggingContext getLoggingContextForThread(String aUuid) {
		// note that this operation requires everything from the common context
		// to be (re)copied into the target context. That seems slow, but it
		// actually
		// helps prevent the thread from overwriting supposedly common data. It
		// also
		// should be fairly quick compared with the overhead of handling the
		// actual
		// service call.

		threadLC = new LoggingContextFactory.Builder().withBaseContext(getCommonLoggingContext()).build();
		// Establish the request-specific UUID, as long as we are here...
		threadLC.put(REQUEST_ID, aUuid);
		threadLC.put("statusCode", "COMPLETE");
		threadLC.put(EcompFields.kEndTimestamp, SaClock.now());
		return threadLC;
	}

	public static void setUpEcompLogging() {

		// Create ECOMP Logger instances
		auditLog = LoggerFactory.getLogger("com.att.ecomp.audit");
		metricsLog = LoggerFactory.getLogger("com.att.ecomp.metrics");
		debugLog = LoggerFactory.getLogger("com.att.ecomp.debug");
		errorLog = LoggerFactory.getLogger("com.att.ecomp.error");

		final LoggingContext lc = getCommonLoggingContext();

		String ipAddr = IP_ADDRESS;
		String hostname = HOST_NAME;
		try {
			final InetAddress ip = InetAddress.getLocalHost();
			hostname = ip.getCanonicalHostName();
			ipAddr = ip.getHostAddress();
		} catch (UnknownHostException x) {
			Log.debug(x.getMessage());
		}

		lc.put("serverName", hostname);
		lc.put("serviceName", "VESCollecor");
		lc.put("statusCode", "RUNNING");
		lc.put("targetEntity", "NULL");
		lc.put("targetServiceName", "NULL");
		lc.put("server", hostname);
		lc.put("serverIpAddress", ipAddr);

		// instance UUID is meaningless here, so we just create a new one each
		// time the
		// server starts. One could argue each new instantiation of the service
		// should
		// have a new instance ID.
		lc.put("instanceUuid", "");
		lc.put("severity", "");
		lc.put(EcompFields.kEndTimestamp, SaClock.now());
		lc.put("EndTimestamp", SaClock.now());
		lc.put("partnerName", "NA");
	}

}

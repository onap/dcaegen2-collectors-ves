/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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

import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import org.junit.Test;
import org.onap.dcae.commonFunction.VESLogger;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.onap.dcae.commonFunction.VESLogger.REQUEST_ID;

public class TestVESLogger {

    @Test
    public void shouldOnLoggingContextInitializationPutRandomUuidAsRequestId() {
        LoggingContext commonLoggingContext = VESLogger.getCommonLoggingContext();
        String requestId = commonLoggingContext.get(REQUEST_ID, "default");

        assertNotNull(requestId);
        assertNotSame(requestId, "default");

    }

    @Test
    public void shouldOnLoggingContextInitializationPutGivenUuuidAsRequestIdAndSupplyEndTimestamp() {
        final UUID uuid = UUID.randomUUID();
        LoggingContext loggingContextForThread = VESLogger.getLoggingContextForThread(uuid);
        String requestId = loggingContextForThread.get(REQUEST_ID, "default");
        String endTimestamp = loggingContextForThread.get(EcompFields.kEndTimestamp, "default");

        assertNotNull(requestId);
        assertNotNull(endTimestamp);
        assertNotSame(endTimestamp, "default");
        assertEquals(requestId, uuid.toString());
    }

    @Test
    public void shouldOnLoggingContextInitializationPutGivenUuidAsRequestIdAndSupplyEndTimestampAndCompleteStatusCode() {
        final UUID uuid = UUID.randomUUID();
        LoggingContext loggingContextForThread = VESLogger.getLoggingContextForThread(uuid.toString());
        String requestId = loggingContextForThread.get(REQUEST_ID, "default");
        String statusCode = loggingContextForThread.get("statusCode", "default");
        String endTimestamp = loggingContextForThread.get(EcompFields.kEndTimestamp, "default");

        assertNotNull(requestId);
        assertNotNull(endTimestamp);
        assertNotNull(statusCode);
        assertNotSame(endTimestamp, "default");
        assertEquals(requestId, uuid.toString());
        assertEquals(statusCode, "COMPLETE");
    }

}


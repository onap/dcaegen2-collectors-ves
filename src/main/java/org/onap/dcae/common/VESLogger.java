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

package org.onap.dcae.common;

import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.LoggingContextFactory;
import com.att.nsa.logging.log4j.EcompFields;
import java.util.UUID;

public class VESLogger {

    public static final String REQUEST_ID = "requestId";
    private static LoggingContext commonLC;
    private static LoggingContext threadLC;

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
     * @param aUuid uuid for request id
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
     * @param aUuid uuid for request id
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
}

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

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.onap.dcae.commonFunction.CustomExceptionLoader.LookupMap;

import com.att.nsa.drumlin.service.standards.HttpStatusCodes;
import org.junit.Test;
import org.onap.dcae.commonFunction.CommonStartup;
import org.onap.dcae.commonFunction.CustomExceptionLoader;

public class TestCustomExceptionLoader {

    @Test
    public void shouldLoadingADefaultExceptionMappingDoNotThrowExceptions() {
        CommonStartup.exceptionConfig = "./etc/ExceptionConfig.json";
        CustomExceptionLoader.LoadMap();
    }

    @Test
    public void shouldLookupErrorMessagePartsOutOfStatusCodeAndReason() {
        // given
        CommonStartup.exceptionConfig = "./etc/ExceptionConfig.json";
        CustomExceptionLoader.LoadMap();
        int statusCode = HttpStatusCodes.k401_unauthorized;
        String message = "Unauthorized user";

        // when
        String[] retarray = LookupMap(String.valueOf(statusCode), message);

        // then
        if (retarray == null) {
            fail(format(
                "Lookup failed, did not find value for a valid status code %s and message %s", statusCode, message));
        } else {
            assertEquals("\"POL2000\"", retarray[0]);
        }
    }
}


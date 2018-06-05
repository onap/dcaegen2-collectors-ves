/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2018 Nokia. All rights reserved.
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

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Test;
import org.onap.dcae.restapi.ApiException;
import org.onap.dcae.restapi.ApiException.ExceptionType;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
public class ApiExceptionTest {

    @Test
    public void shouldStringifyServiceExceptionTypeAccordingToSpecification() {
        assertEquals(ExceptionType.SERVICE_EXCEPTION.toString(), "ServiceException");
    }

    @Test
    public void shouldStringifyPolicyExceptionTypeAccordingToSpecification() {
        assertEquals(ExceptionType.POLICY_EXCEPTION.toString(), "PolicyException");
    }

    @Test
    public void shouldConvertExceptionToBackwardCompatibleFormat() {
        JSONObject responseBody = ApiException.UNAUTHORIZED_USER.toJSON();
        assertJSONEqual(responseBody, asJSON(""
            + "{                                             "
            + "  'requestError': {                           "
            + "     'PolicyException': {                     "
            + "        'messageId': 'POL2000',                "
            + "        'text': 'Unauthorized user'           "
            + "     }                                        "
            + "  }                                           "
            + "}                                             "
        ));
    }

    private JSONObject asJSON(String jsonString) {
        return new JSONObject(jsonString.replace("'", "\""));
    }

    private void assertJSONEqual(JSONObject o1, JSONObject o2) {
        assertEquals(o1.toString(), o2.toString());
    }
}

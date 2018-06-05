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
package org.onap.dcae.restapi;

import com.google.common.base.CaseFormat;
import org.json.JSONObject;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
public enum ApiException {

    INVALID_JSON_INPUT(ExceptionType.SERVICE_EXCEPTION, "SVC0002", "Incorrect JSON payload", 400),
    SCHEMA_VALIDATION_FAILED(ExceptionType.SERVICE_EXCEPTION, "SVC0002", "Bad Parameter (JSON does not conform to schema)", 400),
    INVALID_CONTENT_TYPE(ExceptionType.SERVICE_EXCEPTION, "SVC0002", "Bad Parameter (Incorrect request Content-Type)", 400),
    UNAUTHORIZED_USER(ExceptionType.POLICY_EXCEPTION, "POL2000", "Unauthorized user", 401),
    NO_SERVER_RESOURCES(ExceptionType.SERVICE_EXCEPTION, "SVC1000", "No server resources (internal processing queue full)", 503);

    public final ExceptionType exceptionType;
    public final String exceptionCode;
    public final String exceptionDetails;
    public final int httpStatusCode;

    ApiException(ExceptionType exceptionType, String exceptionCode, String exceptionDetails, int httpStatusCode) {
        this.exceptionType = exceptionType;
        this.exceptionCode = exceptionCode;
        this.exceptionDetails = exceptionDetails;
        this.httpStatusCode = httpStatusCode;
    }

    public enum ExceptionType {
        SERVICE_EXCEPTION, POLICY_EXCEPTION;

        @Override
        public String toString() {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
        }
    }

    /*
     * The body of the error response differs from code representation so we need to convert it to maintain backward
     * compatibility.
     *
     * Backward compatible format has 2 issues:
     * 1) a typo: MessagID
     * 2) double quoted MessagID and text fields like this:
     * {
     *    "requestError":{
     *       "GeneralException":{
     *          "MessagID":"\"SVC0002\"",
     *          "text":"\"Bad Parameter - Schema Validation Failure\""
     *       }
     *    }
     * }
     *
     * TODO: Check if it is safe to fix those 2 issues.
     */
    public JSONObject toJSON() {
        JSONObject exceptionTypeNode = new JSONObject();
        exceptionTypeNode.put("MessagID", "\"" + exceptionCode + "\"");
        exceptionTypeNode.put("text", "\"" + exceptionDetails + "\"");

        JSONObject requestErrorNode = new JSONObject();
        requestErrorNode.put(exceptionType.toString(), exceptionTypeNode);

        JSONObject rootNode = new JSONObject();
        rootNode.put("requestError", requestErrorNode);
        return rootNode;
    }

}

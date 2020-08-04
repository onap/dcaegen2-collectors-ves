/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
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
    INVALID_CUSTOM_HEADER(ExceptionType.SERVICE_EXCEPTION, "SVC0002", "Bad Parameter (Incorrect request api version)", 400),
    MISSING_NAMESPACE_PARAMETER(ExceptionType.SERVICE_EXCEPTION, "SVC2006", "Mandatory input attribute event.commonEventHeader.stndDefinedNamespace is missing from request", 400),
    EMPTY_NAMESPACE_PARAMETER(ExceptionType.SERVICE_EXCEPTION, "SVC2006", "Mandatory input attribute event.commonEventHeader.stndDefinedNamespace is empty in request", 400),
    NO_SERVER_RESOURCES(ExceptionType.SERVICE_EXCEPTION, "SVC1000", "No server resources (internal processing queue full)", 503),
    STND_DEFINED_VALIDATION_FAILED(ExceptionType.SERVICE_EXCEPTION, "SVC2000", "Event.stndDefinedFields.data invalid against event.stndDefinedFields.schemaReference", 400),
    NO_LOCAL_SCHEMA_REFERENCE(ExceptionType.SERVICE_EXCEPTION, "SVC2004", "Invalid input value for attribute event.stndDefinedFields.schemaReference. Referred external schema not present in schema repository", 400),
    INCORRECT_INTERNAL_FILE_REFERENCE(ExceptionType.SERVICE_EXCEPTION, "SVC2000", "Event.stndDefinedFields.schemaReference value does not correspond to any external event schema file in variable externalSchema repo", 400);

    public final int httpStatusCode;
    private final ExceptionType type;
    private final String code;
    private final String details;

    ApiException(ExceptionType type, String code, String details, int httpStatusCode) {
        this.type = type;
        this.code = code;
        this.details = details;
        this.httpStatusCode = httpStatusCode;
    }

    public JSONObject toJSON() {
        JSONObject exceptionTypeNode = new JSONObject();
        exceptionTypeNode.put("messageId", code);
        exceptionTypeNode.put("text", details);

        JSONObject requestErrorNode = new JSONObject();
        requestErrorNode.put(type.toString(), exceptionTypeNode);

        JSONObject rootNode = new JSONObject();
        rootNode.put("requestError", requestErrorNode);
        return rootNode;
    }

    public enum ExceptionType {
        SERVICE_EXCEPTION, POLICY_EXCEPTION;

        @Override
        public String toString() {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.name());
        }
    }

}

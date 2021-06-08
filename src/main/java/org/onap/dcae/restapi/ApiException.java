/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2020-2021 Nokia. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
public enum ApiException {

    INVALID_JSON_INPUT(ExceptionType.SERVICE_EXCEPTION, "SVC0002", "Incorrect JSON payload", 400),
    SCHEMA_VALIDATION_FAILED(ExceptionType.SERVICE_EXCEPTION, "SVC0002", "Bad Parameter (JSON does not conform to schema)", 400),
    INVALID_CONTENT_TYPE(ExceptionType.SERVICE_EXCEPTION, "SVC0002", "Bad Parameter (Incorrect request Content-Type)", 400),
    UNAUTHORIZED_USER(ExceptionType.POLICY_EXCEPTION, "POL2000", "Unauthorized user", 401),
    INVALID_CUSTOM_HEADER(ExceptionType.SERVICE_EXCEPTION, "SVC0002", "Bad Parameter (Incorrect request api version)", 400),
    MISSING_NAMESPACE_PARAMETER(ExceptionType.SERVICE_EXCEPTION, "SVC2006", "Mandatory input %1 %2 is missing from request", List.of("attribute", "event.commonEventHeader.stndDefinedNamespace"), 400),
    EMPTY_NAMESPACE_PARAMETER(ExceptionType.SERVICE_EXCEPTION, "SVC2006", "Mandatory input %1 %2 is empty in request", List.of("attribute", "event.commonEventHeader.stndDefinedNamespace"), 400),
    NO_SERVER_RESOURCES(ExceptionType.SERVICE_EXCEPTION, "SVC1000", "No server resources (internal processing queue full)", 503),
    STND_DEFINED_VALIDATION_FAILED(ExceptionType.SERVICE_EXCEPTION, "SVC2000", "The following service error occurred: %1. Error code is %2", List.of("event.stndDefinedFields.data invalid against event.stndDefinedFields.schemaReference", "400"), 400),
    NO_LOCAL_SCHEMA_REFERENCE(ExceptionType.SERVICE_EXCEPTION, "SVC2004", "Invalid input value for %1 %2: %3", List.of("attribute", "event.stndDefinedFields.schemaReference", "Referred external schema not present in schema repository"), 400),
    INCORRECT_INTERNAL_FILE_REFERENCE(ExceptionType.SERVICE_EXCEPTION, "SVC2000", "The following service error occurred: %1. Error code is %2", List.of("event.stndDefinedFields.schemaReference value does not correspond to any external event schema file in externalSchema repo", "400"), 400),
    DIFFERENT_DOMAIN_FIELDS_IN_BATCH_EVENT(ExceptionType.SERVICE_EXCEPTION, "SVC0001", "Different value of domain fields in Batch Event", 400),
    DIFFERENT_STND_DEFINED_NAMESPACE_WHEN_DOMAIN_STND_DEFINED(ExceptionType.SERVICE_EXCEPTION, "SVC0001","Value of stndDefinedNamespace fields have to be same when domain is stndDefined",400),
    DOMAIN_NOT_DEFINED_FOR_STREAM_ID(ExceptionType.SERVICE_EXCEPTION, "SVC2000", "The following service error occurred: %1. Error code is %2", List.of("No domain defined for stream id", "400"), 400),
    PAYLOAD_TO_LARGE(ExceptionType.SERVICE_EXCEPTION, "SVC2000", "The following service error occurred: %1. Error code is %2", List.of("Request Entity Too Large", "413"), 413),
    NOT_FOUND(ExceptionType.SERVICE_EXCEPTION, "SVC2000", "The following service error occurred: %1. Error code is %2", List.of("Not Found","404"), 404),
    REQUEST_TIMEOUT(ExceptionType.SERVICE_EXCEPTION, "SVC2000", "The following service error occurred: %1. Error code is %2", List.of("Request Timeout","408"), 408),
    TOO_MANY_REQUESTS(ExceptionType.SERVICE_EXCEPTION, "SVC2000", "The following service error occurred: %1. Error code is %2", List.of("Too Many Requests","429"), 429),
    INTERNAL_SERVER_ERROR(ExceptionType.SERVICE_EXCEPTION, "SVC2000", "The following service error occurred: %1. Error code is %2", List.of("Internal Server Error","500"), 500),
    BAD_GATEWAY(ExceptionType.SERVICE_EXCEPTION, "SVC2000", "The following service error occurred: %1. Error code is %2", List.of("Bad Gateway","502"), 502),
    SERVICE_UNAVAILABLE(ExceptionType.SERVICE_EXCEPTION, "SVC2000", "The following service error occurred: %1. Error code is %2", List.of("Service Unavailable","503"), 503),
    GATEWAY_TIMEOUT(ExceptionType.SERVICE_EXCEPTION, "SVC2000", "The following service error occurred: %1. Error code is %2", List.of("Gateway Timeout","504"), 504);

    public final int httpStatusCode;
    private final ExceptionType type;
    private final String code;
    private final String details;
    private final List<String> variables;

    ApiException(ExceptionType type, String code, String details, int httpStatusCode) {
        this(type, code, details, new ArrayList<>(), httpStatusCode);
    }

    ApiException(ExceptionType type, String code, String details, List<String> variables, int httpStatusCode) {
        this.type = type;
        this.code = code;
        this.details = details;
        this.variables = variables;
        this.httpStatusCode = httpStatusCode;
    }

    public JSONObject toJSON() {
        JSONObject exceptionTypeNode = new JSONObject();
        exceptionTypeNode.put("messageId", code);
        exceptionTypeNode.put("text", details);
        if(!variables.isEmpty()) {
            exceptionTypeNode.put("variables", variables);
        }

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

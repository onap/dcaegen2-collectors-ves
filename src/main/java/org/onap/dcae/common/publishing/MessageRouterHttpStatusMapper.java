/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2021 Nokia. All rights reserved.
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
package org.onap.dcae.common.publishing;

import org.jetbrains.annotations.NotNull;
import org.onap.dcae.common.model.BackwardsCompatibilityException;
import org.onap.dcae.common.model.InternalException;
import org.onap.dcae.common.model.PayloadToLargeException;
import org.onap.dcae.restapi.ApiException;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.MessageRouterPublishResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.Objects;

import static org.onap.dcae.ApplicationSettings.responseCompatibility;

public class MessageRouterHttpStatusMapper {

    private static final Logger log = LoggerFactory.getLogger(MessageRouterHttpStatusMapper.class);

    private MessageRouterHttpStatusMapper() {
    }

    @NotNull
    static HttpStatus getHttpStatus(MessageRouterPublishResponse messageRouterPublishResponse) {
        return responseCompatibility.equals("v7.2") ?
                getHttpStatusBackwardsCompatibility(messageRouterPublishResponse):
                getHttpStatusWithMappedResponseCode(messageRouterPublishResponse);
    }

    @NotNull
    private static HttpStatus getHttpStatusBackwardsCompatibility(MessageRouterPublishResponse messageRouterPublishResponse) {
        if (isHttpOk(messageRouterPublishResponse)) {
            log.info("Successfully send event to MR");
            return HttpStatus.ACCEPTED;
        } else {
            log.error(messageRouterPublishResponse.failReason());
            throw new BackwardsCompatibilityException();
        }
    }

    @NotNull
    private static HttpStatus getHttpStatusWithMappedResponseCode(MessageRouterPublishResponse messageRouterPublishResponse) {
        if (isHttpOk(messageRouterPublishResponse)) {
            log.info("Successfully send event to MR");
            return HttpStatus.OK;
        } else if (isHttp413(messageRouterPublishResponse)) {
            log.error(messageRouterPublishResponse.failReason());
            throw new PayloadToLargeException();
        } else {
            log.error(messageRouterPublishResponse.failReason());
            throw new InternalException(responseBody(resolveHttpCode(messageRouterPublishResponse)));
        }
    }

    @NotNull
    private static String resolveHttpCode(MessageRouterPublishResponse messageRouterPublishResponse) {
        return Objects.requireNonNull(messageRouterPublishResponse.failReason()).substring(0, 3);
    }

    @NotNull
    private static ApiException responseBody(String substring) {
        switch (substring) {
            case "404":
                return ApiException.NOT_FOUND;
            case "408":
                return ApiException.REQUEST_TIMEOUT;
            case "429":
                return ApiException.TOO_MANY_REQUESTS;
            case "502":
                return ApiException.BAD_GATEWAY;
            case "503":
                return ApiException.SERVICE_UNAVAILABLE;
            case "504":
                return ApiException.GATEWAY_TIMEOUT;
            default:
                return ApiException.INTERNAL_SERVER_ERROR;
        }
    }

    private static boolean isHttpOk(MessageRouterPublishResponse messageRouterPublishResponse) {
        return messageRouterPublishResponse.successful();
    }

    private static boolean isHttp413(MessageRouterPublishResponse messageRouterPublishResponse) {
        return Objects.requireNonNull(messageRouterPublishResponse.failReason()).startsWith("413");
    }
}

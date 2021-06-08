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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.onap.dcae.common.model.BackwardsCompatibilityException;
import org.onap.dcae.common.model.InternalException;
import org.onap.dcae.common.model.PayloadToLargeException;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.MessageRouterPublishResponse;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.onap.dcae.ApplicationSettings.responseCompatibility;
import static org.onap.dcae.common.publishing.MessageRouterHttpStatusMapper.getHttpStatus;

class MessageRouterHttpStatusMapperTest {

    public static final String BACKWARDS_COMPATIBILITY = "v7.2";
    public static final String BACKWARDS_COMPATIBILITY_NONE = "NONE";

    @Test
    void ves_shouldResponse202() {
        //given
        responseCompatibility = BACKWARDS_COMPATIBILITY;
        MessageRouterPublishResponse messageRouterPublishResponse = mock(MessageRouterPublishResponse.class);
        when(messageRouterPublishResponse.successful()).thenReturn(true);

        //when
        HttpStatus httpStatusResponse = getHttpStatus(messageRouterPublishResponse);

        //then
        assertSame(HttpStatus.ACCEPTED, httpStatusResponse);
    }

    @ParameterizedTest
    @EnumSource(
            value = HttpStatus.class,
            names = {"NOT_FOUND", "REQUEST_TIMEOUT", "TOO_MANY_REQUESTS", "INTERNAL_SERVER_ERROR", "BAD_GATEWAY",
                    "SERVICE_UNAVAILABLE", "GATEWAY_TIMEOUT","PAYLOAD_TOO_LARGE"}
    )
    void ves_shouldMapErrorsToBackwardsCompatibility(HttpStatus httpStatus) {
        //given
        responseCompatibility = BACKWARDS_COMPATIBILITY;
        MessageRouterPublishResponse messageRouterPublishResponse = mock(MessageRouterPublishResponse.class);
        when(messageRouterPublishResponse.failReason()).thenReturn(httpStatus.toString());

        //when
        //then
        assertThrows(BackwardsCompatibilityException.class,()->getHttpStatus(messageRouterPublishResponse));
    }

    @Test
    void ves_shouldResponse200WhenBackwardsCompatibilityIsNone() {
        //given
        responseCompatibility = BACKWARDS_COMPATIBILITY_NONE;
        MessageRouterPublishResponse messageRouterPublishResponse = mock(MessageRouterPublishResponse.class);
        when(messageRouterPublishResponse.successful()).thenReturn(true);

        //when
        HttpStatus httpStatusResponse = getHttpStatus(messageRouterPublishResponse);

        //then
        assertSame(HttpStatus.OK, httpStatusResponse);
    }

    @Test
    void ves_shouldHandleError413WhenBackwardsCompatibilityIsNone() {
        //given
        responseCompatibility = BACKWARDS_COMPATIBILITY_NONE;
        MessageRouterPublishResponse messageRouterPublishResponse = mock(MessageRouterPublishResponse.class);
        when(messageRouterPublishResponse.failReason()).thenReturn(HttpStatus.PAYLOAD_TOO_LARGE.toString());

        //when
        //then
        assertThrows(PayloadToLargeException.class,()->getHttpStatus(messageRouterPublishResponse));
    }

    @ParameterizedTest
    @EnumSource(
            value = HttpStatus.class,
            names = {"NOT_FOUND", "REQUEST_TIMEOUT", "TOO_MANY_REQUESTS", "INTERNAL_SERVER_ERROR", "BAD_GATEWAY",
                    "SERVICE_UNAVAILABLE", "GATEWAY_TIMEOUT"}
    )
    void ves_shouldMapErrorsTo503WhenBackwardsCompatibilityIsNone(HttpStatus httpStatus) {
        //given
        responseCompatibility = BACKWARDS_COMPATIBILITY_NONE;
        MessageRouterPublishResponse messageRouterPublishResponse = mock(MessageRouterPublishResponse.class);
        when(messageRouterPublishResponse.failReason()).thenReturn(httpStatus.toString());

        //when
        //then
        assertThrows(InternalException.class,()->getHttpStatus(messageRouterPublishResponse));
    }
}

/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2018 - 2019 Nokia. All rights reserved.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.configuration.AuthMethodType;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ApiAuthInterceptionTest {
    private static final String USERNAME = "Foo";
    private static final String PASSWORD = "Bar";
    private static final int HTTP_PORT = 8080;
    private static final int OUTSIDE_PORT = 30235;
    public static final String HEALTHCHECK_URL = "/healthcheck";

    @Mock
    private ApplicationSettings settings;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object obj;

    @Mock
    private PrintWriter writer;

    @InjectMocks
    private ApiAuthInterceptor sut;


    private HttpServletRequest createEmptyRequest() {
        return MockMvcRequestBuilders
            .post("")
            .buildRequest(null);
    }

    private HttpServletRequest createRequestWithPorts(int localPort, int serverPort, String urlTemplate) {
        MockHttpServletRequest healthcheckRequest = MockMvcRequestBuilders
            .get(urlTemplate)
            .buildRequest(null);
        healthcheckRequest.setLocalPort(localPort);
        healthcheckRequest.setServerPort(serverPort);
        return healthcheckRequest;
    }

    @Test
    public void shouldSucceedWhenAuthorizationIsDisabled() throws IOException {
        // given
        final HttpServletRequest request = createEmptyRequest();

        when(settings.authMethod()).thenReturn(AuthMethodType.NO_AUTH.value());

        // when
        final boolean isAuthorized = sut.preHandle(request, response, obj);

        // then
        assertTrue(isAuthorized);
    }

    @Test
    public void shouldSucceedForHealthcheckOnHealthcheckPort() throws IOException {
        // given
        final HttpServletRequest request = createRequestWithPorts(HTTP_PORT, HTTP_PORT, HEALTHCHECK_URL);

        when(settings.authMethod()).thenReturn(AuthMethodType.CERT_BASIC_AUTH.value());
        when(settings.httpPort()).thenReturn(HTTP_PORT);
        // when
        final boolean isAuthorized = sut.preHandle(request, response, obj);

        // then
        assertTrue(isAuthorized);
    }

    @Test
    public void shouldFailForHealthcheckOnHealthcheckPortFromOutsideCluster() throws IOException {
        // given
        final HttpServletRequest request = createRequestWithPorts(HTTP_PORT, OUTSIDE_PORT, HEALTHCHECK_URL);

        when(settings.authMethod()).thenReturn(AuthMethodType.CERT_BASIC_AUTH.value());
        when(settings.httpPort()).thenReturn(HTTP_PORT);
        when(response.getWriter()).thenReturn(writer);

        // when
        final boolean isAuthorized = sut.preHandle(request, response, obj);

        // then
        assertFalse(isAuthorized);
        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void shouldFailDueToNotPermittedOperationOnHealthcheckPort() throws IOException {
        // given
        final HttpServletRequest request = createRequestWithPorts(HTTP_PORT, HTTP_PORT, "/");

        when(settings.authMethod()).thenReturn(AuthMethodType.CERT_BASIC_AUTH.value());
        when(settings.httpPort()).thenReturn(HTTP_PORT);
        when(response.getWriter()).thenReturn(writer);

        // when
        final boolean isAuthorized = sut.preHandle(request, response, obj);

        // then
        assertFalse(isAuthorized);
        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
    }

}

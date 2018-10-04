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

import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Test;
import static org.junit.Assert.*;

import org.mockito.Mock;
import static org.mockito.Mockito.*;

import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.vavr.collection.Map;
import io.vavr.collection.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;

import org.onap.dcae.ApplicationSettings;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

public class ApiAuthInterceptionTest {
    final private static String USERNAME = "Foo";
    final private static String PASSWORD = "Bar";
    final private static Map<String,String> CREDENTIALS = HashMap.of(USERNAME, PASSWORD);

    @Mock
    final private Logger log = mock(Logger.class);

    @Mock
    final private ApplicationSettings settings = mock(ApplicationSettings.class);

    @Mock
    final private HttpServletResponse response = mock(HttpServletResponse.class);

    @Mock
    final private Object obj = mock(Object.class);

    @Mock
    final private PrintWriter writer = mock(PrintWriter.class);

    final private ApiAuthInterceptor sut = new ApiAuthInterceptor(settings, log);

    @Test
    public void shouldSucceedWhenAuthorizationIsDisabled() throws IOException
    {
        final HttpServletRequest request =
            MockMvcRequestBuilders
                .post("")
                .buildRequest(null);

        when(settings.authorizationEnabled()).thenReturn(false);

        assertTrue(sut.preHandle(request, response, obj));
    }

    @Test
    public void shouldFailDueToEmptyBasicAuthorizationHeader() throws IOException
    {
        final HttpServletRequest request =
            MockMvcRequestBuilders
                .post("")
                .buildRequest(null);

        when(settings.authorizationEnabled()).thenReturn(true);
        when(response.getWriter()).thenReturn(writer);

        assertFalse(sut.preHandle(request, response, obj));

        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
        verify(writer).write(ApiException.UNAUTHORIZED_USER.toJSON().toString());
    }

    @Test
    public void shouldFailDueToBasicAuthenticationUserMissingFromSettings() throws IOException
    {
        final HttpServletRequest request =
            SecurityMockMvcRequestPostProcessors
                .httpBasic(USERNAME, PASSWORD)
                .postProcessRequest(
                    MockMvcRequestBuilders
                        .post("")
                        .buildRequest(null));

        when(settings.authorizationEnabled()).thenReturn(true);
        when(response.getWriter()).thenReturn(writer);

        assertFalse(sut.preHandle(request, response, obj));

        verify(settings).validAuthorizationCredentials();
        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
        verify(writer).write(ApiException.UNAUTHORIZED_USER.toJSON().toString());
    }

    @Test
    public void shouldSucceed() throws IOException
    {
        final HttpServletRequest request =
            SecurityMockMvcRequestPostProcessors
                .httpBasic(USERNAME, PASSWORD)
                .postProcessRequest(
                        MockMvcRequestBuilders
                            .post("")
                            .buildRequest(null));



        when(settings.authorizationEnabled()).thenReturn(true);
        when(settings.validAuthorizationCredentials()).thenReturn(CREDENTIALS);
        when(response.getWriter()).thenReturn(writer);

        assertTrue(sut.preHandle(request, response, obj));
    }

    @Test
    public void shouldFailDueToInvalidBasicAuthorizationHeaderValue() throws IOException
    {
        final HttpServletRequest request =
            MockMvcRequestBuilders
                .post("")
                .header(HttpHeaders.AUTHORIZATION, "FooBar")
                .buildRequest(null);

        when(settings.authorizationEnabled()).thenReturn(true);
        when(settings.validAuthorizationCredentials()).thenReturn(CREDENTIALS);
        when(response.getWriter()).thenReturn(writer);

        assertFalse(sut.preHandle(request, response, obj));

        verify(response).setStatus(HttpStatus.BAD_REQUEST.value());
        verify(writer).write(ApiException.UNAUTHORIZED_USER.toJSON().toString());
    }
}

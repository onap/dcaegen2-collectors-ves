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

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ApiAuthInterceptionTest {
    private static final String USERNAME = "Foo";
    private static final String PASSWORD = "Bar";
    private static final Map<String, String> CREDENTIALS = HashMap.of(USERNAME, PASSWORD);

    @Mock
    private Logger log;

    @Mock
    private ApplicationSettings settings;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain obj;

    @Mock
    private PrintWriter writer;

    @InjectMocks
    private ApiAuthInterceptor sut;


    private HttpServletRequest createEmptyRequest() {
        return MockMvcRequestBuilders
                .post("")
                .buildRequest(null);
    }

    private HttpServletRequest createRequestWithAuthorizationHeader() {
        return SecurityMockMvcRequestPostProcessors
                .httpBasic(USERNAME, PASSWORD)
                .postProcessRequest(
                        MockMvcRequestBuilders
                                .post("")
                                .buildRequest(null));
    }

    @Test
    public void shouldSucceedWhenAuthorizationIsDisabled() throws IOException, ServletException {
        // given
        final HttpServletRequest request = createEmptyRequest();

        when(settings.authMethod()).thenReturn(AuthMethodType.NO_AUTH.value());

        // when
        sut.doFilter(request, response, obj);

        // then
        verify(obj, atLeastOnce()).doFilter(request, response);
    }

    @Test
    public void shouldFailDueToEmptyBasicAuthorizationHeader() throws IOException, ServletException {
        // given
        final HttpServletRequest request = createEmptyRequest();

        when(settings.authMethod()).thenReturn(AuthMethodType.BASIC_AUTH.value());
        when(response.getWriter()).thenReturn(writer);

        // when
        sut.doFilter(request, response, obj);

        // then
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(writer).write(ApiException.UNAUTHORIZED_USER.toJSON().toString());
    }

    @Test
    public void shouldFailDueToBasicAuthenticationUserMissingFromSettings()
        throws IOException, ServletException {
        // given
        final HttpServletRequest request = createRequestWithAuthorizationHeader();

        when(settings.authMethod()).thenReturn(AuthMethodType.BASIC_AUTH.value());
        when(response.getWriter()).thenReturn(writer);

        // when
        sut.doFilter(request, response, obj);

        // then
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(writer).write(ApiException.UNAUTHORIZED_USER.toJSON().toString());
    }

    @Test
    public void shouldSucceed() throws IOException, ServletException {
        // given
        final HttpServletRequest request = createRequestWithAuthorizationHeader();
        when(settings.authMethod()).thenReturn(AuthMethodType.BASIC_AUTH.value());
        when(settings.validAuthorizationCredentials()).thenReturn(
            HashMap.of(USERNAME, "$2a$10$BsZkEynNm/93wbAeeZuxJeu6IHRyQl4XReqDg2BtYOFDhUsz20.3G"));
        when(response.getWriter()).thenReturn(writer);

        // when
        sut.doFilter(request, response, obj);

        // then
        verify(obj, atLeastOnce()).doFilter(request, response);
    }

    @Test
    public void shouldFailDueToInvalidBasicAuthorizationHeaderValue()
        throws IOException, ServletException {
        // given
        final HttpServletRequest request =
                MockMvcRequestBuilders
                        .post("")
                        .header(HttpHeaders.AUTHORIZATION, "FooBar")
                        .buildRequest(null);

        when(settings.authMethod()).thenReturn(AuthMethodType.BASIC_AUTH.value());
        when(settings.validAuthorizationCredentials()).thenReturn(CREDENTIALS);
        when(response.getWriter()).thenReturn(writer);

        // when
        sut.doFilter(request, response, obj);

        //then
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(writer).write(ApiException.UNAUTHORIZED_USER.toJSON().toString());
    }
}

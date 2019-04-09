/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2018 Nokia. All rights reserved.
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.dcae;

import io.vavr.collection.HashMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.onap.dcae.common.configuration.AuthMethodType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.onap.dcae.TLSTest.HttpsConfiguration.USERNAME;
import static org.onap.dcae.TLSTest.HttpsConfiguration.PASSWORD;

public class TLSTest extends TLSTestBase {

    @Nested
    @Import(HttpConfiguration.class)
    class HttpTest extends TestClassBase {

        @Test
        public void shouldHttpRequestSucceed() {
            assertEquals(HttpStatus.OK, makeHttpRequest().getStatusCode());
        }

        @Test
        public void shouldHttpsRequestFail() {
            assertThrows(Exception.class, this::makeHttpsRequest);
        }
    }

    @Nested
    @Import(HttpsConfiguration.class)
    class HttpsTest extends TestClassBase {


        @Test
        public void shouldHttpsRequestWithoutBasicAuthFail() {
            assertThrows(Exception.class, this::makeHttpsRequest);
        }

        @Test
        public void shouldHttpsRequestWithBasicAuthSucceed() {
            assertEquals(HttpStatus.OK, makeHttpsRequestWithBasicAuth(USERNAME, PASSWORD).getStatusCode());
        }
    }

    @Nested
    @Import(HttpsConfigurationWithTLSAuthentication.class)
    class HttpsWithTLSAuthenticationTest extends TestClassBase {

        @Test
        public void shouldHttpsRequestWithoutCertificateFail() {
            assertThrows(Exception.class, this::makeHttpsRequest);
        }

        @Test
        public void shouldHttpsRequestWithCertificateSucceed() {
            assertEquals(HttpStatus.OK, makeHttpsRequestWithClientCert().getStatusCode());
        }
    }

    @Nested
    @Import(HttpsConfigurationWithTLSAuthenticationAndBasicAuth.class)
    class HttpsWithTLSAuthenticationAndBasicAuthTest extends TestClassBase {

        @Test
        public void shouldHttpsRequestWithoutBasicAuthSucceed() {
            assertEquals(HttpStatus.OK, makeHttpsRequestWithClientCert().getStatusCode());
        }

        @Test
        public void shouldHttpsRequestWithBasicAuthSucceed() {
            assertEquals(HttpStatus.OK, makeHttpsRequestWithClientCertAndBasicAuth(USERNAME, PASSWORD).getStatusCode());
        }
    }

    // ApplicationSettings configurations
    static class HttpConfiguration extends TLSTestBase.ConfigurationBase {
        @Override
        protected void configureSettings(ApplicationSettings settings) {
            when(settings.authMethod()).thenReturn(AuthMethodType.NO_AUTH.value());
        }
    }

    static class HttpsConfiguration extends TLSTestBase.ConfigurationBase {
        public static final String USERNAME = "TestUser";
        public static final String PASSWORD = "TestPassword";

        @Override
        protected void configureSettings(ApplicationSettings settings) {
            when(settings.keystoreFileLocation()).thenReturn(KEYSTORE.toString());
            when(settings.keystorePasswordFileLocation()).thenReturn(KEYSTORE_PASSWORD_FILE.toString());
            when(settings.authMethod()).thenReturn(AuthMethodType.BASIC_AUTH.value());
            when(settings.validAuthorizationCredentials()).thenReturn(HashMap.of(USERNAME, "$2a$10$51tDgG2VNLde5E173Ay/YO.Fq.aD.LR2Rp8pY3QAKriOSPswvGviy"));
        }
    }

    static class HttpsConfigurationWithTLSAuthentication extends HttpsConfiguration {
        @Override
        protected void configureSettings(ApplicationSettings settings) {
            super.configureSettings(settings);
            when(settings.authMethod()).thenReturn(AuthMethodType.CERT_ONLY.value());
            when(settings.truststoreFileLocation()).thenReturn(TRUSTSTORE.toString());
            when(settings.truststorePasswordFileLocation()).thenReturn(TRUSTSTORE_PASSWORD_FILE.toString());
            when(settings.certSubjectMatcher()).thenReturn(CERT_SUBJECT_MATCHER.toString());
        }
    }

    static class HttpsConfigurationWithTLSAuthenticationAndBasicAuth extends HttpsConfigurationWithTLSAuthentication {
        @Override
        protected void configureSettings(ApplicationSettings settings) {
            super.configureSettings(settings);
            when(settings.authMethod()).thenReturn(AuthMethodType.CERT_BASIC_AUTH.value());
        }
    }
}
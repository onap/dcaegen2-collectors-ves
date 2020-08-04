/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2018 Nokia. All rights reserved.
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2019 Nokia. All rights reserved.
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
import static org.onap.dcae.TLSTest.HttpsConfigurationWithTLSAuthenticationAndBasicAuth.USERNAME;
import static org.onap.dcae.TLSTest.HttpsConfigurationWithTLSAuthenticationAndBasicAuth.PASSWORD;

public class TLSTest extends TLSTestBase {

    private static final String MAPPING_FILE_LOCATION = "./etc/externalRepo/schema-map.json";
    private static final String SCHEMA_FILES_LOCATION = "./etc/externalRepo";
    private static final String STND_DEFINED_DATA_PATH = "/event/stndDefinedFields/data";
    private static final String SCHEMA_REF_PATH = "/event/stndDefinedFields/schemaReference";

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
            when(settings.httpPort()).thenReturn(1111);
            when(settings.getExternalSchemaMappingFileLocation()).thenReturn(MAPPING_FILE_LOCATION);
            when(settings.getExternalSchemaSchemasLocation()).thenReturn(SCHEMA_FILES_LOCATION);
            when(settings.getExternalSchemaSchemaRefPath()).thenReturn(SCHEMA_REF_PATH);
            when(settings.getExternalSchemaStndDefinedDataPath()).thenReturn(STND_DEFINED_DATA_PATH);

        }
    }

    static class HttpsConfigurationWithTLSAuthenticationAndBasicAuth extends TLSTestBase.ConfigurationBase {
        public static final String USERNAME = "TestUser";
        public static final String PASSWORD = "TestPassword";
        @Override
        protected void configureSettings(ApplicationSettings settings) {
            when(settings.keystoreFileLocation()).thenReturn(KEYSTORE.toString());
            when(settings.keystorePasswordFileLocation()).thenReturn(KEYSTORE_PASSWORD_FILE.toString());
            when(settings.validAuthorizationCredentials()).thenReturn(HashMap.of(USERNAME, "$2a$10$51tDgG2VNLde5E173Ay/YO.Fq.aD.LR2Rp8pY3QAKriOSPswvGviy"));
            when(settings.authMethod()).thenReturn(AuthMethodType.CERT_BASIC_AUTH.value());
            when(settings.truststoreFileLocation()).thenReturn(TRUSTSTORE.toString());
            when(settings.truststorePasswordFileLocation()).thenReturn(TRUSTSTORE_PASSWORD_FILE.toString());
            when(settings.certSubjectMatcher()).thenReturn(CERT_SUBJECT_MATCHER.toString());
            when(settings.httpPort()).thenReturn(1111);
            when(settings.getExternalSchemaMappingFileLocation()).thenReturn(MAPPING_FILE_LOCATION);
            when(settings.getExternalSchemaSchemasLocation()).thenReturn(SCHEMA_FILES_LOCATION);
            when(settings.getExternalSchemaSchemaRefPath()).thenReturn(SCHEMA_REF_PATH);
            when(settings.getExternalSchemaStndDefinedDataPath()).thenReturn(STND_DEFINED_DATA_PATH);
        }
    }
}
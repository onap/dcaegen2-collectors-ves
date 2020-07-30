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

import static org.onap.dcae.TestingUtilities.configureKeyStore;
import static org.onap.dcae.TestingUtilities.createRestTemplateWithSsl;
import static org.onap.dcae.TestingUtilities.readFile;
import static org.onap.dcae.TestingUtilities.rethrow;
import static org.onap.dcae.TestingUtilities.sslBuilderWithTrustStore;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.onap.dcae.common.EventSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

@Configuration
@ExtendWith(SpringExtension.class)
public class TLSTestBase {
    protected static final Path RESOURCES = Paths.get("src", "test", "resources");
    protected static final Path KEYSTORE = Paths.get(RESOURCES.toString(), "keystore");
    protected static final Path KEYSTORE_PASSWORD_FILE = Paths.get(RESOURCES.toString(), "passwordfile");
    protected static final Path TRUSTSTORE = Paths.get(RESOURCES.toString(), "truststore");
    protected static final Path TRUSTSTORE_PASSWORD_FILE = Paths.get(RESOURCES.toString(), "trustpasswordfile");
    protected static final Path CERT_SUBJECT_MATCHER = Paths.get(RESOURCES.toString(), "certSubjectMatcher");

    protected static abstract class ConfigurationBase {
        protected final ApplicationSettings settings = Mockito.mock(ApplicationSettings.class);

        @Bean
        @Primary
        public ApplicationSettings settings() {
            configureSettings(settings);
            return settings;
        }

        protected abstract void configureSettings(final ApplicationSettings settings);
    }

    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    protected abstract class TestClassBase {

        @MockBean
        @Qualifier("eventSender")
        protected EventSender eventSender;

        @LocalServerPort
        private int port;

        private final String keyStorePassword;
        private final String trustStorePassword;

        public TestClassBase() {
            keyStorePassword = readFile(KEYSTORE_PASSWORD_FILE);
            trustStorePassword = readFile(TRUSTSTORE_PASSWORD_FILE);
        }

        private String getURL(final String protocol, final String uri) {
            return protocol + "://localhost:" + port + uri;
        }

        private RestTemplate addBasicAuth(final RestTemplate template, final String username, final String password) {
            template.getInterceptors()
                    .add(new BasicAuthenticationInterceptor(username, password));

            return template;
        }

        public String createHttpURL(String uri) {
            return getURL("http", uri);
        }

        public String createHttpsURL(String uri) {
            return getURL("https", uri);
        }

        public RestTemplate createHttpRestTemplate() {
            return new RestTemplate();
        }

        public RestTemplate createHttpsRestTemplate() {
            return rethrow(() ->
                    createRestTemplateWithSsl(
                            sslBuilderWithTrustStore(KEYSTORE, keyStorePassword).build()
                    ));
        }

        public RestTemplate createHttpsRestTemplateWithKeyStore() {
            return rethrow(() ->
                    createRestTemplateWithSsl(
                            configureKeyStore(
                                    sslBuilderWithTrustStore(KEYSTORE, keyStorePassword),
                                    TRUSTSTORE,
                                    trustStorePassword
                            ).build())
            );
        }

        public ResponseEntity<String> makeHttpRequest() {
            return createHttpRestTemplate().getForEntity(createHttpURL("/"), String.class);
        }

        public ResponseEntity<String> makeHttpsRequest() {
            return createHttpsRestTemplate().getForEntity(createHttpsURL("/"), String.class);
        }

        public ResponseEntity<String> makeHttpsRequestWithClientCert() {
            return createHttpsRestTemplateWithKeyStore().getForEntity(createHttpsURL("/"), String.class);
        }

        public ResponseEntity<String> makeHttpsRequestWithClientCertAndBasicAuth(
                final String username,
                final String password) {
            return addBasicAuth(createHttpsRestTemplateWithKeyStore(), username, password)
                    .getForEntity(createHttpsURL("/"), String.class);
        }
    }
}

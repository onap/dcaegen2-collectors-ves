/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

package org.onap.dcae.common;

import org.onap.dcae.ApplicationException;
import org.springframework.boot.web.server.Ssl;

import java.nio.file.Path;

public class SSLContextCreator {
    private final String keyStorePassword;
    private final String certAlias;
    private final Path keyStoreFile;

    private Path trustStoreFile;
    private String trustStorePassword;
    private boolean hasTlsClientAuthentication = false;

    public static SSLContextCreator create(final Path keyStoreFile, final String certAlias, final String password) {
        return new SSLContextCreator(keyStoreFile, certAlias, password);
    }

    private SSLContextCreator(final Path keyStoreFile, final String certAlias, final String password) {
        this.certAlias = certAlias;
        this.keyStoreFile = keyStoreFile;
        this.keyStorePassword = password;
    }

    public SSLContextCreator withTlsClientAuthentication(final Path trustStoreFile, final String password) {
        hasTlsClientAuthentication = true;
        this.trustStoreFile = trustStoreFile;
        this.trustStorePassword = password;

        return this;
    }

    private void configureKeyStore(final Ssl ssl) {
        final String keyStore = keyStoreFile.toAbsolutePath().toString();

            ssl.setKeyStore(keyStore);
            ssl.setKeyPassword(keyStorePassword);
            ssl.setKeyAlias(certAlias);
    }

    private void configureTrustStore(final Ssl ssl) {
        final String trustStore = trustStoreFile.toAbsolutePath().toString();

        ssl.setTrustStore(trustStore);
        ssl.setTrustStorePassword(trustStorePassword);
        ssl.setClientAuth(Ssl.ClientAuth.NEED);
    }

    public Ssl build() {
        final Ssl ssl = new Ssl();
        ssl.setEnabled(true);

        configureKeyStore(ssl);

        if (hasTlsClientAuthentication) {
            configureTrustStore(ssl);
        }

        return ssl;
    }
}
/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2018 Nokia. All rights reserved.s
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

import org.onap.dcae.ApplicationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;

import static java.nio.file.Files.readAllBytes;

@Component
public class ServletConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    private static final Logger log = LoggerFactory.getLogger(ServletConfig.class);

    @Autowired
    private ApplicationSettings properties;

    @Override
    public void customize(ConfigurableServletWebServerFactory container) {
        final boolean hasClientTslAuthorization = properties.clientTlsAuthorizationEnabled();
        if (hasClientTslAuthorization || properties.authorizationEnabled()) {
            container.setSsl(createSSL(hasClientTslAuthorization));
            container.setPort(properties.httpsPort());
        } else {
            container.setPort(properties.httpPort());
        }
    }

    private Ssl createSSL(final boolean hasClientTslAuthorization) {
        log.info("Enabling SSL");
        Ssl ssl = new Ssl();
        ssl.setEnabled(true);
        String keyStore = Paths.get(properties.keystoreFileLocation()).toAbsolutePath().toString();
        log.info("Using keyStore path: " + keyStore);
        ssl.setKeyStore(keyStore);
        String keyPasswordFileLocation = Paths.get(properties.keystorePasswordFileLocation()).toAbsolutePath().toString();
        log.info("Using keyStore password from: " + keyPasswordFileLocation);
        ssl.setKeyPassword(getKeyStorePassword(keyPasswordFileLocation));
        ssl.setKeyAlias(properties.keystoreAlias());

        if (hasClientTslAuthorization) {
            log.info("Enabling TLS client authorization");

            final String trustStore =
                    Paths.get(properties.truststoreFileLocation())
                            .toAbsolutePath()
                            .toString();
            log.info("Using trustStore path: " + trustStore);

            ssl.setTrustStore(trustStore);

            final String trustPasswordFileLocation =
                    Paths.get(properties.truststorePasswordFileLocation())
                            .toAbsolutePath()
                            .toString();
            log.info("Using trustStore password from: " + trustPasswordFileLocation);
            ssl.setTrustStorePassword(getKeyStorePassword(trustPasswordFileLocation));
            ssl.setClientAuth(Ssl.ClientAuth.NEED);
        }

        return ssl;
    }

    private String getKeyStorePassword(String location) {
        try {
            return new String(readAllBytes(Paths.get(location)));
        } catch (IOException e) {
            log.error("Could not read keystore password from: '" + location + "'.", e);
            throw new RuntimeException(e);
        }
    }
}
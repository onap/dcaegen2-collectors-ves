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
import org.onap.dcae.commonFunction.SSLContextCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
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
            log.info("Enabling SSL");

            final Path keyStore = toAbsPath(properties.keystoreFileLocation());
            log.info("Using keyStore path: " + keyStore);

            final Path keyStorePasswordLocation = toAbsPath(properties.keystorePasswordFileLocation());
            final String keyStorePassword = getKeyStorePassword(keyStorePasswordLocation);
            log.info("Using keyStore password from: " + keyStorePasswordLocation);

            final String alias = properties.keystoreAlias();
            final SSLContextCreator sslContextCreator = SSLContextCreator.create(keyStore, alias, keyStorePassword);

            if (hasClientTslAuthorization) {
                log.info("Enabling TLS client authorization");

                final Path trustStore = toAbsPath(properties.truststoreFileLocation());
                log.info("Using trustStore path: " + trustStore);

                final Path trustPasswordFileLocation = toAbsPath(properties.truststorePasswordFileLocation());
                final String trustStorePassword = getKeyStorePassword(trustPasswordFileLocation);
                log.info("Using trustStore password from: " + trustPasswordFileLocation);

                sslContextCreator.withTlsClientAuthorization(trustStore, trustStorePassword);
            }

            container.setSsl(sslContextCreator.build());
            container.setPort(properties.httpsPort());
        } else {
            container.setPort(properties.httpPort());
        }
    }

    private Path toAbsPath(final String path) {
        return Paths.get(path).toAbsolutePath();
    }

    private String getKeyStorePassword(final Path location) {
        try {
            return new String(readAllBytes(location));
        } catch (IOException e) {
            log.error("Could not read keystore password from: '" + location + "'.", e);
            throw new RuntimeException(e);
        }
    }
}
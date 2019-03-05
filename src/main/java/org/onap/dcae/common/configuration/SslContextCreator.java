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

package org.onap.dcae.common.configuration;

import static java.nio.file.Files.readAllBytes;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import org.onap.dcae.ApplicationException;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.SSLContextCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.Ssl;

public class SslContextCreator {

  private static final Logger log = LoggerFactory.getLogger(CertAuth.class);
  private final ApplicationSettings properties;

  public SslContextCreator(ApplicationSettings properties) {
    this.properties = properties;
  }

  public Ssl httpsContextWithTlsAuthentication() {
    final SSLContextCreator sslContextCreator = simpleHttpsContextBuilder();

    log.info("Enabling TLS client authorization");

    final Path trustStore = toAbsolutePath(properties.truststoreFileLocation());
    log.info("Using trustStore path: " + trustStore);

    final Path trustPasswordFileLocation = toAbsolutePath(properties.truststorePasswordFileLocation());
    final String trustStorePassword = getKeyStorePassword(trustPasswordFileLocation);
    log.info("Using trustStore password from: " + trustPasswordFileLocation);

    return sslContextCreator.withTlsClientAuthentication(trustStore, trustStorePassword).build();
  }

  public Ssl simpleHttpsContext(){
    return simpleHttpsContextBuilder().build();
  }

  private SSLContextCreator simpleHttpsContextBuilder() {
    log.info("Enabling SSL");

    final Path keyStorePath = toAbsolutePath(properties.keystoreFileLocation());
    log.info("Using keyStore path: " + keyStorePath);

    final Path keyStorePasswordLocation = toAbsolutePath(properties.keystorePasswordFileLocation());
    final String keyStorePassword = getKeyStorePassword(keyStorePasswordLocation);
    log.info("Using keyStore password from: " + keyStorePasswordLocation);
    return SSLContextCreator.create(keyStorePath, getKeyStoreAlias(keyStorePath, keyStorePassword), keyStorePassword);
  }

  private String getKeyStoreAlias(Path keyStorePath, String keyStorePassword) {
    KeyStore keyStore = getKeyStore();
    try(InputStream keyStoreData = new FileInputStream(keyStorePath.toString())){
      keyStore.load(keyStoreData, keyStorePassword.toCharArray());
      String alias = keyStore.aliases().nextElement();
      log.info("Actual key store alias is: " + alias);
      return alias;
    } catch (IOException | GeneralSecurityException ex) {
      log.error("Cannot load Key Store alias cause: " + ex);
      throw new ApplicationException(ex);
    }
  }

  private KeyStore getKeyStore() {
    try {
      return KeyStore.getInstance(KeyStore.getDefaultType());
    } catch (KeyStoreException ex) {
      log.error("Cannot create Key Store instance cause: " + ex);
      throw new ApplicationException(ex);
    }
  }

  private Path toAbsolutePath(final String path) {
    return Paths.get(path).toAbsolutePath();
  }

  private String getKeyStorePassword(final Path location) {
    try {
      return new String(readAllBytes(location));
    } catch (IOException e) {
      log.error("Could not read keystore password from: '" + location + "'.", e);
      throw new ApplicationException(e);
    }
  }
}

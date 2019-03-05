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

import org.onap.dcae.ApplicationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;

public class CertAuth implements AuthMethod {

  private static final Logger log = LoggerFactory.getLogger(CertAuth.class);
  private final ConfigurableServletWebServerFactory container;
  private final ApplicationSettings properties;

  public CertAuth(ConfigurableServletWebServerFactory container, ApplicationSettings properties) {
    this.container = container;
    this.properties = properties;
  }

  @Override
  public void configure() {
    SslContextCreator sslContextCreator = new SslContextCreator(properties);
    container.setSsl(sslContextCreator.httpsContextWithTlsAuthentication(ClientAuth.NEED));
    container.setPort(properties.httpsPort());
    log.info(String.format("Application work in %s mode on %s port.",
        properties.authMethod(), properties.httpsPort()));
  }
}

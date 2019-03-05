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
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;

public class NoAuth implements AuthMethod {

  private static final Logger log = LoggerFactory.getLogger(NoAuth.class);

  private ConfigurableServletWebServerFactory container;
  private ApplicationSettings properties;

  public NoAuth(ConfigurableServletWebServerFactory container, ApplicationSettings properties) {
    this.container = container;
    this.properties = properties;
  }

  @Override
  public void configure() {
    if (validateAuthMethod()){
      container.setPort(properties.httpsPort());
      log.info("Application work on https port: " + properties.httpsPort());
    }
    else {
      container.setPort(properties.httpPort());
      log.info("Application work on http port " + properties.httpPort());
    }
  }

  private boolean validateAuthMethod() {
    return properties.authMethod().equalsIgnoreCase(AuthMethodType.BASIC_AUTH)
        || properties.authMethod().equalsIgnoreCase(AuthMethodType.CERT_ONLY)
        || properties.authMethod().equalsIgnoreCase(AuthMethodType.CERT_BASIC_AUTH);
  }
}

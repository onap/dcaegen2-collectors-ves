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
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;

public class NoAuth implements AuthMethod {

  private ConfigurableServletWebServerFactory container;
  private ApplicationSettings properties;

  public NoAuth(ConfigurableServletWebServerFactory container, ApplicationSettings properties) {
    this.container = container;
    this.properties = properties;
  }

  @Override
  public void configure() {
    if (properties.authorizationEnabled()){
      container.setPort(properties.httpsPort());
    }
    else {
      container.setPort(properties.httpPort());
    }
  }
}

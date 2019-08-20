/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2019 Nokia. All rights reserved.s
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

import org.apache.catalina.connector.Connector;
import org.onap.dcae.ApplicationSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class TomcatHttpConfig {

  private static final int PORT = 8080;

  @Autowired
  private ApplicationSettings applicationSettings;

  @Bean
  public ServletWebServerFactory servletContainer() {
    TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();

    if(!(applicationSettings.authMethod().equalsIgnoreCase(AuthMethodType.NO_AUTH.value())) && tomcat.getPort() != PORT){
      tomcat.addAdditionalTomcatConnectors(getHttpConnector());
    }
    return tomcat;
  }

  private Connector getHttpConnector() {
    Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
    connector.setScheme("http");
    connector.setPort(PORT);
    connector.setSecure(false);
    return connector;
  }
}

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

import org.onap.dcae.ApplicationException;
import org.onap.dcae.ApplicationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;

@Configuration
@Order(1)
@EnableWebSecurity
public class CertBasicAuth extends WebSecurityConfigurerAdapter implements AuthMethod{

  private static final Logger log = LoggerFactory.getLogger(CertAuth.class);
  private final ConfigurableServletWebServerFactory container;
  private final ApplicationSettings properties;

  public CertBasicAuth(ConfigurableServletWebServerFactory container, ApplicationSettings properties) {
    this.container = container;
    this.properties = properties;
  }

  @Override
  public void configure(WebSecurity web) {
    web.ignoring().anyRequest();
  }

  @Override
  protected void configure(HttpSecurity http) {
    try {
      http.authorizeRequests()
          .anyRequest().authenticated().and()
          .addFilterBefore(new CustomFilter(), FilterSecurityInterceptor.class);

    } catch (Exception ex) {
      throw new ApplicationException(ex);
    }
  }

  @Override
  public void configure() {
    SslContextCreator sslContextCreator = new SslContextCreator(properties);
    container.setPort(properties.httpsPort());
    container.setSsl(sslContextCreator.httpsContextWithTlsAuthentication(ClientAuth.WANT));
    log.info(String.format("Application work in %s mode on %s port.",
        properties.authMethod(), properties.httpsPort()));
  }
}


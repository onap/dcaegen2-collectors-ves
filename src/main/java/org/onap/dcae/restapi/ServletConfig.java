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

import java.util.HashMap;
import java.util.Map;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.configuration.AuthMethod;
import org.onap.dcae.common.configuration.BasicAuth;
import org.onap.dcae.common.configuration.CertBasicAuth;
import org.onap.dcae.common.configuration.CertOnly;
import org.onap.dcae.common.configuration.NoAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

@Component
public class ServletConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Autowired
    private ApplicationSettings properties;

    @Override
    public void customize(ConfigurableServletWebServerFactory container) {

        Map<String, AuthMethod> auth = new HashMap<>();

        auth.put("certOnly",new CertOnly(container, properties));
        auth.put("basicAuth",new BasicAuth(container, properties));
        auth.put("certBasicAuth",new CertBasicAuth(container, properties));
        auth.put("noAuth",new NoAuth(container, properties));

        auth.get(properties.authMethod()).configure();
    }
}
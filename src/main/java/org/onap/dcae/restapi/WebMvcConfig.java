/*
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2020 Nokia. All rights reserved.
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

import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

public class WebMvcConfig extends WebMvcConfigurationSupport {

    public static final String SWAGGER_PATH_PATTERN = "swagger-ui.html";
    public static final String SWAGGER_CLASSPATH_RESOURCES = "classpath:/META-INF/resources/";
    public static final String WEBJARS_PATH_PATTERN = "/webjars/**";
    public static final String WEBJARS_CLASSPATH_RESOURCES = "classpath:/META-INF/resources/webjars/";
    public static final String TEMPLATES_PATTERN = "**";
    public static final String TEMPLATES_CLASSPATH_RESOURCES = "classpath:/templates/";
    public static final String PREFIX = "/";
    public static final String SUFFIX = ".html";

    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler(SWAGGER_PATH_PATTERN)
            .addResourceLocations(SWAGGER_CLASSPATH_RESOURCES);

        registry
            .addResourceHandler(WEBJARS_PATH_PATTERN)
            .addResourceLocations(WEBJARS_CLASSPATH_RESOURCES);

        registry
            .addResourceHandler(TEMPLATES_PATTERN)
            .addResourceLocations(TEMPLATES_CLASSPATH_RESOURCES);
    }

    @Bean
    public InternalResourceViewResolver jspViewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix(PREFIX);
        resolver.setSuffix(SUFFIX);
        return resolver;
    }
}

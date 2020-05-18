/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
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


import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

import java.lang.reflect.Field;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.dcae.restapi.WebMvcConfig.PREFIX;
import static org.onap.dcae.restapi.WebMvcConfig.SUFFIX;
import static org.onap.dcae.restapi.WebMvcConfig.SWAGGER_CLASSPATH_RESOURCES;
import static org.onap.dcae.restapi.WebMvcConfig.SWAGGER_PATH_PATTERN;
import static org.onap.dcae.restapi.WebMvcConfig.TEMPLATES_CLASSPATH_RESOURCES;
import static org.onap.dcae.restapi.WebMvcConfig.TEMPLATES_PATTERN;
import static org.onap.dcae.restapi.WebMvcConfig.WEBJARS_CLASSPATH_RESOURCES;
import static org.onap.dcae.restapi.WebMvcConfig.WEBJARS_PATH_PATTERN;

@RunWith(MockitoJUnitRunner.class)
public class WebMvcConfigTest {

    @Mock
    private ResourceHandlerRegistry resourceHandlerRegistry;
    @Mock
    private ResourceHandlerRegistration resourceHandlerRegistration;
    private WebMvcConfig webMvcConfig = new WebMvcConfig();

    @Test
    public void shouldConfigureResourceHandlers() {
        // given
        when(resourceHandlerRegistry.addResourceHandler(Mockito.anyString())).thenReturn(resourceHandlerRegistration);

        // when
        webMvcConfig.addResourceHandlers(resourceHandlerRegistry);

        // then
        verifyThatResourceWasRegistered(SWAGGER_PATH_PATTERN, SWAGGER_CLASSPATH_RESOURCES);
        verifyThatResourceWasRegistered(WEBJARS_PATH_PATTERN, WEBJARS_CLASSPATH_RESOURCES);
        verifyThatResourceWasRegistered(TEMPLATES_PATTERN, TEMPLATES_CLASSPATH_RESOURCES);
    }


    @Test
    public void shouldConfigureJspViewResolverToHandleHtmlRequests() throws NoSuchFieldException, IllegalAccessException {
        // when
        final InternalResourceViewResolver internalResourceViewResolver = webMvcConfig.jspViewResolver();

        // then
        verifyThatFieldWasSet(internalResourceViewResolver, "prefix", PREFIX);
        verifyThatFieldWasSet(internalResourceViewResolver, "suffix", SUFFIX);

    }

    private void verifyThatResourceWasRegistered(String swaggerPathPattern, String swaggerClasspathResources) {
        verify(resourceHandlerRegistry).addResourceHandler(swaggerPathPattern);
        verify(resourceHandlerRegistration).addResourceLocations(swaggerClasspathResources);
    }

    private void verifyThatFieldWasSet(InternalResourceViewResolver internalResourceViewResolver, String prefix, String prefix2) throws NoSuchFieldException, IllegalAccessException {
        String fieldValue = getValueFromPrivateField(internalResourceViewResolver, prefix);
        Assertions.assertThat(fieldValue).isEqualTo(prefix2);
    }

    private String getValueFromPrivateField(InternalResourceViewResolver internalResourceViewResolver, String privateFieldName) throws NoSuchFieldException, IllegalAccessException {
        final Field privatePrefixField = UrlBasedViewResolver.class.
                getDeclaredField(privateFieldName);

        privatePrefixField.setAccessible(true);

        return (String) privatePrefixField.get(internalResourceViewResolver);
    }

}

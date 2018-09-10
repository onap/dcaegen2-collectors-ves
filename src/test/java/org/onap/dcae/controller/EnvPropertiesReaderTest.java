/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2018 Nokia. All rights reserved.
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

package org.onap.dcae.controller;

import static io.vavr.API.Map;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onap.dcae.controller.EnvPropertiesReader.readEnvProps;

import io.vavr.collection.Map;
import org.junit.Test;


public class EnvPropertiesReaderTest {

    @Test
    public void shouldReturnEmptyOnMissingConsulHost() {
        Map<String, String> envs = Map(
            "CONFIG_BINDING_SERVICE", "doesNotMatter",
            "HOSTNAME", "doesNotMatter");
        assertTrue(readEnvProps(envs).isEmpty());
    }

    @Test
    public void shouldReturnEmptyOnMissingCBSName() {
        Map<String, String> envs = Map(
            "CONSUL_HOST", "doesNotMatter",
            "HOSTNAME", "doesNotMatter");
        assertTrue(readEnvProps(envs).isEmpty());
    }

    @Test
    public void shouldReturnEmptyOnMissingVESAppName() {
        Map<String, String> envs = Map(
            "CONSUL_HOST", "doesNotMatter",
            "CONFIG_BINDING_SERVICE", "doesNotMatter");
        assertTrue(readEnvProps(envs).isEmpty());
    }

    @Test
    public void shouldReturnSomeOfAllProperties() {
        Map<String, String> envs = Map(
            "CONSUL_HOST", "doesNotMatter",
            "HOSTNAME", "doesNotMatter",
            "CONFIG_BINDING_SERVICE", "doesNotMatter");
        assertFalse(readEnvProps(envs).isEmpty());
    }

}


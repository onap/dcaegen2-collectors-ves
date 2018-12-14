/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

package org.onap.dcae;

import io.vavr.collection.Map;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CLIUtilsTest {

    @Test
    public void shouldConvertArgsToPropertiesMap() {
        // given
        String[] args = {"-withOutValue", "-collector.service.port", "8000", "-collector.service.secure.port", "8443"};

        //when
        Map<String, String> properties = CLIUtils.processCmdLine(args);

        //then
        assertThat(properties.get("collector.service.port").get()).isEqualTo("8000");
        assertThat(properties.get("collector.service.secure.port").get()).isEqualTo("8443");
        assertThat(properties.get("withOutValue").get()).isEqualTo("");
    }

    @Test
    public void shouldReturnEmptyMapIfThereIsNoArgs() {
        //given
        String[] args = {};

        //when
        Map<String, String> properties = CLIUtils.processCmdLine(args);

        //then
        assertThat(properties).isEmpty();
    }

}
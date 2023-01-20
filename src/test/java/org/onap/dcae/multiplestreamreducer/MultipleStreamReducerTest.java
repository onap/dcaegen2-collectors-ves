/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2021 Nokia. All rights reserved.
 * Copyright (C) 2023 AT&T Intellectual Property. All rights reserved.
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
package org.onap.dcae.multiplestreamreducer;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


class MultipleStreamReducerTest {

    private final MultipleStreamReducer multipleStreamReducer = new MultipleStreamReducer();
    private final Map<String, String[]> domainToStreams = HashMap.of(
            "fault", new String[]{"ves-fault", "stream1", "stream2"},
            "log", new String[]{"ves-syslog", "stream3", "stream4", "stream5"},
            "test", new String[]{"stream6"}
    );

    @Test
    void shouldReduceStreamsToTheFirstOne() {
        //given
        Map<String, String> expected = HashMap.of(
                "fault", "ves-fault",
                "log", "ves-syslog",
                "test", "stream6"
        );

        //when
        final Map<String, String> domainToStreamsAfterReduce = multipleStreamReducer.reduce(domainToStreams);

        //then
        assertEquals(expected, domainToStreamsAfterReduce);
    }

    @Test
    void shouldReturnInfoAboutDomainToStreamsConfig() {
        String newLine = System.getProperty("line.separator");
        //given
        final Map<String, String> domainToStreamsAfterReduce = multipleStreamReducer.reduce(domainToStreams);
        String expectedRedundantStreamsInfo =
                "Domain: fault has active stream: ves-fault" + newLine + 
                "Domain: log has active stream: ves-syslog" + newLine +
                "Domain: test has active stream: stream6" + newLine;

        //when
        final String domainToStreamsConfigInfo = multipleStreamReducer.getDomainToStreamsInfo(domainToStreamsAfterReduce);

        //then
        assertEquals(expectedRedundantStreamsInfo, domainToStreamsConfigInfo);
    }

}

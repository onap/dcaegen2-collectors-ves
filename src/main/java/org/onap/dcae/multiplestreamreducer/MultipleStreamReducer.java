/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2021 Nokia. All rights reserved.
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

import io.vavr.Tuple2;
import io.vavr.collection.Map;

public class MultipleStreamReducer {

    /**
     * Converts configuration from: "one domain many streams"
     * to: "one domain one stream"
     *
     * @param map domain to streams configuration
     * @return configuration - one domain one stream
     */
    public Map<String, String> reduce(Map<String, String[]> map) {
        return map.toStream()
                .toMap(Tuple2::_1, v -> v._2[0]);
    }

    /**
     * Information about the current match: domain to stream
     *
     * @param domainToStreamConfig domain to stream configuration
     * @return current domain to stream information
     */
    public String getDomainToStreamsInfo(Map<String, String> domainToStreamConfig) {
        return domainToStreamConfig.map(v -> "Domain: " +
                v._1 + " has active stream: " + v._2 + System.lineSeparator())
                .reduce((a, b) -> a + b);
    }
}

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

import static io.vavr.API.Try;
import static io.vavr.API.Tuple;
import static org.onap.dcae.common.event.publishing.VavrUtils.f;
import static org.onap.dcae.controller.Conversions.toList;

import io.vavr.collection.Map;
import io.vavr.control.Option;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

interface ConfigParsing {

    Logger log = LoggerFactory.getLogger(ConfigParsing.class);

    static Option<JSONObject> getDMaaPConfig(JSONObject configuration) {
        log.info(f("Getting DMaaP configuration from app configuration: '%s'", configuration));
        return toList(configuration.toMap().entrySet().iterator())
            .filter(t -> t.getKey().startsWith("streams_publishes"))
            .headOption()
            .flatMap(e -> Try(() -> configuration.getJSONObject(e.getKey())).toOption())
            .onEmpty(() -> log.warn(f("App configuration '%s' is missing DMaaP configuration ('streams_publishes' key) "
                + "or DMaaP configuration is not a valid json document", configuration)))
            .peek(dMaaPConf -> log.info(f("Found following DMaaP configuration: '%s'", dMaaPConf)));
    }

    static Map<String, String> getProperties(JSONObject configuration) {
        log.info(f("Getting properties configuration from app configuration: '%s'", configuration));
        Map<String, String> confEntries = toList(configuration.toMap().entrySet().iterator())
            .toMap(e -> Tuple(e.getKey(), String.valueOf(e.getValue())))
            .filterKeys(e -> !e.startsWith("streams_publishes"));
        log.info(f("Found following app properties: '%s'", confEntries));
        return confEntries;
    }

}

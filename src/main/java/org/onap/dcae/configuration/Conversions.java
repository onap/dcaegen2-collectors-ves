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
package org.onap.dcae.configuration;

import static org.onap.dcae.common.publishing.VavrUtils.enhanceError;

import io.vavr.API;
import io.vavr.collection.List;
import io.vavr.control.Try;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import org.json.JSONObject;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
interface Conversions {

    static Try<JSONObject> toJson(String strBody) {
        return API.Try(() -> new JSONObject(strBody))
            .mapFailure(enhanceError("Value '%s' is not a valid JSON document", strBody));
    }

    static <T> List<T> toList(Iterator<T> iterator) {
        return List
            .ofAll(StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false));
    }
}
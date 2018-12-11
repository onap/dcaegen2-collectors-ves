/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
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
package org.onap.dcae.common.publishing;

import io.vavr.collection.Map;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
public interface EventPublisher {

    static EventPublisher createPublisher(Logger outputLogger, Map<String, PublisherConfig> dMaaPConfig) {
        return new DMaaPEventPublisher(new DMaaPPublishersCache(dMaaPConfig), outputLogger);
    }

    void sendEvent(JSONObject event, String domain);

    void reconfigure(Map<String, PublisherConfig> dMaaPConfig);
}

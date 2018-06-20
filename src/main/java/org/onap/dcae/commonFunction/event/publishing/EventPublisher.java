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
package org.onap.dcae.commonFunction.event.publishing;

import static org.onap.dcae.commonFunction.event.publishing.DMaaPConfigurationParser.parseDMaaPConfig;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
public interface EventPublisher {

    static AtomicReference<EventPublisher> createPublisher(Logger outputLogger, Path cambriaConfigFile) {
        return new AtomicReference<>(
            new CambriaEventPublisher(CambriaPublishersCache.create(parseDMaaPConfig(cambriaConfigFile)), outputLogger)
        );
    }

    void sendEvent(JSONObject event, String streamID);
}

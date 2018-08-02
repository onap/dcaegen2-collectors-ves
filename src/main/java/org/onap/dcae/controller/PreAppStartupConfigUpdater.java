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
package org.onap.dcae.controller;

import io.vavr.collection.Map;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import org.onap.dcae.commonFunction.event.publishing.PublisherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * On the first application launch, the configuration update thread that application spawns, has no chance to run yet
 * and prepare initial application configuration. In this case, it needs to be fetched from outside of the application,
 * so this is run from the .sh script.
 * Later on, once application is already started it will take care of the configuration update itself
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
public class PreAppStartupConfigUpdater {
    private final static Logger log = LoggerFactory.getLogger(PreAppStartupConfigUpdater.class);

    private static final Path DEFAULT_CONFIGURATION_FILE_PATH = Paths.get("etc/collector.properties");
    private static final Path DEFAULT_DMAAP_FILE_PATH = Paths.get("etc/DmaapConfig.json");
    private static final Consumer<Map<String, PublisherConfig>> NO_OP_CONSUMER = c -> { };

    public static void main(String[] args) {
        log.info("Running initial configuration update, before the application gets started.");
        ConfigLoader.create(NO_OP_CONSUMER, DEFAULT_DMAAP_FILE_PATH, DEFAULT_CONFIGURATION_FILE_PATH)
            .updateConfig();
    }
}

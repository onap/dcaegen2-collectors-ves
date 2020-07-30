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

import io.vavr.collection.Map;
import io.vavr.control.Option;
import org.json.JSONObject;
import org.onap.dcae.configuration.cbs.CbsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private final ConfigFilesFacade configFilesFacade;
    private final CbsClient cbsClient;
    private final Runnable applicationRestarter;
    private boolean isApplicationRestartNeeded;

    ConfigLoader(ConfigFilesFacade configFilesFacade, CbsClient cbsClient, Runnable applicationRestarter) {
        this.configFilesFacade = configFilesFacade;
        this.cbsClient = cbsClient;
        this.applicationRestarter = applicationRestarter;
        this.isApplicationRestartNeeded = false;
    }

    public synchronized void updateConfig() {
        Option<JSONObject> appConfig = cbsClient.getAppConfig();
        appConfig.peek(this::handleUpdate).onEmpty(logSkipMessage());
    }

    private Runnable logSkipMessage() {
        return () -> log.info("Skipping dynamic configuration");
    }

    private void handleUpdate(JSONObject appConfig) {
        updatePropertiesIfChanged(appConfig);
        updateDmaapConfigIfChanged(appConfig);
        restartApplicationIfNeeded();
    }

    private void updatePropertiesIfChanged(JSONObject appConfig) {
        Map<String, String> newProperties = ConfigParsing.getProperties(appConfig);
        Map<String, String> oldProperties = configFilesFacade.readCollectorProperties().get();

        if (!areCommonPropertiesSame(oldProperties, newProperties)) {
            configFilesFacade.writeProperties(newProperties);
            isApplicationRestartNeeded = true;
        }
    }

    private boolean areCommonPropertiesSame(Map<String, String> oldProperties, Map<String, String> newProperties) {
        Map<String, String> filteredOldProperties = filterIntersectingKeys(oldProperties, newProperties);
        return filteredOldProperties.equals(newProperties);
    }

    private Map<String, String> filterIntersectingKeys(Map<String, String> primaryProperties,
        Map<String, String> otherProperties) {
        return primaryProperties.filterKeys(key -> containsKey(key, otherProperties));
    }

    private boolean containsKey(String key, Map<String, String> properties) {
        return properties.keySet().contains(key);
    }

    private void updateDmaapConfigIfChanged(JSONObject appConfig) {
        JSONObject oldDmaapConfig = configFilesFacade.readDMaaPConfiguration().get();
        JSONObject newDmaapConfig = ConfigParsing.getDMaaPConfig(appConfig).get();

        if (!oldDmaapConfig.similar(newDmaapConfig)) {
            configFilesFacade.writeDMaaPConfiguration(newDmaapConfig);
            isApplicationRestartNeeded = true;
        }
    }

    private void restartApplicationIfNeeded() {
        if (isApplicationRestartNeeded) {
            applicationRestarter.run();
            isApplicationRestartNeeded = false;
        }
    }
}
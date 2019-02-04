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

import static org.onap.dcae.common.publishing.DMaaPConfigurationParser.parseToDomainMapping;
import static org.onap.dcae.controller.ConfigParsing.getDMaaPConfig;
import static org.onap.dcae.controller.ConfigParsing.getProperties;
import static org.onap.dcae.controller.EnvPropertiesReader.readEnvProps;

import io.vavr.Function0;
import io.vavr.Function1;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.json.JSONObject;
import org.onap.dcae.AliasConfig;
import org.onap.dcae.VesApplication;
import org.onap.dcae.common.publishing.PublisherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigLoader {

    private static final String SKIP_MSG = "Skipping dynamic configuration update";
    private static Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private final Consumer<Map<String, PublisherConfig>> eventPublisherReconfigurer;
    private final ConfigFilesFacade configFilesFacade;
    private final Function1<EnvProps, Try<JSONObject>> configurationSource;
    private final Function0<Map<String, String>> envVariablesSupplier;
    private AliasConfig aliasConfig;

    ConfigLoader(Consumer<Map<String, PublisherConfig>> eventPublisherReconfigurer,
                ConfigFilesFacade configFilesFacade,
                Function1<EnvProps, Try<JSONObject>> configurationSource,
                Function0<Map<String, String>> envVariablesSupplier, AliasConfig aliasConfig) {
        this.eventPublisherReconfigurer = eventPublisherReconfigurer;
        this.configFilesFacade = configFilesFacade;
        this.configurationSource = configurationSource;
        this.envVariablesSupplier = envVariablesSupplier;
        this.aliasConfig = aliasConfig;
    }

    public static ConfigLoader create(
        Consumer<Map<String, PublisherConfig>> eventPublisherReconfigurer,
        Path dMaaPConfigFile, Path propertiesConfigFile, AliasConfig aliasConfig) {
        return new ConfigLoader(eventPublisherReconfigurer,
            new ConfigFilesFacade(dMaaPConfigFile, propertiesConfigFile),
            ConfigSource::getAppConfig,
            () -> HashMap.ofAll(System.getenv()),aliasConfig);
    }

    public void updateConfig() {
        log.info("Trying to dynamically update config from Config Binding Service");
        Map<String, String> environmentVariables = envVariablesSupplier.get();
        readEnvProps(environmentVariables).onEmpty(() -> log.warn(SKIP_MSG)).forEach(this::updateConfig);
    }

    private void updateConfig(EnvProps props) {
        configurationSource.apply(props)
            .onFailure(logSkip())
            .onSuccess(newConf -> {
                    updateConfigurationProperties(newConf);
                    updateDMaaPProperties(newConf);
                }
            );
    }

    private void updateDMaaPProperties(JSONObject newConf) {
        configFilesFacade.readDMaaPConfiguration()
            .onFailure(logSkip())
            .onSuccess(oldDMaaPConf -> getDMaaPConfig(newConf)
                .onEmpty(() -> log.warn(SKIP_MSG))
                .forEach(newDMaaPConf -> compareAndOverwriteDMaaPConfig(oldDMaaPConf, newDMaaPConf)));
    }


    private void updateConfigurationProperties(JSONObject newConf) {
        configFilesFacade.readCollectorProperties()
            .onFailure(logSkip())
            .onSuccess(oldProps -> compareAndOverwritePropertiesConfig(newConf, oldProps));
    }

    private void compareAndOverwritePropertiesConfig(JSONObject newConf,
        Map<String, String> oldProps) {
        Map<String, String> newProperties = getProperties(newConf);
        if (!oldProps.equals(newProperties)) {
            configFilesFacade.writeProperties(newProperties)
                .onSuccess(__ -> {
                    log.info("New properties configuration written to file");
                    aliasConfig.updateKeystoreAlias();
                    VesApplication.restartApplication(); })
                .onFailure(logSkip());
        } else {
            log.info(
                "Collector properties from CBS are the same as currently used ones. " + SKIP_MSG);
        }
    }

    private void compareAndOverwriteDMaaPConfig(JSONObject oldDMaaPConf, JSONObject newDMaaPConf) {
        if (!oldDMaaPConf.toString().equals(newDMaaPConf.toString())) {
            parseToDomainMapping(newDMaaPConf)
                .onFailure(exc -> log.error(SKIP_MSG, exc))
                .onSuccess(eventPublisherReconfigurer)
                .onSuccess(parsedConfig ->
                    configFilesFacade.writeDMaaPConfiguration(newDMaaPConf)
                        .onFailure(logSkip())
                        .onSuccess(__ -> log.info("New dMaaP configuration written to file")));
        } else {
            log.info("DMaaP config from CBS is the same as currently used one. " + SKIP_MSG);
        }
    }

    private Consumer<Throwable> logSkip() {
        return __ -> log.error(SKIP_MSG);
    }
}

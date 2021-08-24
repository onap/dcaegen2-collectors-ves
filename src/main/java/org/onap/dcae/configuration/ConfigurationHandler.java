/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2020-2021 Nokia. All rights reserved.s
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

import com.google.gson.JsonObject;
import io.vavr.control.Option;
import org.json.JSONObject;
import org.onap.dcae.configuration.cbs.CbsClientConfigurationProvider;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClientFactory;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsRequests;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsClientConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsRequest;
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.RequestDiagnosticContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * ConfigurationHandler is responsible for receiving configuration updates from config file or Consul (if config file doesn't exist).
 * Any change made in the configuration will be reported as a notification.
 */
public class ConfigurationHandler {

    private static Logger log = LoggerFactory.getLogger(ConfigurationHandler.class);
    private static final String CONFIG_DICT = "config";

    private final CbsClientConfigurationProvider cbsClientConfigurationProvider;
    private final ConfigUpdater configUpdater;

    /**
     * Constructor
     * @param cbsClientConfigurationProvider provides configuration to connect with Consul
     * @param configUpdater for updating application configuration
     */
    public ConfigurationHandler(CbsClientConfigurationProvider cbsClientConfigurationProvider, ConfigUpdater configUpdater) {
        this.cbsClientConfigurationProvider = cbsClientConfigurationProvider;
        this.configUpdater = configUpdater;
    }

    /**
     * Start listen for application configuration notifications with configuration changes
     * @param interval defines period of time when notification can come
     * @return {@link Disposable} handler to close configuration listener at the end
     */
    public Disposable startListen(Duration interval) {

        log.info("Start listening for configuration ...");
        log.info(String.format("Configuration will be fetched in %s period.", interval));

        // Polling properties
        final Duration initialDelay = Duration.ofSeconds(5);
        final Duration period = interval;

        final CbsRequest request = createCbsRequest();
        final CbsClientConfiguration cbsClientConfiguration = cbsClientConfigurationProvider.get();

        return createCbsClient(cbsClientConfiguration)
                .flatMapMany(cbsClient -> cbsClient.updates(request, initialDelay, period))
                .subscribe(
                        this::handleConfiguration,
                        this::handleError
                );
    }

    Mono<CbsClient> createCbsClient(CbsClientConfiguration cbsClientConfiguration) {
        return CbsClientFactory.createCbsClient(cbsClientConfiguration);
    }

    void handleConfiguration(JsonObject jsonObject) {
        log.info("Configuration update {}", jsonObject);
        if(jsonObject.has(CONFIG_DICT)) {
            JsonObject config = jsonObject.getAsJsonObject(CONFIG_DICT);
            JSONObject jObject = new JSONObject(config.toString());
            configUpdater.updateConfig(Option.of(jObject));
        } else {
            throw new IllegalArgumentException(String.format("Invalid application configuration: %s ", jsonObject));
        }
    }

    private void handleError(Throwable throwable) {
        log.error("Unexpected error occurred during fetching configuration", throwable);
    }

    private CbsRequest createCbsRequest() {
        RequestDiagnosticContext diagnosticContext = RequestDiagnosticContext.create();
        return CbsRequests.getAll(diagnosticContext);
    }
}

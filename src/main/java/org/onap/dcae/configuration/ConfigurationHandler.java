/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.s
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
import org.onap.dcae.configuration.cbs.CbsClientConfigurationResolver;
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

public class ConfigurationHandler {

    private static final String CONFIG_DICT = "config";
    private static Logger log = LoggerFactory.getLogger(ConfigurationHandler.class);

    private final CbsClientConfigurationResolver cbsClientConfigurationResolver;
    private final ConfigUpdater configLoader;

    public ConfigurationHandler(CbsClientConfigurationResolver cbsClientConfigurationResolver, ConfigUpdater configLoader) {
        this.cbsClientConfigurationResolver = cbsClientConfigurationResolver;
        this.configLoader = configLoader;
    }

    public Disposable startListen(Duration interval) {

        log.info("Start listening for configuration from Consul ...");
        log.info(String.format("Consul configuration will be fetched in %s period.", interval));

        // Polling properties
        final Duration initialDelay = Duration.ofSeconds(5);
        final Duration period = interval;

        final CbsRequest request = createCbsRequest();
        final CbsClientConfiguration cbsClientConfiguration = cbsClientConfigurationResolver.resolveCbsClientConfiguration();

        return createCbsClient(cbsClientConfiguration)
                .flatMapMany(cbsClient -> cbsClient.updates(request, initialDelay, period))
                .subscribe(
                        this::handleConfigurationFromConsul,
                        this::handleError
                );
    }

    Mono<CbsClient> createCbsClient(CbsClientConfiguration cbsClientConfiguration) {
        return CbsClientFactory.createCbsClient(cbsClientConfiguration);
    }

    void handleConfigurationFromConsul(JsonObject jsonObject) {
        log.info("Configuration update from Consul {}", jsonObject);
        if(jsonObject.has(CONFIG_DICT)) {
            JsonObject config = jsonObject.getAsJsonObject(CONFIG_DICT);
            JSONObject jObject = new JSONObject(config.toString());
            configLoader.updateConfig(Option.of(jObject));
        } else {
            throw new IllegalArgumentException(String.format("Invalid application configuration: %s ", jsonObject));
        }
    }

    private void handleError(Throwable throwable) {
        log.error("Unexpected error occurred during fetching configuration from Consul", throwable);
    }

    private CbsRequest createCbsRequest() {
        RequestDiagnosticContext diagnosticContext = RequestDiagnosticContext.create();
        return CbsRequests.getAll(diagnosticContext);
    }
}

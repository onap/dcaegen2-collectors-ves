/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.dcae;

import io.vavr.collection.Map;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.onap.dcae.common.EventProcessor;
import org.onap.dcae.common.EventSender;
import org.onap.dcae.common.publishing.DMaaPConfigurationParser;
import org.onap.dcae.common.publishing.EventPublisher;
import org.onap.dcae.common.publishing.PublisherConfig;
import org.onap.dcae.controller.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

@SpringBootApplication(exclude = {GsonAutoConfiguration.class, SecurityAutoConfiguration.class})
public class VesApplication {

    private static final Logger metriclog = LoggerFactory.getLogger("com.att.ecomp.metrics");
    private static final Logger incomingRequestsLogger = LoggerFactory.getLogger("org.onap.dcae.common.input");
    private static final Logger oplog = LoggerFactory.getLogger("org.onap.dcae.common.output");
    private static final Logger errorLog = LoggerFactory.getLogger("org.onap.dcae.common.error");
    private static final int MAX_THREADS = 20;
    public static LinkedBlockingQueue<JSONObject> fProcessingInputQueue;
    private static ApplicationSettings properties;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(VesApplication.class);

        properties = new ApplicationSettings(args, CLIUtils::processCmdLine);

        fProcessingInputQueue = new LinkedBlockingQueue<>(properties.maximumAllowedQueuedEvents());

        EventPublisher publisher = EventPublisher.createPublisher(oplog,
                DMaaPConfigurationParser
                        .parseToDomainMapping(Paths.get(properties.dMaaPConfigurationFileLocation()))
                        .get());
        spawnDynamicConfigUpdateThread(publisher, properties);
        EventProcessor ep = new EventProcessor(
            new EventSender(EventPublisher.createPublisher(oplog, getDmapConfig()), properties));

        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
        for (int i = 0; i < MAX_THREADS; ++i) {
            executor.execute(ep);
        }

        app.setAddCommandLineProperties(true);
        app.run();
    }

    private static void spawnDynamicConfigUpdateThread(EventPublisher eventPublisher, ApplicationSettings properties) {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        ConfigLoader configLoader = ConfigLoader
                .create(eventPublisher::reconfigure,
                        Paths.get(properties.dMaaPConfigurationFileLocation()),
                        properties.configurationFileLocation());
        scheduledThreadPoolExecutor
                .scheduleAtFixedRate(() -> configLoader.updateConfig(),
                        properties.configurationUpdateFrequency(),
                        properties.configurationUpdateFrequency(),
                        TimeUnit.MINUTES);
    }

    private static Map<String, PublisherConfig> getDmapConfig() {
        return DMaaPConfigurationParser.
                parseToDomainMapping(Paths.get(properties.dMaaPConfigurationFileLocation())).get();
    }

    @Bean
    @Lazy
    public ApplicationSettings applicationSettings() {
        return properties;
    }

    @Bean
    @Qualifier("incomingRequestsLogger")
    public Logger incomingRequestsLogger() {
        return incomingRequestsLogger;
    }

    @Bean
    @Qualifier("metricsLog")
    public Logger incomingRequestsMetricsLogger() {
        return metriclog;
    }

    @Bean
    @Qualifier("errorLog")
    public Logger errorLogger() {
        return errorLog;
    }

    @Bean
    public LinkedBlockingQueue<JSONObject> inputQueue() {
        return fProcessingInputQueue;
    }

}

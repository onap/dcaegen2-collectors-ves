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
import org.json.JSONObject;
import org.onap.dcae.commonFunction.EventProcessor;
import org.onap.dcae.commonFunction.event.publishing.DMaaPConfigurationParser;
import org.onap.dcae.commonFunction.event.publishing.EventPublisher;
import org.onap.dcae.commonFunction.event.publishing.PublisherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {GsonAutoConfiguration.class, SecurityAutoConfiguration.class})
public class VesApplication {

    private static final Logger metriclog = LoggerFactory.getLogger("com.att.ecomp.metrics");
    private static final Logger incomingRequestsLogger = LoggerFactory.getLogger("org.onap.dcae.commonFunction.input");
    private static final Logger oplog = LoggerFactory.getLogger("org.onap.dcae.commonFunction.output");
    private static final Logger errorLog = LoggerFactory.getLogger("org.onap.dcae.commonFunction.error");
    private static final int MAX_THREADS = 20;
    public static LinkedBlockingQueue<JSONObject> fProcessingInputQueue;
    private static ApplicationSettings properties;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(VesApplication.class);

        properties = new ApplicationSettings(args, CLIUtils::processCmdLine);

        fProcessingInputQueue = new LinkedBlockingQueue<>(properties.maximumAllowedQueuedEvents());

        app.setAddCommandLineProperties(true);
        app.run();

        EventProcessor ep = new EventProcessor(EventPublisher.createPublisher(oplog, getDmapConfig()), properties);

        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
        for (int i = 0; i < MAX_THREADS; ++i) {
            executor.execute(ep);
        }
    }


    private static Map<String, PublisherConfig> getDmapConfig() {
        return DMaaPConfigurationParser.
                parseToDomainMapping(Paths.get(properties.cambriaConfigurationFileLocation())).get();
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
    @Qualifier("metriclog")
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

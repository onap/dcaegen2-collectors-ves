/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.dcae;

import io.vavr.collection.Map;
import org.onap.dcae.common.EventSender;
import org.onap.dcae.common.validator.StndDefinedValidatorResolver;
import org.onap.dcae.common.publishing.DMaaPConfigurationParser;
import org.onap.dcae.common.publishing.DMaaPEventPublisher;
import org.onap.dcae.common.publishing.PublisherConfig;
import org.onap.dcae.configuration.ConfigLoader;
import org.onap.dcae.configuration.ConfigLoaderFactory;
import org.onap.dcaegen2.services.sdk.services.external.schema.manager.service.StndDefinedValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

import java.nio.file.Paths;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SpringBootApplication(exclude = {GsonAutoConfiguration.class, SecurityAutoConfiguration.class})
public class VesApplication {

    private static final Logger incomingRequestsLogger = LoggerFactory.getLogger("org.onap.dcae.common.input");
    private static final Logger errorLog = LoggerFactory.getLogger("org.onap.dcae.common.error");
    private static ApplicationSettings applicationSettings;
    private static ConfigurableApplicationContext context;
    private static ConfigLoader configLoader;
    private static ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private static DMaaPEventPublisher eventPublisher;
    private static ScheduledFuture<?> scheduleFeatures;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(VesApplication.class);
        applicationSettings = new ApplicationSettings(args, CLIUtils::processCmdLine);
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        init();
        app.setAddCommandLineProperties(true);
        context = app.run();
        configLoader.updateConfig();
    }

    public static void restartApplication() {
        Thread thread = new Thread(() -> {
            context.close();
            applicationSettings.reloadProperties();
            scheduleFeatures.cancel(true);
            init();
            context = SpringApplication.run(VesApplication.class);
        });
        thread.setDaemon(false);
        thread.start();
    }

    private static void init() {
        createConfigLoader();
        createSchedulePoolExecutor();
        createExecutors();
    }

    private static void createExecutors() {
        eventPublisher = new DMaaPEventPublisher(getDmaapConfig());
    }

    private static void createSchedulePoolExecutor() {
        scheduleFeatures = scheduledThreadPoolExecutor.scheduleAtFixedRate(configLoader::updateConfig,
                applicationSettings.configurationUpdateFrequency(),
                applicationSettings.configurationUpdateFrequency(),
                TimeUnit.MINUTES);
    }

    private static void createConfigLoader() {
        configLoader = new ConfigLoaderFactory().create(
                applicationSettings.configurationFileLocation(),
                Paths.get(applicationSettings.dMaaPConfigurationFileLocation()));
    }

    private static Map<String, PublisherConfig> getDmaapConfig() {
        return DMaaPConfigurationParser
                .parseToDomainMapping(Paths.get(applicationSettings.dMaaPConfigurationFileLocation())).get();
    }

    @Bean
    @Lazy
    public ApplicationSettings applicationSettings() {
        return applicationSettings;
    }

    @Bean
    @Qualifier("incomingRequestsLogger")
    public Logger incomingRequestsLogger() {
        return incomingRequestsLogger;
    }

    @Bean
    @Qualifier("errorLog")
    public Logger errorLogger() {
        return errorLog;
    }

    @Bean
    @Qualifier("eventSender")
    public EventSender eventSender() {
        return new EventSender(eventPublisher, applicationSettings.getDmaapStreamIds());
    }

    @Bean
    public StndDefinedValidator getStndDefinedValidator(StndDefinedValidatorResolver resolver) {
        return resolver.resolve();
    }

}

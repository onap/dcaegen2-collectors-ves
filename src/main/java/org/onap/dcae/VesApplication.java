/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2020-2021 Nokia. All rights reserved.
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
import org.onap.dcae.configuration.ConfigurationHandler;
import org.onap.dcae.configuration.ConfigUpdater;
import org.onap.dcae.configuration.ConfigUpdaterFactory;
import org.onap.dcae.configuration.cbs.CbsClientConfigurationProvider;
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
import java.time.Duration;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

@SpringBootApplication(exclude = {GsonAutoConfiguration.class, SecurityAutoConfiguration.class})
public class VesApplication {

    private static final int DEFAULT_CONFIGURATION_FETCH_PERIOD = 5;

    private static final Logger incomingRequestsLogger = LoggerFactory.getLogger("org.onap.dcae.common.input");
    private static final Logger errorLog = LoggerFactory.getLogger("org.onap.dcae.common.error");
    private static ApplicationSettings applicationSettings;
    private static ConfigurableApplicationContext context;
    private static ConfigUpdater configUpdater;
    private static DMaaPEventPublisher eventPublisher;
    private static ApplicationConfigurationListener applicationConfigurationListener;
    private static ReentrantLock applicationLock = new ReentrantLock();

    public static void main(String[] args) {
        applicationLock.lock();
        try {
            startApplication(args);
            startListeningForApplicationConfiguration();
        } finally {
            applicationLock.unlock();
        }
    }

    private static void startApplication(String[] args) {
        SpringApplication app = new SpringApplication(VesApplication.class);
        applicationSettings = new ApplicationSettings(args, CLIUtils::processCmdLine);
        configUpdater = ConfigUpdaterFactory.create(
                applicationSettings.configurationFileLocation(),
                Paths.get(applicationSettings.dMaaPConfigurationFileLocation()));
        eventPublisher = new DMaaPEventPublisher(getDmaapConfig());
        app.setAddCommandLineProperties(true);
        context = app.run();
    }

    public static void restartApplication() {
        Thread thread = new Thread(() -> {
            try {
                applicationLock.lock();
                reloadApplicationResources();
                reloadSpringContext();
            } finally {
                applicationLock.unlock();
            }
        });
        thread.setDaemon(false);
        thread.start();
    }

    private static void reloadApplicationResources() {
        applicationSettings.reload();
        eventPublisher.reload(getDmaapConfig());
        configUpdater.setPaths(applicationSettings.configurationFileLocation(),
                Paths.get(applicationSettings.dMaaPConfigurationFileLocation()));
        applicationConfigurationListener.reload(Duration.ofMinutes(applicationSettings.configurationUpdateFrequency()));
    }

    private static void reloadSpringContext() {
        context.close();
        context = SpringApplication.run(VesApplication.class);
    }

    private static void startListeningForApplicationConfiguration() {
        ConfigurationHandler cbsHandler = new ConfigurationHandler(new CbsClientConfigurationProvider(), configUpdater);
        ApplicationConfigurationListener applicationConfigProvider = new ApplicationConfigurationListener(Duration.ofMinutes(DEFAULT_CONFIGURATION_FETCH_PERIOD), cbsHandler);

        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        scheduledThreadPoolExecutor.execute(applicationConfigProvider);
        applicationConfigurationListener = applicationConfigProvider;
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

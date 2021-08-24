/*-
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
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

import org.onap.dcae.configuration.ConfigurationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;

import java.time.Duration;

/**
 * ApplicationConfigurationListener is used to listen at notifications with configuration updates.
 */
public class ApplicationConfigurationListener implements Runnable {

    private static Logger log = LoggerFactory.getLogger(ApplicationConfigurationListener.class);

    private Duration interval;
    private boolean terminate = false;
    private final ConfigurationHandler configurationHandler;

    /**
     * Constructor
     * @param interval defines period of time when notification can come
     * @param configurationHandler handles notifications
     */
    public ApplicationConfigurationListener(Duration interval, ConfigurationHandler configurationHandler) {
        this.interval = interval;
        this.configurationHandler = configurationHandler;
    }

    /**
     * Reload listener to start listening for configurations notifications with defined interval.
     * @param interval defines period of time when notification can come
     */
    public synchronized void reload(Duration interval) {
        this.interval = interval;
        log.info("Handler configuration was changed. Need to reload configuration handler.");
        sendReloadAction();
    }

    synchronized void sendReloadAction() {
        this.notifyAll();
    }

    /**
     * Start listening for configurations notification.
     */
    @Override
    public void run() {
        Disposable configListener = null;
        do {
            try {
                configListener = listenForConfigurationUpdates();
                synchronized (this) {
                    log.info("Switch to configuration handler thread. Active waiting for configuration.");
                    this.wait();
                }
            } catch (Exception e) {
                log.error("Unexpected error occurred during handling data.", e);
                terminate();
            } finally {
                stopListeningForConfigurationUpdates(configListener);
            }
        } while (!this.terminate);
    }

    private Disposable listenForConfigurationUpdates() {
        return this.configurationHandler.startListen(this.interval);
    }

    void terminate() {
        this.terminate = true;
    }

    /**
     * Release resources when there is a need to stop listener
     * @param consulListener Handler to configurations listener
     */
    void stopListeningForConfigurationUpdates(Disposable consulListener) {
        if (consulListener != null) {
            consulListener.dispose();
        }
    }
}

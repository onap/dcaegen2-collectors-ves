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
package org.onap.dcae.commonFunction.event.publishing;

import static io.vavr.API.Option;
import static org.onap.dcae.commonFunction.event.publishing.VavrUtils.f;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
class DMaaPPublishersCache {

    private static final Logger log = LoggerFactory.getLogger(DMaaPPublishersCache.class);
    private final LoadingCache<String, CambriaBatchingPublisher> publishersCache;
    private AtomicReference<Map<String, PublisherConfig>> dMaaPConfiguration;

    DMaaPPublishersCache(Map<String, PublisherConfig> dMaaPConfiguration) {
        this.dMaaPConfiguration = new AtomicReference<>(dMaaPConfiguration);
        this.publishersCache = CacheBuilder.newBuilder()
            .removalListener(new OnPublisherRemovalListener())
            .build(new CambriaPublishersCacheLoader());
    }

    DMaaPPublishersCache(CambriaPublishersCacheLoader dMaaPPublishersCacheLoader,
                         OnPublisherRemovalListener onPublisherRemovalListener,
                         Map<String, PublisherConfig> dMaaPConfiguration) {
        this.dMaaPConfiguration = new AtomicReference<>(dMaaPConfiguration);
        this.publishersCache = CacheBuilder.newBuilder()
            .removalListener(onPublisherRemovalListener)
            .build(dMaaPPublishersCacheLoader);
    }

    Option<CambriaBatchingPublisher> getPublisher(String streamID) {
        try {
            return Option(publishersCache.getUnchecked(streamID));
        } catch (Exception e) {
            log.warn("Could not create / load Cambria Publisher for streamID", e);
            return Option.none();
        }
    }

    void closePublisherFor(String streamId) {
        publishersCache.invalidate(streamId);
    }

    synchronized void reconfigure(Map<String, PublisherConfig> newConfig) {
        Map<String, PublisherConfig> currentConfig = dMaaPConfiguration.get();
        Map<String, PublisherConfig> removedConfigurations = currentConfig
            .filterKeys(domain -> !newConfig.containsKey(domain));
        Map<String, PublisherConfig> changedConfigurations = newConfig
            .filterKeys(e -> currentConfig.containsKey(e) && !currentConfig.get(e).equals(newConfig.get(e)));
        dMaaPConfiguration.set(newConfig);
        removedConfigurations.merge(changedConfigurations).forEach(e -> publishersCache.invalidate(e._1));
    }

    static class OnPublisherRemovalListener implements RemovalListener<String, CambriaBatchingPublisher> {

        @Override
        public void onRemoval(@Nonnull RemovalNotification<String, CambriaBatchingPublisher> notification) {
            CambriaBatchingPublisher publisher = notification.getValue();
            if (publisher != null) { // The value might get Garbage Collected at this moment, regardless of @Nonnull
                try {
                    int timeout = 20;
                    TimeUnit unit = TimeUnit.SECONDS;
                    java.util.List<?> stuck = publisher.close(timeout, unit);
                    if (!stuck.isEmpty()) {
                        log.error(f("Publisher got stuck and did not manage to close in '%s' '%s', "
                            + "%s messages were dropped", stuck.size(), timeout, unit));
                    }
                } catch (InterruptedException | IOException e) {
                    log.error("Could not close Cambria publisher, some messages might have been dropped", e);
                }
            }
        }
    }

    class CambriaPublishersCacheLoader extends CacheLoader<String, CambriaBatchingPublisher> {

        @Override
        public CambriaBatchingPublisher load(@Nonnull String domain) {
            return dMaaPConfiguration.get()
                .get(domain)
                .toTry(() -> new RuntimeException(
                    f("DMaaP configuration contains no configuration for domain: '%s'", domain)))
                .flatMap(DMaaPPublishersBuilder::buildPublisher)
                .get();
        }
    }

}

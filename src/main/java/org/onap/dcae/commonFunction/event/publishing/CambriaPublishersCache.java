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
import static io.vavr.API.unchecked;
import static java.lang.String.format;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.cambria.client.CambriaClientBuilders;
import com.att.nsa.cambria.client.CambriaClientBuilders.PublisherBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
class CambriaPublishersCache {

    private static final Logger LOG = LoggerFactory.getLogger(CambriaPublishersCache.class);
    private final LoadingCache<String, CambriaBatchingPublisher> publishersCache;

    @VisibleForTesting
    CambriaPublishersCache(CambriaPublishersCacheLoader cambriaPublishersCacheLoader) {
        this.publishersCache = CacheBuilder.newBuilder()
            .removalListener(new OnPublisherRemovalListener())
            .build(cambriaPublishersCacheLoader);
    }

    static CambriaPublishersCache create(Map<String, PublisherConfig> dMaaPPublisherConfigs) {
        return new CambriaPublishersCache(new CambriaPublishersCacheLoader(dMaaPPublisherConfigs));
    }

    Option<CambriaBatchingPublisher> getPublisher(String streamID) {
        try {
            return Option(publishersCache.getUnchecked(streamID));
        } catch (Exception e) {
            LOG.error("Could not create / load Cambria Publisher for streamID", e);
            return Option.none();
        }
    }

    void closePublisherFor(String streamId) {
        publishersCache.invalidate(streamId);
    }


    static class CambriaPublishersCacheLoader extends CacheLoader<String, CambriaBatchingPublisher> {

        private final Map<String, PublisherConfig> dMaaPPublisherConfigs;

        CambriaPublishersCacheLoader(Map<String, PublisherConfig> dMaaPPublisherConfigs) {
            this.dMaaPPublisherConfigs = dMaaPPublisherConfigs;
        }

        @Override
        public CambriaBatchingPublisher load(@Nonnull String streamId) {
            return dMaaPPublisherConfigs.get(streamId)
                .map(config -> {
                    if (config.isSecured()) {
                        return buildAuthenticatedPublisher(config);
                    } else {
                        return buildPublisher(config);
                    }
                }).getOrElseThrow(() -> new RuntimeException("No publisher found for streamId: " + streamId));
        }

        private CambriaBatchingPublisher buildAuthenticatedPublisher(PublisherConfig config) {
            return unchecked(() -> builder(config)
                .usingHttps()
                .authenticatedByHttp(config.userName().get(), config.password().get())
                .build()).apply();
        }

        private CambriaBatchingPublisher buildPublisher(PublisherConfig config) {
            return unchecked(() -> builder(config).build()).apply();
        }

        private PublisherBuilder builder(PublisherConfig config) {
            return new CambriaClientBuilders.PublisherBuilder()
                .usingHosts(config.destinations().mkString(","))
                .onTopic(config.topic())
                .logSendFailuresAfter(5);
        }

    }

    private static class OnPublisherRemovalListener implements RemovalListener<String, CambriaBatchingPublisher> {

        private static final Logger LOG = LoggerFactory.getLogger(CambriaPublishersCache.class);

        @Override
        public void onRemoval(@Nonnull RemovalNotification<String, CambriaBatchingPublisher> notification) {
            CambriaBatchingPublisher publisher = notification.getValue();
            if (publisher != null) { // The value might get Garbage Collected at this moment, regardless of @Nonnull
                try {
                    List<?> stuck = publisher.close(20, TimeUnit.SECONDS);
                    if (!stuck.isEmpty()) {
                        LOG.error(format("Publisher got stuck, %s messages were dropped", stuck.size()));
                    }
                } catch (InterruptedException | IOException e) {
                    LOG.error("Could not close Cambria publisher, some messages might have been dropped", e);
                }
            }
        }
    }


}

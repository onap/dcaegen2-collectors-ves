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
package org.onap.dcae.commonFunction;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DmaapPublishers {

    private static final Logger log = LoggerFactory.getLogger(DmaapPublishers.class);
    private final LoadingCache<String, CambriaBatchingPublisher> publishers;

    private DmaapPublishers(
            LoadingCache<String, CambriaBatchingPublisher> publishers) {
        this.publishers = publishers;
    }

    static DmaapPublishers create() {
        return create(new CambriaPublisherFactory());
    }

    static DmaapPublishers create(final CambriaPublisherFactory publisherFactory) {
        final LoadingCache<String, CambriaBatchingPublisher> cache = CacheBuilder.<String, CambriaBatchingPublisher>newBuilder()
                .removalListener((RemovalListener<String, CambriaBatchingPublisher>) notification -> {
                    if (notification.getValue() != null) {
                        onCacheItemInvalidated(notification.getValue());
                    }
                })
                .build(new CacheLoader<String, CambriaBatchingPublisher>() {
                    @Override
                    public CambriaBatchingPublisher load(String streamId)
                            throws MalformedURLException, GeneralSecurityException {
                        try {
                            return publisherFactory.createCambriaPublisher(streamId);
                        } catch (MalformedURLException | GeneralSecurityException e) {
                            log.error("CambriaClientBuilders connection reader exception : streamID - " + streamId + " "
                                    + e.getMessage());
                            throw e;
                        }
                    }
                });
        return new DmaapPublishers(cache);
    }

    CambriaBatchingPublisher getByStreamId(String streamId) {
        return publishers.getUnchecked(streamId);
    }

    void closeByStreamId(String streamId) {
        publishers.invalidate(streamId);
    }

    private static void onCacheItemInvalidated(CambriaBatchingPublisher pub) {
        try {
            final List<?> stuck = pub.close(20, TimeUnit.SECONDS);
            if (!stuck.isEmpty()) {
                log.error(stuck.size() + " messages unsent");
            }
        } catch (InterruptedException | IOException e) {
            log.error("Caught Exception on Close event: {}", e);
        }
    }
}

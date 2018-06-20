/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
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

import static io.vavr.API.List;
import static io.vavr.API.Map;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.onap.dcae.commonFunction.event.publishing.CambriaPublishersCache.CambriaPublishersCacheLoader;


public class CambriaPublishersCacheTest {

    private String streamId;
    private Map<String, PublisherConfig> dMaaPConfigs;

    @Before
    public void setUp() {
        streamId = "sampleStream";
        dMaaPConfigs = Map("sampleStream", new PublisherConfig(List("destination"), "topic"));
    }

    @Test
    public void shouldReturnTheSameCachedInstanceOnConsecutiveRetrievals() {
        // when
        CambriaPublishersCache cambriaPublishersCache = CambriaPublishersCache.create(dMaaPConfigs);

        // when
        Option<CambriaBatchingPublisher> firstPublisher = cambriaPublishersCache.getPublisher(streamId);
        Option<CambriaBatchingPublisher> secondPublisher = cambriaPublishersCache.getPublisher(streamId);

        // then
        assertSame("should return same instance", firstPublisher.get(), secondPublisher.get());
    }

    @Test
    public void shouldCloseCambriaPublisherOnCacheInvalidate() throws IOException, InterruptedException {
        // given
        CambriaBatchingPublisher cambriaPublisherMock = mock(CambriaBatchingPublisher.class);
        CambriaPublishersCacheLoader cacheLoaderMock = mock(CambriaPublishersCacheLoader.class);
        CambriaPublishersCache cambriaPublishersCache = new CambriaPublishersCache(cacheLoaderMock);
        when(cacheLoaderMock.load(streamId)).thenReturn(cambriaPublisherMock);

        // when
        cambriaPublishersCache.getPublisher(streamId);
        cambriaPublishersCache.closePublisherFor(streamId);

        // then
        verify(cambriaPublisherMock).close(20, TimeUnit.SECONDS);

    }

    @Test
    public void shouldReturnNoneIfThereIsNoDMaaPConfigurationForGivenStreamID() {
        // given
        CambriaPublishersCache cambriaPublishersCache = CambriaPublishersCache.create(dMaaPConfigs);

        // then
        assertTrue("should not exist", cambriaPublishersCache.getPublisher("non-existing").isEmpty());
    }
}
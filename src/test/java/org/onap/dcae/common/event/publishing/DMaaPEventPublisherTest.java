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
package org.onap.dcae.common.event.publishing;

import static io.vavr.API.Option;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import java.io.IOException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

public class DMaaPEventPublisherTest {

    private static final String STREAM_ID = "sampleStreamId";

    private DMaaPEventPublisher eventPublisher;
    private CambriaBatchingPublisher cambriaPublisher;
    private DMaaPPublishersCache DMaaPPublishersCache;

    @Before
    public void setUp() {
        cambriaPublisher = mock(CambriaBatchingPublisher.class);
        DMaaPPublishersCache = mock(DMaaPPublishersCache.class);
        when(DMaaPPublishersCache.getPublisher(anyString())).thenReturn(Option(cambriaPublisher));
        eventPublisher = new DMaaPEventPublisher(DMaaPPublishersCache, mock(Logger.class));
    }

    @Test
    public void shouldSendEventToTopic() throws Exception {
        // given
        JSONObject event = new JSONObject("{}");

        // when
        eventPublisher.sendEvent(event, STREAM_ID);

        // then
        verify(cambriaPublisher).send("MyPartitionKey", event.toString());
    }

    @Test
    public void shouldRemoveInternalVESUIDBeforeSending() throws Exception {
        // given
        JSONObject event = new JSONObject(
            "{\"VESuniqueId\": \"362e0146-ec5f-45f3-8d8f-bfe877c3f58e\", \"another\": 8}");

        // when
        eventPublisher.sendEvent(event, STREAM_ID);

        // then
        verify(cambriaPublisher).send("MyPartitionKey", new JSONObject("{\"another\": 8}").toString());
    }

    @Test
    public void shouldCloseConnectionWhenExceptionOccurred() throws Exception {
        // given
        JSONObject event = new JSONObject("{}");
        given(cambriaPublisher.send(anyString(), anyString())).willThrow(new IOException("epic fail"));

        // when
        eventPublisher.sendEvent(event, STREAM_ID);

        // then
        verify(DMaaPPublishersCache).closePublisherFor(STREAM_ID);
    }
}
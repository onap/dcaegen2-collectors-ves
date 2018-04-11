/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import java.io.IOException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventPublisherHashTest {
    private EventPublisherHash cut;

    @Mock
    private DmaapPublishers dmaapPublishers;
    @Mock
    private CambriaBatchingPublisher cambriaPublisher;

    @Before
    public void setUp() {
        given(dmaapPublishers.getByStreamId(anyString())).willReturn(cambriaPublisher);

        cut = new EventPublisherHash(dmaapPublishers);
    }

    @Test
    public void sendEventShouldSendEventToATopic() throws Exception {
        // given
        JSONObject event = new JSONObject("{}");
        final String streamId = "sampleStreamId";

        // when
        cut.sendEvent(event, streamId);

        // then
        verify(cambriaPublisher).send("MyPartitionKey", event.toString());
    }

    @Test
    public void sendEventShouldRemoveUuid() throws Exception {
        // given
        JSONObject event = new JSONObject("{\"VESuniqueId\": \"362e0146-ec5f-45f3-8d8f-bfe877c3f58e\", \"another\": 8}");
        final String streamId = "sampleStreamId";

        // when
        cut.sendEvent(event, streamId);

        // then
        verify(cambriaPublisher).send("MyPartitionKey", new JSONObject("{\"another\": 8}").toString());
    }

    @Test
    public void sendEventShouldCloseConnectionWhenExceptionOccurred() throws Exception {
        // given
        JSONObject event = new JSONObject("{}");
        final String streamId = "sampleStreamId";
        given(cambriaPublisher.send(anyString(), anyString())).willThrow(new IOException("epic fail"));

        // when
        cut.sendEvent(event, streamId);

        // then
        verify(dmaapPublishers).closeByStreamId(streamId);
    }
}
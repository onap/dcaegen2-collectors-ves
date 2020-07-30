/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2018 Nokia. All rights reserved.
 * Copyright (C) 2020 AT&T. All rights reserved.
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
package org.onap.dcae.common.publishing;

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
        eventPublisher = new DMaaPEventPublisher(DMaaPPublishersCache);
    }

    @Test
    public void shouldSendEventToTopic() throws Exception {
        // given
        JSONObject event = new JSONObject("{\"event\":{\"commonEventHeader\":{\"startEpochMicrosec\":1537562659253019,\"sourceId\":\"79e90d76-513a-4f79-886d-470a0037c5cf\",\"eventId\":\"Heartbeat_vDNS_100.100.10.10\",\"nfcNamingCode\":\"DNS\",\"reportingEntityId\":\"79e90d76-513a-4f79-886d-470a0037c5cf\",\"eventType\":\"applicationVnf\",\"priority\":\"Normal\",\"version\":3,\"reportingEntityName\":\"dns01cmd004\",\"sequence\":36312,\"domain\":\"heartbeat\",\"lastEpochMicrosec\":1537562659253019,\"eventName\":\"Heartbeat_vDNS\",\"sourceName\":\"dns01cmd004\",\"nfNamingCode\":\"MDNS\"}}}");


        // when
        eventPublisher.sendEvent(event, STREAM_ID);

        // then
        verify(cambriaPublisher).send("dns01cmd004", event.toString());
    }
    

    @Test
    public void shouldRemoveInternalVESUIDBeforeSending() throws Exception {
        // given
        JSONObject event = new JSONObject(
            "{\"VESuniqueId\": \"362e0146-ec5f-45f3-8d8f-bfe877c3f58e\",\"event\":{\"commonEventHeader\":{\"startEpochMicrosec\":1537562659253019,\"sourceId\":\"79e90d76-513a-4f79-886d-470a0037c5cf\",\"eventId\":\"Heartbeat_vDNS_100.100.10.10\",\"nfcNamingCode\":\"DNS\",\"reportingEntityId\":\"79e90d76-513a-4f79-886d-470a0037c5cf\",\"eventType\":\"applicationVnf\",\"priority\":\"Normal\",\"version\":3,\"reportingEntityName\":\"dns01cmd004\",\"sequence\":36312,\"domain\":\"heartbeat\",\"lastEpochMicrosec\":1537562659253019,\"eventName\":\"Heartbeat_vDNS\",\"sourceName\":\"dns01cmd004\",\"nfNamingCode\":\"MDNS\"}}}"); 

        // when
        eventPublisher.sendEvent(event, STREAM_ID);

        // then
        verify(cambriaPublisher).send("dns01cmd004", new JSONObject("{\"event\":{\"commonEventHeader\":{\"startEpochMicrosec\":1537562659253019,\"sourceId\":\"79e90d76-513a-4f79-886d-470a0037c5cf\",\"eventId\":\"Heartbeat_vDNS_100.100.10.10\",\"nfcNamingCode\":\"DNS\",\"reportingEntityId\":\"79e90d76-513a-4f79-886d-470a0037c5cf\",\"eventType\":\"applicationVnf\",\"priority\":\"Normal\",\"version\":3,\"reportingEntityName\":\"dns01cmd004\",\"sequence\":36312,\"domain\":\"heartbeat\",\"lastEpochMicrosec\":1537562659253019,\"eventName\":\"Heartbeat_vDNS\",\"sourceName\":\"dns01cmd004\",\"nfNamingCode\":\"MDNS\"}}}").toString());
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

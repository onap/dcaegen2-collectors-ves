/*-
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2023 AT&T Intellectual Property. All rights reserved.
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

import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.dcae.common.JsonDataLoader;
import org.onap.dcae.common.model.VesEvent;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.ImmutableMessageRouterPublishResponse;
import org.onap.dcaegen2.services.sdk.rest.services.dmaap.client.model.MessageRouterPublishResponse;
import org.springframework.http.HttpStatus;

import com.google.gson.JsonElement;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

public class DMaaPEventPublisherTest {

    @Test
    public void sendEventtest() throws IOException, URISyntaxException {

        HttpStatus expectedrc = HttpStatus.ACCEPTED;

        Map<String, PublisherConfig> dMaaPConfig = HashMap.of("key1", new PublisherConfig(null, null));
        DMaaPEventPublisher dmaapEventpub = Mockito.spy(new DMaaPEventPublisher(dMaaPConfig));
        java.util.List<VesEvent> eventToSend = createEventToSend("/eventsAfterTransformation/ves7_valid_event.json");
        doReturn(expectedrc).when(dmaapEventpub).messageRouterPublishResponse(Mockito.any(), Mockito.any());
        HttpStatus rc = dmaapEventpub.sendEvent(eventToSend, "ves-fault");
        assertEquals(expectedrc.toString(), rc.toString());
    }

    private java.util.List<VesEvent> createEventToSend(String path) throws IOException, URISyntaxException {
        String event = JsonDataLoader.loadContent(path);
        JSONObject jsonObject = new JSONObject(event);
        java.util.List<VesEvent> javaStringList = (List.of(new VesEvent(jsonObject))).toJavaList();
        return javaStringList;
    }

    private MessageRouterPublishResponse successPublishResponse(List<JsonElement> items) {
        return ImmutableMessageRouterPublishResponse.builder().items(items).build();
    }

}

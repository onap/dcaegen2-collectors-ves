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

import com.google.gson.Gson;
import java.util.concurrent.atomic.AtomicReference;

import io.vavr.collection.HashMap;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import org.onap.dcae.commonFunction.event.publishing.EventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.dcae.commonFunction.EventProcessor.EVENT_LIST_TYPE;

public class EventProcessorTest {

    private final String ev = "{\"event\": {\"commonEventHeader\": {	\"reportingEntityName\": \"VM name will be provided by ECOMP\",	\"startEpochMicrosec\": 1477012779802988,\"lastEpochMicrosec\": 1477012789802988,\"eventId\": \"83\",\"sourceName\": \"Dummy VM name - No Metadata available\",\"sequence\": 83,\"priority\": \"Normal\",\"functionalRole\": \"vFirewall\",\"domain\": \"measurementsForVfScaling\",\"reportingEntityId\": \"VM UUID will be provided by ECOMP\",\"sourceId\": \"Dummy VM UUID - No Metadata available\",\"version\": 1.1},\"measurementsForVfScalingFields\": {\"measurementInterval\": 10,\"measurementsForVfScalingVersion\": 1.1,\"vNicUsageArray\": [{\"multicastPacketsIn\": 0,\"bytesIn\": 3896,\"unicastPacketsIn\": 0,	\"multicastPacketsOut\": 0,\"broadcastPacketsOut\": 0,		\"packetsOut\": 28,\"bytesOut\": 12178,\"broadcastPacketsIn\": 0,\"packetsIn\": 58,\"unicastPacketsOut\": 0,\"vNicIdentifier\": \"eth0\"}]}}}";

    @Before
    public void setUp() {
        CommonStartup.streamID = convertDMaaPStreamsPropertyToMap("fault=sec_fault|syslog=sec_syslog|heartbeat=sec_heartbeat|measurementsForVfScaling=sec_measurement|mobileFlow=sec_mobileflow|other=sec_other|stateChange=sec_statechange|thresholdCrossingAlert=sec_thresholdCrossingAlert|voiceQuality=ves_voicequality|sipSignaling=ves_sipsignaling");
    }

    @Test
    public void testLoad() {
        //given
        EventProcessor ec = new EventProcessor(mock(EventPublisher.class));
        ec.event = new org.json.JSONObject(ev);
        //when
        ec.overrideEvent();

        //then
        Boolean hasSourceNameNode = ec.event.getJSONObject("event").getJSONObject("commonEventHeader").has("sourceName");
        assertTrue(hasSourceNameNode);
    }

    @Test
    public void shouldParseJsonEvents() throws ReflectiveOperationException {
        //given
        EventProcessor eventProcessor = new EventProcessor(mock(EventPublisher.class));
        String event_json = "[{ \"filter\": {\"event.commonEventHeader.domain\":\"heartbeat\",\"VESversion\":\"v4\"},\"processors\":[" +
                "{\"functionName\": \"concatenateValue\",\"args\":{\"field\":\"event.commonEventHeader.eventName\",\"concatenate\": [\"$event.commonEventHeader.domain\",\"$event.commonEventHeader.eventType\",\"$event.faultFields.alarmCondition\"], \"delimiter\":\"_\"}}" +
                ",{\"functionName\": \"addAttribute\",\"args\":{\"field\": \"event.heartbeatFields.heartbeatFieldsVersion\",\"value\": \"1.0\",\"fieldType\": \"number\"}}" +
                ",{\"functionName\": \"map\",\"args\":{\"field\": \"event.commonEventHeader.nfNamingCode\",\"oldField\": \"event.commonEventHeader.functionalRole\"}}]}]";
        List<Event> events = new Gson().fromJson(event_json, EVENT_LIST_TYPE);
        EventProcessor.ConfigProcessorAdapter configProcessorAdapter = mock(EventProcessor.ConfigProcessorAdapter.class);

        when(configProcessorAdapter.isFilterMet(any(JSONObject.class))).thenReturn(true);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JSONObject> jsonObjectArgumentCaptor = ArgumentCaptor.forClass(JSONObject.class);
        //when
        eventProcessor.parseEventsJson(events, configProcessorAdapter);

        //then
        verify(configProcessorAdapter, times(3)).runConfigProcessorFunctionByName(stringArgumentCaptor.capture(), jsonObjectArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).contains("concatenateValue", "addAttribute", "map");
    }

    private HashMap<String, String[]> convertDMaaPStreamsPropertyToMap(String streamIdsProperty) {
        java.util.HashMap<String, String[]> domainToStreamIdsMapping = new java.util.HashMap<>();
        String[] topics = streamIdsProperty.split("\\|");
        for (String t : topics) {
            String domain = t.split("=")[0];
            String[] streamIds = t.split("=")[1].split(",");
            domainToStreamIdsMapping.put(domain, streamIds);
        }
        return HashMap.ofAll(domainToStreamIdsMapping);
    }

}


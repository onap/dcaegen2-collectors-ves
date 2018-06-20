/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.att.nsa.cmdLine.NsaCommandLineUtil;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequest;
import com.att.nsa.drumlin.till.nv.impl.nvReadableStack;
import com.att.nsa.drumlin.till.nv.impl.nvReadableTable;
import com.att.nsa.drumlin.till.nv.rrNvReadable.loadException;
import com.att.nsa.drumlin.till.nv.rrNvReadable.missingReqdSetting;
import com.att.nsa.security.authenticators.SimpleAuthenticator;
import com.att.nsa.security.db.simple.NsaSimpleApiKey;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.dcae.commonFunction.CommonStartup.QueueFullException;
import org.onap.dcae.commonFunction.event.publishing.EventPublisher;
import org.onap.dcae.restapi.RestfulCollectorServlet;


public class TestCommonStartup {

    @Test
    public void testParseCLIArguments() {
        // given
        String args[] = {"-a", "aa"};
        Map<String, String> argMap = NsaCommandLineUtil.processCmdLine(args, true);
        // when
        nvReadableStack settings = new nvReadableStack();
        settings.push(new nvReadableTable(argMap));

        // then
        assertEquals(settings.getString("a", "default"), "aa");
    }

    @Test
    public void shouldPutValidVESEventOnProcessingQueueWithoutExceptions() throws IOException, QueueFullException {
        // given
        CommonStartup.fProcessingInputQueue = new LinkedBlockingQueue<>(
            CommonStartup.KDEFAULT_MAXQUEUEDEVENTS);
        JsonElement vesEvent = new JsonParser().parse(new FileReader("src/test/resources/VES_valid.txt"));
        JSONObject validVESEvent = new JSONObject(vesEvent.toString());
        JSONArray jsonArrayMod = new JSONArray().put(validVESEvent);

        // then
        CommonStartup.handleEvents(jsonArrayMod);
    }


    @Test
    public void testParseStreamIdToStreamHashMapping() {
        // given
        CommonStartup.streamid = "fault=sec_fault|syslog=sec_syslog|heartbeat=sec_heartbeat|measurementsForVfScaling=sec_measurement|mobileFlow=sec_mobileflow|other=sec_other|stateChange=sec_statechange|thresholdCrossingAlert=sec_thresholdCrossingAlert|voiceQuality=ves_voicequality|sipSignaling=ves_sipsignaling";
        EventProcessor eventProcessor = new EventProcessor(new AtomicReference<>(mock(EventPublisher.class)));

        // when
        Map<String, String[]> streamHashMapping = EventProcessor.streamidHash;

        // then
        assertEquals(streamHashMapping.get("fault")[0], "sec_fault");
        assertEquals(streamHashMapping.get("measurementsForVfScaling")[0], "sec_measurement");
    }

    @Test
    public void testAuthListHandler() throws loadException, missingReqdSetting {
        // given
        final nvReadableStack settings = new nvReadableStack();

        String user1 = "secureid";
        String password1Hashed = "IWRjYWVSb2FkbTEyMyEt";
        String password1UnHashed = decode("IWRjYWVSb2FkbTEyMyEt");
        String user2 = "sample1";
        String password2Hashed = "c2FtcGxlMQ";

        String authlist = user1 + "," + password1Hashed + "|" + user2 + "," + password2Hashed;

        RestfulCollectorServlet rsv = new RestfulCollectorServlet(settings);

        DrumlinRequest drumlinRequestMock = mock(DrumlinRequest.class);

        String basicHeaderForUser1 = "Basic " + encode(user1, password1UnHashed);
        when(drumlinRequestMock.getFirstHeader("Authorization")).thenReturn(basicHeaderForUser1);

        // when
        SimpleAuthenticator simpleAuthenticator = (SimpleAuthenticator) rsv.AuthlistHandler(authlist);
        NsaSimpleApiKey authentic = simpleAuthenticator.isAuthentic(drumlinRequestMock);

        // then
        assertEquals(authentic.getSecret(), password1UnHashed);
    }

    private String decode(String hashedPassword) {
        return new String(Base64.getDecoder().decode(hashedPassword.getBytes()));
    }

    private String encode(String user1, String password1UnHashed) {
        return Base64.getEncoder().encodeToString((user1 + ":" + password1UnHashed).getBytes());
    }

}



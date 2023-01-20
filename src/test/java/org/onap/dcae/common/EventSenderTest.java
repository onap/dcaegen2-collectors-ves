/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2017,2023 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2018-2021 Nokia. All rights reserved.
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
package org.onap.dcae.common;

import io.vavr.collection.HashMap;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.dcae.common.model.StndDefinedNamespaceParameterNotDefinedException;
import org.onap.dcae.common.model.VesEvent;
import org.onap.dcae.common.publishing.DMaaPEventPublisher;
import org.onap.dcae.restapi.EventValidatorException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.Silent.class)
public class EventSenderTest {

  @Mock
  private DMaaPEventPublisher eventPublisher;


  @Test
  public void shouldNotSendEventWhenStreamIdIsNotDefined() throws IOException, URISyntaxException {
    // given
    EventSender eventSender = givenConfiguredEventSender(HashMap.empty());
    List<VesEvent> eventToSend = createEventToSend("/eventsAfterTransformation/ves7_valid_event.json");

    // when
    assertThatExceptionOfType(EventValidatorException.class)
            .isThrownBy(() -> eventSender.send(eventToSend));

    // then
    verifyThatEventWasNotSendAtStream();
  }

  @Test
  public void shouldSendStdDefinedEventAtStreamAssignedToEventDomain() throws IOException, URISyntaxException {
    // given
    EventSender eventSender = givenConfiguredEventSender(
            HashMap.of("3GPP-FaultSupervision", "ves-3gpp-fault-supervision")
    );
    List<VesEvent> eventToSend = createEventToSend("/eventsAfterTransformation/ves_stdnDefined_valid.json");

    // when
    eventSender.send(eventToSend);

    // then
    verifyThatEventWasSendAtStream("ves-3gpp-fault-supervision");
  }

  @Test
  public void shouldNotSendStndEventWhenStreamIsNotDefined() throws IOException, URISyntaxException {
    // given
    EventSender eventSender = givenConfiguredEventSender(HashMap.empty());
    List<VesEvent> eventToSend = createEventToSend("/eventsAfterTransformation/ves_stdnDefined_valid.json");

    // when
    assertThatExceptionOfType(EventValidatorException.class)
            .isThrownBy(() -> eventSender.send(eventToSend));

    // then
    verifyThatEventWasNotSendAtStream();
  }

  @Test
  public void shouldReportThatNoStndDefinedNamespaceParameterIsDefinedInEvent() throws IOException, URISyntaxException {
    // given
    EventSender eventSender = givenConfiguredEventSender(HashMap.empty());
    List<VesEvent> eventToSend = createEventToSend(
            "/eventsAfterTransformation/ves_stdnDefined_missing_namespace_invalid.json"
    );

    // when
    assertThatExceptionOfType(StndDefinedNamespaceParameterNotDefinedException.class)
            .isThrownBy(() -> eventSender.send(eventToSend));

    // then
    verifyThatEventWasNotSendAtStream();
  }

  private List<VesEvent> createEventToSend(String path) throws IOException, URISyntaxException {
    String event = JsonDataLoader.loadContent(path);
    return givenEventToSend(event);
  }

  private EventSender givenConfiguredEventSender(io.vavr.collection.Map<String, String> streamIds) {
    return new EventSender(eventPublisher, streamIds);
  }

  private List<VesEvent> givenEventToSend(String event) {
    JSONObject jsonObject = new JSONObject(event);
    return List.of(new VesEvent(jsonObject));
  }

  private void verifyThatEventWasNotSendAtStream() {
      verify(eventPublisher,never()).sendEvent(any(),any());
  }

  private void verifyThatEventWasSendAtStream(String s) {
      verify(eventPublisher).sendEvent(any(), eq(s));
    }
}

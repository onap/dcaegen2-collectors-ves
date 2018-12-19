package org.onap.dcae.commonFunction;


import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vavr.collection.Map;
import junit.framework.Assert;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.vavr.collection.HashMap;

import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.commonFunction.event.publishing.EventPublisher;

@RunWith(MockitoJUnitRunner.Silent.class)
public class EventSenderTest {


  private String event = "{\"VESversion\":\"v7\",\"VESuniqueId\":\"fd69d432-5cd5-4c15-9d34-407c81c61c6a-0\",\"event\":{\"commonEventHeader\":{\"startEpochMicrosec\":1544016106000000,\"eventId\":\"fault33\",\"timeZoneOffset\":\"UTC+00.00\",\"priority\":\"Normal\",\"version\":\"4.0.1\",\"nfVendorName\":\"Ericsson\",\"reportingEntityName\":\"1\",\"sequence\":1,\"domain\":\"fault\",\"lastEpochMicrosec\":1544016106000000,\"eventName\":\"Fault_KeyFileFault\",\"vesEventListenerVersion\":\"7.0.1\",\"sourceName\":\"1\"},\"faultFields\":{\"eventSeverity\":\"CRITICAL\",\"alarmCondition\":\"KeyFileFault\",\"faultFieldsVersion\":\"4.0\",\"eventCategory\":\"PROCESSINGERRORALARM\",\"specificProblem\":\"License Key File Fault_1\",\"alarmAdditionalInformation\":{\"probableCause\":\"ConfigurationOrCustomizationError\",\"additionalText\":\"test_1\",\"source\":\"ManagedElement=1,SystemFunctions=1,Lm=1\"},\"eventSourceType\":\"Lm\",\"vfStatus\":\"Active\"}}}\n";

  @Mock
  private EventPublisher eventPublisher;
  @Mock
  private ApplicationSettings settings;

  private EventSender eventSender;


  @Test
  public void shouldntSendEventWhenStreamIdsIsEmpty() {
    when(settings.dMaaPStreamsMapping()).thenReturn(HashMap.empty());
    eventSender = new EventSender(eventPublisher, settings );
    eventSender.send(new JSONObject(event));
    verify(eventPublisher,never()).sendEvent(any(),any());
  }

  @Test
  public void shouldSendEvent() {
    Map<String, String[]> streams = HashMap.of("fault", new String[]{"ves-fault", "fault-ves"});
    when(settings.dMaaPStreamsMapping()).thenReturn(streams);
    eventSender = new EventSender(eventPublisher, settings );
    eventSender.send(new JSONObject(event));
    verify(eventPublisher, times(2)).sendEvent(any(),any());
  }

  @Test
  public void eventShouldNotContainVESversionField() {
    Map<String, String[]> streams = HashMap.of("fault", new String[]{"ves-fault", "fault-ves"});
    when(settings.dMaaPStreamsMapping()).thenReturn(streams);
    when(settings.eventTransformingEnabled()).thenReturn(true);
    eventSender = new EventSender(eventPublisher, settings );
    eventSender.send(new JSONObject(event));
    assertEquals(false, eventSender.getObject().toString().contains("VESversion"));
  }

  @Test
  public void eventShouldContainCollectorTimeStamp() {
    Map<String, String[]> streams = HashMap.of("fault", new String[]{"ves-fault", "fault-ves"});
    when(settings.dMaaPStreamsMapping()).thenReturn(streams);
    when(settings.eventTransformingEnabled()).thenReturn(true);
    eventSender = new EventSender(eventPublisher, settings );
    eventSender.send(new JSONObject(event));
    assertEquals(true, eventSender.getObject().toString().contains("collectorTimeStamp"));
  }

}
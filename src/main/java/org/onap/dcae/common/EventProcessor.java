///*-
// * ============LICENSE_START=======================================================
// * PROJECT
// * ================================================================================
// * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
// * ================================================================================
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// * ============LICENSE_END=========================================================
// */
//
//package org.onap.dcae.common;
//
//import com.att.nsa.clock.SaClock;
//import com.att.nsa.logging.LoggingContext;
//import com.att.nsa.logging.log4j.EcompFields;
//import org.json.JSONObject;
//import org.onap.dcae.VesApplication;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class EventProcessor implements Runnable {
//
//    private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);
//    private EventSender eventSender;
//
//    public EventProcessor(EventSender eventSender) {
//        this.eventSender = eventSender;
//    }
//
//    @Override
//    public void run() {
//        try {
//          while (true){
//            JSONObject event = VesApplication.fProcessingInputQueue.take();
//            log.info("QueueSize:" + VesApplication.fProcessingInputQueue.size() + "\tEventProcessor\tRemoving element: " + event);
//            setLoggingContext(event);
//            log.debug("event.VESuniqueId" + event.get("VESuniqueId") + "event.commonEventHeader.domain:" + eventSender.getDomain(event));
//            eventSender.send(event);
//            log.debug("Message published" + event);
//          }
//        } catch (InterruptedException e) {
//            log.error("EventProcessor InterruptedException" + e.getMessage());
//            Thread.currentThread().interrupt();
//        }
//    }
//
//  private void setLoggingContext(JSONObject event) {
//        LoggingContext localLC = VESLogger.getLoggingContextForThread(event.get("VESuniqueId").toString());
//        localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
//    }
//}
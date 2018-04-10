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

import com.att.nsa.clock.SaClock;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class EventProcessor implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);
	private static final String EVENT_LITERAL = "event";
	private static final String COMMON_EVENT_HEADER = "commonEventHeader";

	private static HashMap<String, String[]> streamid_hash = new HashMap<String, String[]>();
	public JSONObject event;

	public EventProcessor() {
		log.debug("EventProcessor: Default Constructor");

		String[] list = CommonStartup.streamid.split("\\|");
		for (String aList : list) {
			String domain = aList.split("=")[0];
			// String streamIdList[] = list[i].split("=")[1].split(",");
			String[] streamIdList = aList.substring(aList.indexOf('=') + 1).split(",");

			log.debug(String.format("Domain: %s streamIdList:%s", domain, Arrays.toString(streamIdList)));
			streamid_hash.put(domain, streamIdList);
		}

	}

	@Override
	public void run() {

		try {

			event = CommonStartup.fProcessingInputQueue.take();
			

			// EventPublisher Ep=new EventPublisher();
			while (event != null) {
				// As long as the producer is running we remove elements from
				// the queue.
				log.info("QueueSize:" + CommonStartup.fProcessingInputQueue.size()+  "\tEventProcessor\tRemoving element: " + event );
				
				String uuid = event.get("VESuniqueId").toString();
				LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid);
				localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());

				log.debug("event.VESuniqueId" + event.get("VESuniqueId") + "event.commonEventHeader.domain:"
						+ event.getJSONObject(EVENT_LITERAL).getJSONObject(COMMON_EVENT_HEADER).getString("domain"));
				String[] streamIdList = streamid_hash
						.get(event.getJSONObject(EVENT_LITERAL).getJSONObject(COMMON_EVENT_HEADER).getString("domain"));
				log.debug("streamIdList:" + streamIdList);

				if (streamIdList.length == 0) {
					log.error("No StreamID defined for publish - Message dropped" + event);
				} else {
					for (String aStreamIdList : streamIdList) {
						log.info("Invoking publisher for streamId:" + aStreamIdList);
						this.overrideEvent();
						//EventPublisher.getInstance(aStreamIdList).sendEvent(event);
						EventPublisherHash.getInstance().sendEvent(event, aStreamIdList);

					}
				}
				log.debug("Message published" + event);
				event = CommonStartup.fProcessingInputQueue.take();

			}
		} catch (InterruptedException e) {
			log.error("EventProcessor InterruptedException" + e.getMessage());
			Thread.currentThread().interrupt();
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void overrideEvent() {
		// Set collector timestamp in event payload before publish
		final Date currentTime = new Date();
		final SimpleDateFormat sdf = new SimpleDateFormat("EEE, MM dd yyyy hh:mm:ss z");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

		JSONObject collectorTimeStamp = new JSONObject().put("collectorTimeStamp", sdf.format(currentTime));
		JSONObject commonEventHeaderkey = event.getJSONObject(EVENT_LITERAL).getJSONObject(COMMON_EVENT_HEADER);
		commonEventHeaderkey.put("internalHeaderFields", collectorTimeStamp);
		event.getJSONObject(EVENT_LITERAL).put(COMMON_EVENT_HEADER, commonEventHeaderkey);

		if (CommonStartup.eventTransformFlag == 1) {
			// read the mapping json file
			final JsonParser parser = new JsonParser();
			FileReader fr = null;
			try {
				fr = new FileReader("./etc/eventTransform.json");
				final JsonArray jo = (JsonArray) parser.parse(fr);
				log.info("parse eventTransform.json");
				// now convert to org.json
				final String jsonText = jo.toString();
				final JSONArray topLevel = new JSONArray(jsonText);
				// log.info("topLevel == " + topLevel);

				Class[] paramJSONObject = new Class[1];
				paramJSONObject[0] = JSONObject.class;
				// load VESProcessors class at runtime
				Class cls = Class.forName("org.onap.dcae.commonFunction.ConfigProcessors");
				Constructor constr = cls.getConstructor(paramJSONObject);
				Object obj = constr.newInstance(event);

				for (int j = 0; j < topLevel.length(); j++) {
					JSONObject filterObj = topLevel.getJSONObject(j).getJSONObject("filter");
					Method method = cls.getDeclaredMethod("isFilterMet", paramJSONObject);
					boolean filterMet = (boolean) method.invoke(obj, filterObj);
					if (filterMet) {
						final JSONArray processors = topLevel.getJSONObject(j).getJSONArray("processors");

						// call the processor method
						for (int i = 0; i < processors.length(); i++) {
							final JSONObject processorList = processors.getJSONObject(i);
							final String functionName = processorList.getString("functionName");
							final JSONObject args = processorList.getJSONObject("args");
					

							log.info(String.format("functionName==%s | args==%s", functionName, args));
							// reflect method call
							method = cls.getDeclaredMethod(functionName, paramJSONObject);
							method.invoke(obj, args);
						}
					}
				}

			} catch (Exception e) {

				log.error("EventProcessor Exception" + e.getMessage() + e + e.getCause());
			} finally {
				// close the file
				if (fr != null) {
					try {
						fr.close();
					} catch (IOException e) {
						log.error("Error closing file reader stream : " + e.toString());
					}

				}
			}
		}
		// Remove VESversion from event. This field is for internal use and must
		// be removed after use.
		if (event.has("VESversion"))
			event.remove("VESversion");

		log.debug("Modified event:" + event);

	}
}

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

package org.onap.dcae.restapi.endpoints;

import com.att.nsa.apiServer.endpoints.NsaBaseEndpoint;
import com.att.nsa.clock.SaClock;
import com.att.nsa.drumlin.service.framework.context.DrumlinRequestContext;
import com.att.nsa.drumlin.service.standards.HttpStatusCodes;
import com.att.nsa.drumlin.service.standards.MimeTypes;
import com.att.nsa.logging.LoggingContext;
import com.att.nsa.logging.log4j.EcompFields;
import com.att.nsa.security.db.simple.NsaSimpleApiKey;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onap.dcae.commonFunction.CommonStartup;
import org.onap.dcae.commonFunction.CommonStartup.QueueFullException;
import org.onap.dcae.commonFunction.VESLogger;
import org.onap.dcae.restapi.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventReceipt extends NsaBaseEndpoint {

	private static final Logger log = LoggerFactory.getLogger(EventReceipt.class);
	private static final String MESSAGE = " Message:";
	static String valresult;
	static JSONObject customerror;

	public static void receiveVESEvent(DrumlinRequestContext ctx) {
		// the request body carries events. assume for now it's an array
		// of json objects that fits in memory. (See cambria's parsing for
		// handling large messages)

		NsaSimpleApiKey retkey = null;


		JSONObject jsonObject;
		FileReader fr = null;
		InputStream istr = null;
		int arrayFlag = 0;
		String vesVersion = null;
		String userId=null;

		try {


			istr = ctx.request().getBodyStream();
			jsonObject = new JSONObject(new JSONTokener(istr));

			log.info("ctx getPathInContext: " + ctx.request().getPathInContext());
			Pattern p = Pattern.compile("(v\\d+)");
			Matcher m = p.matcher(ctx.request().getPathInContext());

			if (m.find()) {
				log.info("VES version:" + m.group());
				vesVersion = m.group();
				m = null;
				p = null;

			}
			
			final UUID uuid = UUID.randomUUID();
			LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid);
			localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());
			
			if (ctx.request().getPathInContext().contains("eventBatch")) {
				CommonStartup.inlog.info(ctx.request().getRemoteAddress() + "VESUniqueID-Prefix:" + uuid
						+ " VES Batch Input Messsage: " + jsonObject);
				log.info(ctx.request().getRemoteAddress() + "VESUniqueID-Prefix:" + uuid + " VES Batch Input Messsage: "
						+ jsonObject);
				arrayFlag = 1;
			} else {
				CommonStartup.inlog.info(
						ctx.request().getRemoteAddress() + "VESUniqueID:" + uuid + " Input Messsage: " + jsonObject);
				log.info(ctx.request().getRemoteAddress() + "VESUniqueID:" + uuid + " Input Messsage: " + jsonObject);

			}

			try {
				if (CommonStartup.authflag == 1) {
					userId = getUser (ctx);
					retkey = NsaBaseEndpoint.getAuthenticatedUser(ctx);
				}
			} catch (NullPointerException x) {
				//log.info("Invalid user request :" + userId + " FROM " + ctx.request().getRemoteAddress() + " " +  ctx.request().getContentType() + MESSAGE + jsonObject);
				log.info(String.format("Unauthorized request %s FROM %s %s %s %s", getUser(ctx), ctx.request().getRemoteAddress(), ctx.request().getContentType(), MESSAGE,	jsonObject));
				CommonStartup.eplog.info("EVENT_RECEIPT_FAILURE: Unauthorized user" + userId +  x);
				respondWithCustomMsginJson(ctx, ApiException.UNAUTHORIZED_USER);
				return;
			}
			
			Boolean ErrorStatus = false;
			ErrorStatus = schemaCheck( retkey,  arrayFlag, jsonObject,  vesVersion,  ctx,  uuid);
			if (ErrorStatus)
			{
				return;
			}

		} catch (JSONException | NullPointerException | IOException x) {
			log.error(String.format("Couldn't parse JSON Array - HttpStatusCodes.k400_badRequest%d%s%s",
					HttpStatusCodes.k400_badRequest, MESSAGE, x.getMessage()));
			CommonStartup.eplog.info("EVENT_RECEIPT_FAILURE: Invalid user request " + x);
			respondWithCustomMsginJson(ctx, ApiException.INVALID_JSON_INPUT);
			return;
		} catch (QueueFullException e) {
			log.error("Collector internal queue full  :" + e.getMessage(), e);
			CommonStartup.eplog.info("EVENT_RECEIPT_FAILURE: QueueFull" + e);
			respondWithCustomMsginJson(ctx, ApiException.NO_SERVER_RESOURCES);
			return;
		} finally {
			if (fr != null) {
				safeClose(fr);
			}

			if (istr != null) {
				safeClose(istr);
			}
		}
		log.info("MessageAccepted and k200_ok to be sent");
		ctx.response().sendErrorAndBody(HttpStatusCodes.k200_ok, "Message Accepted", MimeTypes.kAppJson);
	}
	
	
	public static String getUser( DrumlinRequestContext ctx){
		String authorization = null;
		authorization = ctx.request().getFirstHeader("Authorization");
		 if (authorization != null && authorization.startsWith("Basic")) {
		        // Authorization: Basic base64credentials
		        String base64Credentials = authorization.substring("Basic".length()).trim();
		        String credentials = new String(Base64.getDecoder().decode(base64Credentials),
		                Charset.forName("UTF-8"));
		        // credentials = username:password
		        final String[] values = credentials.split(":",2);
		        log.debug("User:" + values[0].toString() + " Pwd:" + values[1].toString());
		        return values[0].toString();
		 }
		 return null;
		
	}
	public static Boolean schemaCheck(NsaSimpleApiKey retkey, int arrayFlag,JSONObject jsonObject, String vesVersion,  DrumlinRequestContext ctx, UUID uuid) throws JSONException, QueueFullException, IOException
	{
		JSONArray jsonArray;
		JSONArray jsonArrayMod = new JSONArray();
		JSONObject event;
		Boolean ErrorStatus=false;
		FileReader fr;
		if (retkey != null || CommonStartup.authflag == 0) {
			if (CommonStartup.schemaValidatorflag > 0) {
				if ((arrayFlag == 1) && (jsonObject.has("eventList") && (!jsonObject.has("event")))
						|| ((arrayFlag == 0) && (!jsonObject.has("eventList") && (jsonObject.has("event"))))) {
					fr = new FileReader(schemaFileVersion(vesVersion));
					String schema = new JsonParser().parse(fr).toString();

					valresult = CommonStartup.schemavalidate(jsonObject.toString(), schema);
					if (valresult.equals("true")) {
						log.info("Validation successful");
					} else if (valresult.equals("false")) {
						log.info("Validation failed");
						respondWithCustomMsginJson(ctx, ApiException.SCHEMA_VALIDATION_FAILED);
						ErrorStatus=true;
						return ErrorStatus;
					} else {
						log.error("Validation errored" + valresult);
						respondWithCustomMsginJson(ctx, ApiException.INVALID_JSON_INPUT);
						ErrorStatus=true;
						return ErrorStatus;
					}
				} else {
					log.info("Validation failed");
					respondWithCustomMsginJson(ctx, ApiException.SCHEMA_VALIDATION_FAILED);
					ErrorStatus=true;
					return ErrorStatus;
				}
				if (arrayFlag == 1) {
					jsonArray = jsonObject.getJSONArray("eventList");
					log.info("Validation successful for all events in batch");
					for (int i = 0; i < jsonArray.length(); i++) {
						event = new JSONObject().put("event", jsonArray.getJSONObject(i));
						event.put("VESuniqueId", uuid + "-" + i);
						event.put("VESversion", vesVersion);
						jsonArrayMod.put(event);
					}
					log.info("Modified jsonarray:" + jsonArrayMod.toString());
				} else {
					jsonObject.put("VESuniqueId", uuid);
					jsonObject.put("VESversion", vesVersion);
					jsonArrayMod = new JSONArray().put(jsonObject);
				}
			}

			// reject anything that's not JSON
			if (!ctx.request().getContentType().equalsIgnoreCase("application/json")) {
				log.info(String.format("Rejecting request with content type %s Message:%s",
						ctx.request().getContentType(), jsonObject));
				respondWithCustomMsginJson(ctx, ApiException.INVALID_CONTENT_TYPE);
				ErrorStatus=true;
				return ErrorStatus;
			}

			CommonStartup.handleEvents(jsonArrayMod);
		} else {
			log.info(String.format("Unauthorized request %s FROM %s %s %s %s", getUser(ctx), ctx.request().getRemoteAddress(), ctx.request().getContentType(), MESSAGE,
					jsonObject));
			respondWithCustomMsginJson(ctx, ApiException.UNAUTHORIZED_USER);
			ErrorStatus=true;
			return ErrorStatus;
		}
		return ErrorStatus;
	}

	public static void respondWithCustomMsginJson(DrumlinRequestContext ctx, ApiException apiException) {
		ctx.response()
			.sendErrorAndBody(apiException.httpStatusCode,
				apiException.toJSON().toString(), MimeTypes.kAppJson);
	}

	public static void safeClose(FileReader fr) {
		if (fr != null) {
			try {
				fr.close();
			} catch (IOException e) {
				log.error("Error closing file reader stream : " + e);
			}
		}

	}

	public static void safeClose(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
				log.error("Error closing Input stream : " + e);
			}
		}

	}

	public static String schemaFileVersion(String version) {
		String filename = null;

		if (CommonStartup.schemaFileJson.has(version)) {
			filename = CommonStartup.schemaFileJson.getString(version);
		} else {
			filename = CommonStartup.schemaFileJson.getString("v5");

		}
		log.info(String.format("VESversion: %s Schema File:%s", version, filename));
		return filename;

	}

}


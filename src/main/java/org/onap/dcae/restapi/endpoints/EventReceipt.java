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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onap.dcae.commonFunction.CommonStartup;
import org.onap.dcae.commonFunction.CommonStartup.QueueFullException;
import org.onap.dcae.commonFunction.CustomExceptionLoader;
import org.onap.dcae.commonFunction.VESLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        JSONArray jsonArray;
        JSONArray jsonArrayMod = new JSONArray();
        JSONObject event;
        JSONObject jsonObject;
        FileReader fr = null;
        InputStream istr = null;
        int arrayFlag = 0;
        String vesVersion = null;

        try {
            //System.out.print("Version string:" + version);

            // String br = new BufferedReader(new InputStreamReader(ctx.request().getBodyStream())).readLine();
            // JsonElement msg = new JsonParser().parse(new BufferedReader(new InputStreamReader(ctx.request().getBodyStream())).readLine());
            // jsonArray = new JSONArray ( new JSONTokener ( ctx.request().getBodyStream () ) );

            log.debug("Request recieved :" + ctx.request().getRemoteAddress());
            istr = ctx.request().getBodyStream();
            jsonObject = new JSONObject(new JSONTokener(istr));

            log.info("ctx getPathInContext: " + ctx.request().getPathInContext());
            Pattern p = Pattern.compile("(v\\d+)");
            Matcher m = p.matcher(ctx.request().getPathInContext());

            if (m.find()) {
                log.info("VES version:" + m.group());
                vesVersion = m.group();
            }
            if (ctx.request().getPathInContext().contains("eventBatch")) {
                CommonStartup.inlog.info(
                    ctx.request().getRemoteAddress() + "VES Batch Input Messsage: " + jsonObject);
                log.info(
                    ctx.request().getRemoteAddress() + "VES Batch Input Messsage: " + jsonObject);
                arrayFlag = 1;
            } else {
                CommonStartup.inlog
                    .info(ctx.request().getRemoteAddress() + "Input Messsage: " + jsonObject);
                log.info(ctx.request().getRemoteAddress() + "Input Messsage: " + jsonObject);

            }

            UUID uuid = UUID.randomUUID();
            LoggingContext localLC = VESLogger.getLoggingContextForThread(uuid);
            localLC.put(EcompFields.kBeginTimestampMs, SaClock.now());

            try {
                if (CommonStartup.authflag == 1) {
                    retkey = NsaBaseEndpoint.getAuthenticatedUser(ctx);
                }
            } catch (NullPointerException x) {
                log.info(
                    "Invalid user request " + ctx.request().getContentType() + MESSAGE
                        + jsonObject);
                CommonStartup.eplog.info("EVENT_RECEIPT_FAILURE: Unauthorized user" + x);
                respondWithCustomMsginJson(ctx, HttpStatusCodes.k401_unauthorized, "Invalid user");
                return;
            }

            if (retkey != null || CommonStartup.authflag == 0) {
                if (CommonStartup.schema_Validatorflag > 0) {

                    //fr = new FileReader(CommonStartup.schemaFile);
                    fr = new FileReader(schemaFileVersion(vesVersion));
                    String schema = new JsonParser().parse(fr).toString();

                    valresult = CommonStartup.schemavalidate(jsonObject.toString(), schema);
                    if ("true".equals(valresult)) {
                        log.info("Validation successful");
                    } else if ("false".equals(valresult)) {
                        log.info("Validation failed");
                        respondWithCustomMsginJson(ctx, HttpStatusCodes.k400_badRequest,
                            "Schema validation failed");

                        return;
                    } else {
                        log.error("Validation errored" + valresult);
                        respondWithCustomMsginJson(ctx, HttpStatusCodes.k400_badRequest,
                            "Couldn't parse JSON object");
                        return;

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

                        log.info("Modified jsonarray:" + jsonArrayMod);

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
                    respondWithCustomMsginJson(ctx, HttpStatusCodes.k400_badRequest,
                        "Incorrect message content-type; only accepts application/json messages");
                    return;
                }

                CommonStartup.handleEvents(jsonArrayMod);
            } else {
                log.info(
                    String.format("Unauthorized request %s%s%s", ctx.request().getContentType(),
                        MESSAGE, jsonObject));
                respondWithCustomMsginJson(ctx, HttpStatusCodes.k401_unauthorized,
                    "Unauthorized user");
                return;
            }
        } catch (JSONException | NullPointerException | IOException x) {
            log.error(String
                .format("Couldn't parse JSON Array - HttpStatusCodes.k400_badRequest%d%s%s",
                    HttpStatusCodes.k400_badRequest, MESSAGE, x.getMessage()));
            CommonStartup.eplog.info("EVENT_RECEIPT_FAILURE: Invalid user request " + x);
            respondWithCustomMsginJson(ctx, HttpStatusCodes.k400_badRequest,
                "Couldn't parse JSON object");
            return;
        } catch (QueueFullException e) {
            log.error("Collector internal queue full  :" + e.getMessage(), e);
            CommonStartup.eplog.info("EVENT_RECEIPT_FAILURE: QueueFull" + e);
            respondWithCustomMsginJson(ctx, HttpStatusCodes.k503_serviceUnavailable, "Queue full");
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
        ctx.response()
            .sendErrorAndBody(HttpStatusCodes.k200_ok, "Message Accepted", MimeTypes.kAppJson);
    }


    public static void respondWithCustomMsginJson(DrumlinRequestContext ctx, int sc, String msg) {
        String[] str;
        String exceptionType = "GeneralException";

        str = CustomExceptionLoader.LookupMap(String.valueOf(sc), msg);
        log.info("Post CustomExceptionLoader.LookupMap" + str);

        if (str != null) {

            if (str[0].matches("SVC")) {
                exceptionType = "ServiceException";
            } else if (str[1].matches("POL")) {
                exceptionType = "PolicyException";
            }

            JSONObject jb = new JSONObject().put("requestError",
                new JSONObject().put(exceptionType,
                    new JSONObject().put("MessagID", str[0]).put("text", str[1])));

            log.debug("Constructed json error : " + jb);
            ctx.response().sendErrorAndBody(sc, jb.toString(), MimeTypes.kAppJson);
        } else {
            JSONObject jb = new JSONObject().put("requestError",
                new JSONObject()
                    .put(exceptionType, new JSONObject().put("Status", sc).put("Error", msg)));
            ctx.response().sendErrorAndBody(sc, jb.toString(), MimeTypes.kAppJson);
        }

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
            filename = CommonStartup.schemaFile;
        }
        log.info(String.format("VESversion: %s Schema File:%s", version, filename));
        return filename;

    }

}

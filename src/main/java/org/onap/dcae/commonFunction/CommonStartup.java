/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

import com.att.nsa.apiServer.ApiServer;
import com.att.nsa.apiServer.ApiServerConnector;
import com.att.nsa.apiServer.endpoints.NsaBaseEndpoint;
import com.att.nsa.drumlin.till.nv.rrNvReadable;
import com.att.nsa.drumlin.till.nv.rrNvReadable.loadException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.apache.catalina.LifecycleException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.CLIUtils;
import org.onap.dcae.commonFunction.event.publishing.DMaaPConfigurationParser;
import org.onap.dcae.commonFunction.event.publishing.EventPublisher;
import org.onap.dcae.restapi.RestfulCollectorServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class CommonStartup extends NsaBaseEndpoint implements Runnable {

    private static final Logger metriclog = LoggerFactory.getLogger("com.att.ecomp.metrics");
    public static final Logger inlog = LoggerFactory.getLogger("org.onap.dcae.commonFunction.input");
    static final Logger oplog = LoggerFactory.getLogger("org.onap.dcae.commonFunction.output");
    public static final Logger eplog = LoggerFactory.getLogger("org.onap.dcae.commonFunction.error");

    static int maxQueueEvent = 1024 * 4;
    public static boolean schemaValidatorflag = false;
    public static boolean authflag = false;
    static boolean eventTransformFlag = true;
    public static JSONObject schemaFileJson;
    static String cambriaConfigFile;
    public static io.vavr.collection.Map<String , String [] > streamID;

    static LinkedBlockingQueue<JSONObject> fProcessingInputQueue;
    private static ApiServer fTomcatServer = null;
    private static final Logger log = LoggerFactory.getLogger(CommonStartup.class);

    private CommonStartup(ApplicationSettings settings) throws loadException, IOException, rrNvReadable.missingReqdSetting {
        final List<ApiServerConnector> connectors = new LinkedList<>();

        if (!settings.authorizationEnabled()) {
            connectors.add(new ApiServerConnector.Builder(settings.httpPort()).secure(false).build());
        }

        final int securePort = settings.httpsPort();
        final String keystoreFile = settings.keystoreFileLocation();
        final String keystorePasswordFile = settings.keystorePasswordFileLocation();
        final String keyAlias = settings.keystoreAlias();

        if (settings.authorizationEnabled()) {
            String keystorePassword = readFile(keystorePasswordFile);
            connectors.add(new ApiServerConnector.Builder(securePort).secure(true)
                    .keystorePassword(keystorePassword).keystoreFile(keystoreFile).keyAlias(keyAlias).build());

        }

        schemaValidatorflag = settings.jsonSchemaValidationEnabled();
        maxQueueEvent = settings.maximumAllowedQueuedEvents();
        if (schemaValidatorflag) {
            schemaFileJson = settings.jsonSchema();

        }
        authflag = settings.authorizationEnabled();
        cambriaConfigFile = settings.cambriaConfigurationFileLocation();
        streamID = settings.dMaaPStreamsMapping();
        eventTransformFlag = settings.eventTransformingEnabled();

        fTomcatServer = new ApiServer.Builder(connectors, new RestfulCollectorServlet(settings)).encodeSlashes(true)
            .name("collector").build();
    }

    public static void main(String[] args) {
        try {

            fProcessingInputQueue = new LinkedBlockingQueue<>(CommonStartup.maxQueueEvent);

            VESLogger.setUpEcompLogging();

            CommonStartup cs = new CommonStartup(new ApplicationSettings(args, CLIUtils::processCmdLine));

            Thread commonStartupThread = new Thread(cs);
            commonStartupThread.start();

            EventProcessor ep = new EventProcessor(EventPublisher.createPublisher(oplog,
                                                                                  DMaaPConfigurationParser
                                                                                      .parseToDomainMapping(Paths.get(cambriaConfigFile))
                                                                                      .get()));
            ExecutorService executor = Executors.newFixedThreadPool(20);
            for (int i = 0; i < 20; ++i) {
                executor.execute(ep);
            }
        } catch (Exception e) {
            CommonStartup.eplog.error("Fatal error during application startup", e);
            throw new RuntimeException(e);
        }
    }

    public void run() {
        try {
            fTomcatServer.start();
            fTomcatServer.await();
        } catch (LifecycleException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class QueueFullException extends Exception {

        private static final long serialVersionUID = 1L;
    }

    public static void handleEvents(JSONArray a) throws QueueFullException, JSONException {
        CommonStartup.metriclog.info("EVENT_PUBLISH_START");
        for (int i = 0; i < a.length(); i++) {
            if (!fProcessingInputQueue.offer(a.getJSONObject(i))) {
                throw new QueueFullException();
            }
        }
        log.debug("CommonStartup.handleEvents:EVENTS has been published successfully!");
        CommonStartup.metriclog.info("EVENT_PUBLISH_END");
    }

    private static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        String pwd = new String(encoded);
        return pwd.substring(0, pwd.length() - 1);
    }

    public static String validateAgainstSchema(String jsonData, String jsonSchema) {
        ProcessingReport report;
        String result = "false";

        try {
            log.trace("Schema validation for event:" + jsonData);
            JsonNode schemaNode = JsonLoader.fromString(jsonSchema);
            JsonNode data = JsonLoader.fromString(jsonData);
            JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            JsonSchema schema = factory.getJsonSchema(schemaNode);
            report = schema.validate(data);
        } catch (JsonParseException e) {
            log.error("validateAgainstSchema:JsonParseException for event:" + jsonData);
            return e.getMessage();
        } catch (ProcessingException e) {
            log.error("validateAgainstSchema:Processing exception for event:" + jsonData);
            return e.getMessage();
        } catch (IOException e) {
            log.error(
                "validateAgainstSchema:IO exception; something went wrong trying to read json data for event:" + jsonData);
            return e.getMessage();
        }
        if (report != null) {
            for (ProcessingMessage pm : report) {
                log.trace("Processing Message: " + pm.getMessage());
            }
            result = String.valueOf(report.isSuccess());
        }
        try {
            log.debug("Validation Result:" + result + " Validation report:" + report);
        } catch (NullPointerException e) {
            log.error("validateAgainstSchema:NullpointerException on report");
        }
        return result;
    }


}

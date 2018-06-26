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
import com.att.nsa.cmdLine.NsaCommandLineUtil;
import com.att.nsa.drumlin.service.framework.DrumlinServlet;
import com.att.nsa.drumlin.till.nv.impl.nvPropertiesFile;
import com.att.nsa.drumlin.till.nv.impl.nvReadableStack;
import com.att.nsa.drumlin.till.nv.impl.nvReadableTable;
import com.att.nsa.drumlin.till.nv.rrNvReadable;
import com.att.nsa.drumlin.till.nv.rrNvReadable.loadException;
import com.att.nsa.drumlin.till.nv.rrNvReadable.missingReqdSetting;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.catalina.LifecycleException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onap.dcae.commonFunction.event.publishing.DMaaPConfigurationParser;
import org.onap.dcae.commonFunction.event.publishing.EventPublisher;
import org.onap.dcae.restapi.RestfulCollectorServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonStartup extends NsaBaseEndpoint implements Runnable {

    private static final String KCONFIG = "c";
    private static final String KSETTING_PORT = "collector.service.port";
    private static final int KDEFAULT_PORT = 8080;
    private static final String KSETTING_SECUREPORT = "collector.service.secure.port";
    private static final int KDEFAULT_SECUREPORT = -1;
    private static final String KSETTING_KEYSTOREPASSFILE = "collector.keystore.passwordfile";
    private static final String KDEFAULT_KEYSTOREPASSFILE = "../etc/passwordfile";
    private static final String KSETTING_KEYSTOREFILE = "collector.keystore.file.location";
    private static final String KDEFAULT_KEYSTOREFILE = "../etc/keystore";
    private static final String KSETTING_KEYALIAS = "collector.keystore.alias";
    private static final String KDEFAULT_KEYALIAS = "tomcat";
    private static final String KSETTING_DMAAPCONFIGS = "collector.dmaapfile";
    private static final String[] KDEFAULT_DMAAPCONFIGS = new String[]{"/etc/DmaapConfig.json"};
    private static final String KSETTING_SCHEMAVALIDATOR = "collector.schema.checkflag";
    private static final int KDEFAULT_SCHEMAVALIDATOR = -1;
    private static final String KSETTING_SCHEMAFILE = "collector.schema.file";
    private static final String KDEFAULT_SCHEMAFILE = "{\"v5\":\"./etc/CommonEventFormat_28.3.json\"}";
    private static final String KSETTING_DMAAPSTREAMID = "collector.dmaap.streamid";
    private static final String KSETTING_AUTHFLAG = "header.authflag";
    private static final int KDEFAULT_AUTHFLAG = 0;
    private static final String KSETTING_EVENTTRANSFORMFLAG = "event.transform.flag";
    private static final int KDEFAULT_EVENTTRANSFORMFLAG = 1;
    private static final Logger metriclog = LoggerFactory.getLogger("com.att.ecomp.metrics");
    public static final Logger inlog = LoggerFactory.getLogger("org.onap.dcae.commonFunction.input");
    static final Logger oplog = LoggerFactory.getLogger("org.onap.dcae.commonFunction.output");
    public static final Logger eplog = LoggerFactory.getLogger("org.onap.dcae.commonFunction.error");

    public static final String KSETTING_AUTHLIST = "header.authlist";
    static final int KDEFAULT_MAXQUEUEDEVENTS = 1024 * 4;
    public static int schemaValidatorflag = -1;
    public static int authflag = 1;
    static int eventTransformFlag = 1;
    public static JSONObject schemaFileJson;
    static String cambriaConfigFile;
    public static String streamID;

    static LinkedBlockingQueue<JSONObject> fProcessingInputQueue;
    private static ApiServer fTomcatServer = null;
    private static final Logger log = LoggerFactory.getLogger(CommonStartup.class);

    private CommonStartup(rrNvReadable settings) throws loadException, IOException, rrNvReadable.missingReqdSetting {
        final List<ApiServerConnector> connectors = new LinkedList<>();

        if (settings.getInt(KSETTING_PORT, KDEFAULT_PORT) > 0) {
            connectors.add(new ApiServerConnector.Builder(settings.getInt(KSETTING_PORT, KDEFAULT_PORT)).secure(false)
                               .build());
        }

        final int securePort = settings.getInt(KSETTING_SECUREPORT, KDEFAULT_SECUREPORT);
        final String keystoreFile = settings.getString(KSETTING_KEYSTOREFILE, KDEFAULT_KEYSTOREFILE);
        final String keystorePasswordFile = settings.getString(KSETTING_KEYSTOREPASSFILE, KDEFAULT_KEYSTOREPASSFILE);
        final String keyAlias = settings.getString(KSETTING_KEYALIAS, KDEFAULT_KEYALIAS);

        if (securePort > 0) {
            String keystorePassword = readFile(keystorePasswordFile);
            connectors.add(new ApiServerConnector.Builder(securePort).secure(true)
                               .keystorePassword(keystorePassword).keystoreFile(keystoreFile).keyAlias(keyAlias).build());

        }

        schemaValidatorflag = settings.getInt(KSETTING_SCHEMAVALIDATOR, KDEFAULT_SCHEMAVALIDATOR);
        if (schemaValidatorflag > 0) {
            String schemaFile = settings.getString(KSETTING_SCHEMAFILE, KDEFAULT_SCHEMAFILE);
            schemaFileJson = new JSONObject(schemaFile);

        }
        authflag = settings.getInt(CommonStartup.KSETTING_AUTHFLAG, CommonStartup.KDEFAULT_AUTHFLAG);
        String[] currentConfigFile = settings.getStrings(KSETTING_DMAAPCONFIGS, KDEFAULT_DMAAPCONFIGS);
        cambriaConfigFile = currentConfigFile[0];
        streamID = settings.getString(KSETTING_DMAAPSTREAMID, null);
        eventTransformFlag = settings.getInt(KSETTING_EVENTTRANSFORMFLAG, KDEFAULT_EVENTTRANSFORMFLAG);

        fTomcatServer = new ApiServer.Builder(connectors, new RestfulCollectorServlet(settings)).encodeSlashes(true)
            .name("collector").build();
    }

    public static void main(String[] args) {
        try {
            final Map<String, String> argMap = NsaCommandLineUtil.processCmdLine(args, true);
            final String config = NsaCommandLineUtil.getSetting(argMap, KCONFIG, "collector.properties");
            final URL settingStream = DrumlinServlet.findStream(config, CommonStartup.class);

            final nvReadableStack settings = new nvReadableStack();
            settings.push(new nvPropertiesFile(settingStream));
            settings.push(new nvReadableTable(argMap));

            fProcessingInputQueue = new LinkedBlockingQueue<>(CommonStartup.KDEFAULT_MAXQUEUEDEVENTS);

            VESLogger.setUpEcompLogging();

            CommonStartup cs = new CommonStartup(settings);

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

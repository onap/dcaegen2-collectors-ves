/*
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2018 Nokia. All rights reserved.s
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

package org.onap.dcae;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Function1;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static io.vavr.API.Tuple;
import static java.lang.String.format;
import static java.nio.file.Files.readAllBytes;
import static java.util.Arrays.stream;

/**
 * Abstraction over application configuration.
 * Its job is to provide easily discoverable (by method names lookup) and type safe access to configuration properties.
 */
@Component
public class ApplicationSettings {

    private static final Logger log = LoggerFactory.getLogger(ApplicationSettings.class);
    private static final String FALLBACK_VES_VERSION = "v5";
    private final String appInvocationDir;
    private final String configurationFileLocation;
    private final PropertiesConfiguration properties = new PropertiesConfiguration();
    private final Map<String, JsonSchema> loadedJsonSchemas;

    public ApplicationSettings(String[] args, Function1<String[], Map<String, String>> argsParser) {
        this(args, argsParser, System.getProperty("user.dir"));
    }

    public ApplicationSettings(String[] args, Function1<String[], Map<String, String>> argsParser, String appInvocationDir) {
        this.appInvocationDir = appInvocationDir;
        properties.setDelimiterParsingDisabled(true);
        Map<String, String> parsedArgs = argsParser.apply(args);
        configurationFileLocation = findOutConfigurationFileLocation(parsedArgs);
        loadPropertiesFromFile();
        parsedArgs.filterKeys(k -> !k.equals("c")).forEach(this::updateProperty);
        loadedJsonSchemas = loadJsonSchemas();
    }

    private void loadPropertiesFromFile() {
        try {
            properties.load(configurationFileLocation);
        } catch (ConfigurationException ex) {
            log.error("Cannot load properties cause:", ex);
            throw new RuntimeException(ex);
        }
    }

    public Map<String, String> validAuthorizationCredentials() {
        return prepareUsersMap(properties.getString("header.authlist", null));
    }

    private Map<String, String> prepareUsersMap(@Nullable String allowedUsers) {
        return allowedUsers == null ? HashMap.empty() : List.ofAll(stream(allowedUsers.split("\\|")))
                .toMap(t -> t.split(",")[0].trim(), t -> new String(Base64.getDecoder().decode(t.split(",")[1])).trim());
    }

    private String findOutConfigurationFileLocation(Map<String, String> parsedArgs) {
        return prependWithUserDirOnRelative(parsedArgs.get("c").getOrElse("etc/collector.properties"));
    }

    public Path configurationFileLocation() {
        return Paths.get(configurationFileLocation);
    }

    public int maximumAllowedQueuedEvents() {
        return properties.getInt("collector.inputQueue.maxPending", 1024 * 4);
    }

    public boolean jsonSchemaValidationEnabled() {
        return properties.getInt("collector.schema.checkflag", -1) > 0;
    }

    public boolean authorizationEnabled() {
        return properties.getInt("header.authflag", 0) > 0;
    }

    public JsonSchema jsonSchema(String version) {
        return loadedJsonSchemas.get(version)
                .orElse(loadedJsonSchemas.get(FALLBACK_VES_VERSION))
                .getOrElseThrow(() -> new IllegalStateException("No fallback schema present in application."));
    }

    private Map<String, JsonSchema> loadJsonSchemas() {
        return jsonSchema().toMap().entrySet().stream()
                .map(versionToFilePath -> readSchemaForVersion(versionToFilePath))
                .collect(HashMap.collector());
    }

    private Tuple2<String, JsonSchema> readSchemaForVersion(java.util.Map.Entry<String, Object> versionToFilePath) {
        try {
            String schemaContent = new String(
                    readAllBytes(Paths.get(versionToFilePath.getValue().toString())));
            JsonNode schemaNode = JsonLoader.fromString(schemaContent);
            JsonSchema schema = JsonSchemaFactory.byDefault().getJsonSchema(schemaNode);
            return Tuple(versionToFilePath.getKey(), schema);
        } catch (IOException | ProcessingException e) {
            throw new RuntimeException("Could not read schema from path: " + versionToFilePath.getValue(), e);
        }
    }

    public int httpPort() {
        return properties.getInt("collector.service.port", 8080);
    }

    public int httpsPort() {
        return properties.getInt("collector.service.secure.port", 8443);
    }

    public int configurationUpdateFrequency() {
        return properties.getInt("collector.dynamic.config.update.frequency", 5);
    }

    public boolean httpsEnabled() {
        return httpsPort() > 0;
    }

    public boolean eventTransformingEnabled() {
        return properties.getInt("event.transform.flag", 1) > 0;
    }

    public String keystorePasswordFileLocation() {
        return prependWithUserDirOnRelative(properties.getString("collector.keystore.passwordfile", "etc/passwordfile"));
    }

    public String keystoreFileLocation() {
        return prependWithUserDirOnRelative(properties.getString("collector.keystore.file.location", "etc/keystore"));
    }

    public String keystoreAlias() {
        return properties.getString("collector.keystore.alias", "tomcat");
    }

    public String exceptionConfigFileLocation() {
        return properties.getString("exceptionConfig", null);
    }

    public String dMaaPConfigurationFileLocation() {
        return prependWithUserDirOnRelative(properties.getString("collector.dmaapfile", "etc/DmaapConfig.json"));
    }

    public Map<String, String[]> dMaaPStreamsMapping() {
        String streamIdsProperty = properties.getString("collector.dmaap.streamid", null);
        if (streamIdsProperty == null) {
            return HashMap.empty();
        } else {
            return convertDMaaPStreamsPropertyToMap(streamIdsProperty);
        }
    }

    private JSONObject jsonSchema() {
        return new JSONObject(properties.getString("collector.schema.file",
                format("{\"%s\":\"etc/CommonEventFormat_28.4.1.json\"}", FALLBACK_VES_VERSION)));
    }

    private Map<String, String[]> convertDMaaPStreamsPropertyToMap(String streamIdsProperty) {
        java.util.HashMap<String, String[]> domainToStreamIdsMapping = new java.util.HashMap<>();
        String[] topics = streamIdsProperty.split("\\|");
        for (String t : topics) {
            String domain = t.split("=")[0];
            String[] streamIds = t.split("=")[1].split(",");
            domainToStreamIdsMapping.put(domain, streamIds);
        }
        return HashMap.ofAll(domainToStreamIdsMapping);
    }

    private void updateProperty(String key, String value) {
        if (properties.containsKey(key)) {
            properties.setProperty(key, value);
        } else {
            properties.addProperty(key, value);
        }
    }

    private String prependWithUserDirOnRelative(String filePath) {
        if (!Paths.get(filePath).isAbsolute()) {
            filePath = Paths.get(appInvocationDir, filePath).toString();
        }
        return filePath;
    }

    @VisibleForTesting
    String getStringDirectly(String key) {
        return properties.getString(key);
    }
}


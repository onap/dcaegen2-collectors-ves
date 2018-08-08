package org.onap.dcae;

/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2018 Nokia. All rights reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.onap.dcae.CLIUtils.processCmdLine;
import static org.onap.dcae.TestingUtilities.createTemporaryFile;

public class ApplicationSettingsTest {

    @Test
    public void shouldMakeApplicationSettingsOutOfCLIArguments() {
        // given
        String[] cliArguments = {"-param1", "param1value", "-param2", "param2value"};

        // when
        ApplicationSettings configurationAccessor = new ApplicationSettings(cliArguments, CLIUtils::processCmdLine);
        String param1value = configurationAccessor.getStringDirectly("param1");
        String param2value = configurationAccessor.getStringDirectly("param2");

        // then
        assertEquals("param1value", param1value);
        assertEquals("param2value", param2value);
    }

    @Test
    public void shouldMakeApplicationSettingsOutOfCLIArgumentsAndAConfigurationFile()
            throws IOException {
        // given
        File tempConfFile = File.createTempFile("doesNotMatter", "doesNotMatter");
        Files.write(tempConfFile.toPath(), Arrays.asList("section.subSection1=abc", "section.subSection2=zxc"));
        tempConfFile.deleteOnExit();
        String[] cliArguments = {"-param1", "param1value", "-param2", "param2value", "-c", tempConfFile.toString()};

        // when
        ApplicationSettings configurationAccessor = new ApplicationSettings(cliArguments, CLIUtils::processCmdLine);
        String param1value = configurationAccessor.getStringDirectly("param1");
        String param2value = configurationAccessor.getStringDirectly("param2");
        String fromFileParam1Value = configurationAccessor.getStringDirectly("section.subSection1");
        String fromFileParam2Value = configurationAccessor.getStringDirectly("section.subSection2");

        // then
        assertEquals("param1value", param1value);
        assertEquals("param2value", param2value);
        assertEquals("abc", fromFileParam1Value);
        assertEquals("zxc", fromFileParam2Value);
    }

    @Test
    public void shouldCLIArgumentsOverrideConfigFileParameters() throws IOException {
        // given
        String[] cliArguments = {"-section.subSection1", "abc"};
        File tempConfFile = File.createTempFile("doesNotMatter", "doesNotMatter");
        Files.write(tempConfFile.toPath(), singletonList("section.subSection1=zxc"));
        tempConfFile.deleteOnExit();

        // when
        ApplicationSettings configurationAccessor = new ApplicationSettings(cliArguments, CLIUtils::processCmdLine);
        String actuallyOverridenByCLIParam = configurationAccessor.getStringDirectly("section.subSection1");

        // then
        assertEquals("abc", actuallyOverridenByCLIParam);
    }

    @Test
    public void shouldReturnHTTPPort() throws IOException {
        // when
        int applicationPort = fromTemporaryConfiguration("collector.service.port=8090")
                .httpPort();

        // then
        assertEquals(8090, applicationPort);
    }

    @Test
    public void shouldReturnDefaultHTTPPort() throws IOException {
        // when
        int applicationPort = fromTemporaryConfiguration().httpPort();

        // then
        assertEquals(8080, applicationPort);
    }

    @Test
    public void shouldReturnIfHTTPSIsEnabled() throws IOException {
        // when
        boolean httpsEnabled = fromTemporaryConfiguration("collector.service.secure.port=8443")
                .httpsEnabled();

        // then
        assertTrue(httpsEnabled);
    }

    @Test
    public void shouldReturnIfHTTPIsEnabled() throws IOException {
        // when
        boolean httpsEnabled = fromTemporaryConfiguration("collector.service.port=8080").httpsEnabled();
        // then
        assertTrue(httpsEnabled);
    }

    @Test
    public void shouldByDefaultHTTPSBeDisabled() throws IOException {
        // when
        boolean httpsEnabled = fromTemporaryConfiguration().httpsEnabled();

        // then
        assertTrue(httpsEnabled);
    }

    @Test
    public void shouldReturnHTTPSPort() throws IOException {
        // when
        int httpsPort = fromTemporaryConfiguration("collector.service.secure.port=8443")
                .httpsPort();

        // then
        assertEquals(8443, httpsPort);
    }

    @Test
    public void shouldReturnConfigurationUpdateInterval() throws IOException {
        // when
        int updateFrequency = fromTemporaryConfiguration("collector.dynamic.config.update.frequency=10")
                .configurationUpdateFrequency();

        // then
        assertEquals(10, updateFrequency);
    }

    @Test
    public void shouldReturnDefaultConfigurationUpdateInterval() throws IOException {
        // when
        int updateFrequency = fromTemporaryConfiguration()
                .configurationUpdateFrequency();

        // then
        assertEquals(5, updateFrequency);
    }

    @Test
    public void shouldReturnLocationOfThePasswordFile() throws IOException {
        // when
        String passwordFileLocation = fromTemporaryConfiguration("collector.keystore.passwordfile=/somewhere/password").keystorePasswordFileLocation();

        // then
        assertEquals(sanitizePath("/somewhere/password"), passwordFileLocation);
    }

    @Test
    public void shouldReturnDefaultLocationOfThePasswordFile() throws IOException {
        // when
        String passwordFileLocation = fromTemporaryConfiguration().keystorePasswordFileLocation();

        // then
        assertEquals(sanitizePath("etc/passwordfile"), passwordFileLocation);
    }

    @Test
    public void shouldReturnLocationOfTheKeystoreFile() throws IOException {
        // when
        String keystoreFileLocation = fromTemporaryConfiguration("collector.keystore.file.location=/somewhere/keystore")
                .keystoreFileLocation();

        // then
        assertEquals(sanitizePath("/somewhere/keystore"), keystoreFileLocation);
    }

    @Test
    public void shouldReturnLocationOfTheDefaultKeystoreFile() throws IOException {
        // when
        String keystoreFileLocation = fromTemporaryConfiguration().keystoreFileLocation();

        // then
        assertEquals(sanitizePath("etc/keystore"), keystoreFileLocation);
    }


    @Test
    public void shouldReturnKeystoreAlias() throws IOException {
        // when
        String keystoreAlias = fromTemporaryConfiguration("collector.keystore.alias=alias").keystoreAlias();

        // then
        assertEquals("alias", keystoreAlias);
    }

    @Test
    public void shouldReturnDefaultKeystoreAlias() throws IOException {
        // when
        String keystoreAlias = fromTemporaryConfiguration().keystoreAlias();

        // then
        assertEquals("tomcat", keystoreAlias);
    }

    @Test
    public void shouldReturnDMAAPConfigFileLocation() throws IOException {
        // when
        String dmaapConfigFileLocation = fromTemporaryConfiguration("collector.dmaapfile=/somewhere/dmaapFile").dMaaPConfigurationFileLocation();

        // then
        assertEquals(sanitizePath("/somewhere/dmaapFile"), dmaapConfigFileLocation);
    }

    @Test
    public void shouldReturnDefaultDMAAPConfigFileLocation() throws IOException {
        // when
        String dmaapConfigFileLocation = fromTemporaryConfiguration().dMaaPConfigurationFileLocation();

        // then
        assertEquals(sanitizePath("etc/DmaapConfig.json"), dmaapConfigFileLocation);
    }

    @Test
    public void shouldReturnMaximumAllowedQueuedEvents() throws IOException {
        // when
        int maximumAllowedQueuedEvents = fromTemporaryConfiguration("collector.inputQueue.maxPending=10000")
                .maximumAllowedQueuedEvents();

        // then
        assertEquals(10000, maximumAllowedQueuedEvents);
    }

    @Test
    public void shouldReturnDefaultMaximumAllowedQueuedEvents() throws IOException {
        // when
        int maximumAllowedQueuedEvents = fromTemporaryConfiguration().maximumAllowedQueuedEvents();

        // then
        assertEquals(1024 * 4, maximumAllowedQueuedEvents);
    }

    @Test
    public void shouldTellIfSchemaValidationIsEnabled() throws IOException {
        // when
        boolean jsonSchemaValidationEnabled = fromTemporaryConfiguration("collector.schema.checkflag=1")
                .jsonSchemaValidationEnabled();

        // then
        assertTrue(jsonSchemaValidationEnabled);
    }

    @Test
    public void shouldByDefaultSchemaValidationBeDisabled() throws IOException {
        // when
        boolean jsonSchemaValidationEnabled = fromTemporaryConfiguration().jsonSchemaValidationEnabled();

        // then
        assertFalse(jsonSchemaValidationEnabled);
    }

    @Test
    public void shouldReturnJSONSchema() throws IOException, ProcessingException {
        // when
        String sampleJsonSchema = "{" +
                "  \"type\": \"object\"," +
                "  \"properties\": {" +
                "     \"state\": { \"type\": \"string\" }" +
                "  }" +
                "}";
        Path temporarySchemaFile = createTemporaryFile(sampleJsonSchema);

        // when
        JsonSchema schema = fromTemporaryConfiguration(String.format("collector.schema.file={\"v1\": \"%s\"}", temporarySchemaFile))
                .jsonSchema("v1");

        // then
        JsonNode incorrectTestObject = new ObjectMapper().readTree("{ \"state\": 1 }");
        JsonNode correctTestObject = new ObjectMapper().readTree("{ \"state\": \"hi\" }");
        assertFalse(schema.validate(incorrectTestObject).isSuccess());
        assertTrue(schema.validate(correctTestObject).isSuccess());
    }

    @Test
    public void shouldReturnExceptionConfigFileLocation() throws IOException {
        // when
        String exceptionConfigFileLocation = fromTemporaryConfiguration("exceptionConfig=/somewhere/exceptionFile")
                .exceptionConfigFileLocation();

        // then
        assertEquals("/somewhere/exceptionFile", exceptionConfigFileLocation);
    }

    @Test
    public void shouldReturnDefaultExceptionConfigFileLocation() throws IOException {
        // when
        String exceptionConfigFileLocation = fromTemporaryConfiguration().exceptionConfigFileLocation();

        // then
        assertNull(exceptionConfigFileLocation);
    }


    @Test
    public void shouldReturnDMAAPStreamId() throws IOException {
        // given
        Map<String, String[]> expected = HashMap.of(
                "s", new String[]{"something", "something2"},
                "s2", new String[]{"something3"}
        );

        // when
        Map<String, String[]> dmaapStreamID = fromTemporaryConfiguration("collector.dmaap.streamid=s=something,something2|s2=something3")
                .dMaaPStreamsMapping();

        // then
        assertArrayEquals(expected.get("s").get(), Objects.requireNonNull(dmaapStreamID).get("s").get());
        assertArrayEquals(expected.get("s2").get(), Objects.requireNonNull(dmaapStreamID).get("s2").get());
        assertEquals(expected.keySet(), dmaapStreamID.keySet());
    }

    @Test
    public void shouldReturnDefaultDMAAPStreamId() throws IOException {
        // when
        Map<String, String[]> dmaapStreamID = fromTemporaryConfiguration().dMaaPStreamsMapping();

        // then
        assertEquals(dmaapStreamID, HashMap.empty());
    }

    @Test
    public void shouldReturnIfAuthorizationIsEnabled() throws IOException {
        // when
        boolean authorizationEnabled = fromTemporaryConfiguration("header.authflag=1")
                .authorizationEnabled();

        // then
        assertTrue(authorizationEnabled);
    }

    @Test
    public void shouldAuthorizationBeDisabledByDefault() throws IOException {
        // when
        boolean authorizationEnabled = fromTemporaryConfiguration().authorizationEnabled();

        // then
        assertFalse(authorizationEnabled);
    }

    @Test
    public void shouldReturnValidCredentials() throws IOException {
        // when
        Map<String, String> allowedUsers = fromTemporaryConfiguration(
                "header.authlist=pasza,c2ltcGxlcGFzc3dvcmQNCg==|someoneelse,c2ltcGxlcGFzc3dvcmQNCg=="
        ).validAuthorizationCredentials();

        // then
        assertEquals(allowedUsers.get("pasza").get(), "simplepassword");
        assertEquals(allowedUsers.get("someoneelse").get(), "simplepassword");
    }

    @Test
    public void shouldbyDefaultThereShouldBeNoValidCredentials() throws IOException {
        // when
        Map<String, String> userToBase64PasswordDelimitedByCommaSeparatedByPipes = fromTemporaryConfiguration().
                validAuthorizationCredentials();

        // then
        assertTrue(userToBase64PasswordDelimitedByCommaSeparatedByPipes.isEmpty());
    }

    @Test
    public void shouldReturnIfEventTransformingIsEnabled() throws IOException {
        // when
        boolean isEventTransformingEnabled = fromTemporaryConfiguration("event.transform.flag=0")
                .eventTransformingEnabled();

        // then
        assertFalse(isEventTransformingEnabled);
    }

    @Test
    public void shouldEventTransformingBeEnabledByDefault() throws IOException {
        // when
        boolean isEventTransformingEnabled = fromTemporaryConfiguration().eventTransformingEnabled();

        // then
        assertTrue(isEventTransformingEnabled);
    }

    @Test
    public void shouldReturnCambriaConfigurationFileLocation() throws IOException {
        // when
        String cambriaConfigurationFileLocation = fromTemporaryConfiguration("collector.dmaapfile=/somewhere/dmaapConfig")
                .dMaaPConfigurationFileLocation();

        // then
        assertEquals(sanitizePath("/somewhere/dmaapConfig"), cambriaConfigurationFileLocation);
    }

    @Test
    public void shouldReturnDefaultCambriaConfigurationFileLocation() throws IOException {
        // when
        String cambriaConfigurationFileLocation = fromTemporaryConfiguration()
                .dMaaPConfigurationFileLocation();

        // then
        assertEquals(sanitizePath("etc/DmaapConfig.json"), cambriaConfigurationFileLocation);
    }

    private static ApplicationSettings fromTemporaryConfiguration(String... fileLines)
            throws IOException {
        File tempConfFile = File.createTempFile("doesNotMatter", "doesNotMatter");
        Files.write(tempConfFile.toPath(), Arrays.asList(fileLines));
        tempConfFile.deleteOnExit();
        return new ApplicationSettings(new String[]{"-c", tempConfFile.toString()}, args -> processCmdLine(args), "");
    }

    private String sanitizePath(String path) {
        return Paths.get(path).toString();
    }
}
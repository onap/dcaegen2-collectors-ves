/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2018 - 2021 Nokia. All rights reserved.
 * Copyright (C) 2018,2023 AT&T Intellectual Property. All rights reserved.
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.onap.dcae.CLIUtils.processCmdLine;
import static org.onap.dcae.TestingUtilities.createTemporaryFile;

public class ApplicationSettingsTest {
    
    /**
    * The Unix separator character.
    */
    private static final char UNIX_SEPARATOR = '/';
    
    /**
    * The Windows separator character.
    */
    private static final char WINDOWS_SEPARATOR = '\\';    

    private static final String SAMPLE_JSON_SCHEMA = "{"
            + "  \"type\": \"object\","
            + "  \"properties\": {"
            + "     \"state\": { \"type\": \"string\" }"
            + "  }"
            + "}";
    
    /**
    * Converts all separators to the Unix separator of forward slash.
    *
    * @param path  the path to be changed, null ignored
    * @return the updated path
    */    
    private static String separatorsToUnix(final String path) {
        if (path == null || path.indexOf(WINDOWS_SEPARATOR) == -1) {
            return path;
        }
        return path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
    }    

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
        String passwordFileLocation = fromTemporaryConfiguration("collector.keystore.passwordfile=/somewhere/password")
                .keystorePasswordFileLocation();

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
    public void shouldReturnDMAAPConfigFileLocation() throws IOException {
        // when
        String dmaapConfigFileLocation = fromTemporaryConfiguration("collector.dmaapfile=/somewhere/dmaapFile")
                .dMaaPConfigurationFileLocation();

        // then
        assertEquals(sanitizePath("/somewhere/dmaapFile"), dmaapConfigFileLocation);
    }

    @Test
    public void shouldTellIfSchemaValidationIsEnabled() throws IOException {
        // when
        boolean jsonSchemaValidationEnabled = fromTemporaryConfiguration("collector.schema.checkflag=1")
                .eventSchemaValidationEnabled();

        // then
        assertTrue(jsonSchemaValidationEnabled);
    }

    @Test
    public void shouldByDefaultSchemaValidationBeDisabled() throws IOException {
        // when
        boolean jsonSchemaValidationEnabled = fromTemporaryConfiguration().eventSchemaValidationEnabled();

        // then
        assertFalse(jsonSchemaValidationEnabled);
    }

    @Test
    public void shouldReportValidateJSONSchemaErrorWhenJsonContainsIntegerValueNotString() throws IOException, URISyntaxException {
        // when
        Path temporarySchemaFile = createTemporaryFile(SAMPLE_JSON_SCHEMA);
        String normalizedSchemaFile = separatorsToUnix(temporarySchemaFile.toString());
        // when
        JsonSchema schema = fromTemporaryConfiguration(
                String.format("collector.schema.file={\"v1\": \"%s\"}", normalizedSchemaFile))
                .jsonSchema("v1");

        // then
        JsonNode incorrectTestObject = new ObjectMapper().readTree("{ \"state\": 1 }");

        assertFalse(schema.validate(incorrectTestObject).isEmpty());

    }

    @Test
    public void shouldDoNotReportAnyValidateJSONSchemaError() throws IOException {
        // when
        Path temporarySchemaFile = createTemporaryFile(SAMPLE_JSON_SCHEMA);
        String normalizedSchemaFile = separatorsToUnix(temporarySchemaFile.toString());
        // when
        JsonSchema schema = fromTemporaryConfiguration(
                String.format("collector.schema.file={\"v1\": \"%s\"}", normalizedSchemaFile))
                .jsonSchema("v1");

        // then
        JsonNode correctTestObject = new ObjectMapper().readTree("{ \"state\": \"hi\" }");
        assertTrue(schema.validate(correctTestObject).isEmpty());
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
        Map<String, String> expected = HashMap.of(
                "log", "ves-syslog",
                "fault", "ves-fault"
        );

        // when
        Map<String, String> dmaapStreamID = fromTemporaryConfiguration(
                "collector.dmaap.streamid=fault=ves-fault,stream1|log=ves-syslog,stream2,stream3")
                .getDmaapStreamIds();

        // then
        assertEquals(expected.get("log").get(), Objects.requireNonNull(dmaapStreamID).get("log").get());
        assertEquals(expected.get("fault").get(), Objects.requireNonNull(dmaapStreamID).get("fault").get());
        assertEquals(expected.keySet(), dmaapStreamID.keySet());
    }

    @Test
    public void shouldReturnDefaultDMAAPStreamId() throws IOException {
        // when
        Map<String, String> dmaapStreamID = fromTemporaryConfiguration().getDmaapStreamIds();

        // then
        assertEquals(dmaapStreamID, HashMap.empty());
    }

    @Test
    public void shouldAuthorizationBeDisabledByDefault() throws IOException {
        // when
        boolean authorizationEnabled = fromTemporaryConfiguration().authMethod().contains("noAuth");

        // then
        assertTrue(authorizationEnabled);
    }

    @Test
    public void shouldReturnValidCredentials() throws IOException {
        // when
        Map<String, String> allowedUsers = fromTemporaryConfiguration(
                "header.authlist=pasza,c2ltcGxlcGFzc3dvcmQNCg==|someoneelse,c2ltcGxlcGFzc3dvcmQNCg=="
        ).validAuthorizationCredentials();

        // then
        assertEquals("c2ltcGxlcGFzc3dvcmQNCg==", allowedUsers.get("pasza").get());
        assertEquals("c2ltcGxlcGFzc3dvcmQNCg==", allowedUsers.get("someoneelse").get());
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
    public void shouldReturnConfigurationFileLocation() throws IOException {
        // when
        String configurationFileLocation = fromTemporaryConfiguration(
                "collector.dmaapfile=/somewhere/etc/ves-dmaap-config.json")
                .dMaaPConfigurationFileLocation();

        // then
        assertEquals(sanitizePath("/somewhere/etc/ves-dmaap-config.json"), configurationFileLocation);
    }

    @Test
    public void shouldReturnDefaultConfigurationFileLocation() throws IOException {
        // when
        String configurationFileLocation = fromTemporaryConfiguration()
                .dMaaPConfigurationFileLocation();

        // then
        assertEquals(sanitizePath("etc/ves-dmaap-config.json"), configurationFileLocation);
    }

    @Test
    public void shouldReturnDefaultExternalSchemaSchemasLocation() throws IOException {
        //when
        String externalSchemaSchemasLocation = fromTemporaryConfiguration()
                .getExternalSchemaSchemasLocation();

        //then
        assertEquals("./etc/externalRepo", externalSchemaSchemasLocation);
    }

    @Test
    public void shouldReturnDefaultExternalSchemaMappingFileLocation() throws IOException {
        //when
        String externalSchemaMappingFileLocation = fromTemporaryConfiguration()
                .getExternalSchemaMappingFileLocation();

        //then
        assertEquals("./etc/externalRepo/schema-map.json", externalSchemaMappingFileLocation);
    }

    @Test
    public void shouldReturnDefaultExternalSchemaSchemaRefPath() throws IOException {
        //when
        String externalSchemaSchemaRefPath = fromTemporaryConfiguration()
                .getExternalSchemaSchemaRefPath();

        //then
        assertEquals("/event/stndDefinedFields/schemaReference", externalSchemaSchemaRefPath);
    }

    @Test
    public void shouldReturnDefaultExternalSchemaStndDefinedDataPath() throws IOException {
        //when
        String externalSchemaStndDefinedDataPath = fromTemporaryConfiguration()
                .getExternalSchemaStndDefinedDataPath();

        //then
        assertEquals("/event/stndDefinedFields/data", externalSchemaStndDefinedDataPath);
    }

    @Test
    public void shouldReturnEnabledExternalSchema2ndStageValidation() throws IOException {
        //when
        boolean externalSchema2ndStageValidation = fromTemporaryConfiguration("collector.externalSchema.2ndStageValidation=-1")
                .getExternalSchemaValidationCheckflag();

        //then
        assertFalse(externalSchema2ndStageValidation);
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

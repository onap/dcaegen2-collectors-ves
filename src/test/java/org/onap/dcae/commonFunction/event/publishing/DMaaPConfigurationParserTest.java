/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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
package org.onap.dcae.commonFunction.event.publishing;

import static io.vavr.API.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.onap.dcae.commonFunction.event.publishing.DMaaPConfigurationParser.parseDMaaPConfig;

import io.vavr.collection.Map;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
public class DMaaPConfigurationParserTest {

    @Test
    public void testParseCredentialsForGen2() {
        Path path = Paths.get("src/test/resources/testParseDMaaPCredentialsGen2.json");
        Map<String, PublisherConfig> publisherConfigs = parseDMaaPConfig(path);

        PublisherConfig authCredentialsNulls = publisherConfigs.get("auth-credentials-null").getOrNull();
        assertThat(authCredentialsNulls.userName().isEmpty()).isTrue();
        assertThat(authCredentialsNulls.password().isEmpty()).isTrue();
        assertThat(authCredentialsNulls.isSecured()).isFalse();

        PublisherConfig authCredentialsPresent = publisherConfigs.get("auth-credentials-present").getOrNull();
        assertThat(authCredentialsPresent.userName().getOrNull()).isEqualTo("sampleUser");
        assertThat(authCredentialsPresent.password().getOrNull()).isEqualTo("samplePassword");
        assertThat(authCredentialsPresent.isSecured()).isTrue();

        PublisherConfig authCredentialsKeysMissing = publisherConfigs.get("auth-credentials-missing").getOrNull();
        assertThat(authCredentialsKeysMissing.userName().isEmpty()).isTrue();
        assertThat(authCredentialsKeysMissing.password().isEmpty()).isTrue();
        assertThat(authCredentialsKeysMissing.isSecured()).isFalse();
    }


    @Test
    public void testParseCredentialsForLegacy() {
        Path path = Paths.get("src/test/resources/testParseDMaaPCredentialsLegacy.json");
        Map<String, PublisherConfig> publisherConfigs = parseDMaaPConfig(path);

        PublisherConfig authCredentialsNull = publisherConfigs.get("auth-credentials-null").getOrNull();
        assertThat(authCredentialsNull.userName().isEmpty()).isTrue();
        assertThat(authCredentialsNull.password().isEmpty()).isTrue();
        assertThat(authCredentialsNull.isSecured()).isFalse();

        PublisherConfig authCredentialsPresent = publisherConfigs.get("auth-credentials-present").getOrNull();
        assertThat(authCredentialsPresent.userName().getOrNull()).isEqualTo("sampleUser");
        assertThat(authCredentialsPresent.password().getOrNull()).isEqualTo("samplePassword");
        assertThat(authCredentialsPresent.isSecured()).isTrue();

        PublisherConfig authCredentialsMissing = publisherConfigs.get("auth-credentials-missing").getOrNull();
        assertThat(authCredentialsMissing.userName().isEmpty()).isTrue();
        assertThat(authCredentialsMissing.password().isEmpty()).isTrue();
        assertThat(authCredentialsMissing.isSecured()).isFalse();
    }


    @Test
    public void testParseGen2() {
        Path path = Paths.get("src/test/resources/testParseDMaaPGen2.json");
        Map<String, PublisherConfig> publisherConfigs = parseDMaaPConfig(path);

        PublisherConfig withEventsSegment = publisherConfigs.get("event-segments-with-port").getOrNull();
        assertThat(withEventsSegment.destinations()).isEqualTo(List("UEBHOST:3904"));
        assertThat(withEventsSegment.topic()).isEqualTo("DCAE-SE-COLLECTOR-EVENTS-DEV");

        PublisherConfig withOtherSegment = publisherConfigs.get("other-segments-without-ports").getOrNull();
        assertThat(withOtherSegment.destinations()).isEqualTo(List("UEBHOST"));
        assertThat(withOtherSegment.topic()).isEqualTo("DCAE-SE-COLLECTOR-EVENTS-DEV");
    }

    @Test
    public void testParseLegacy() {
        Path exemplaryConfig = Paths.get("src/test/resources/testParseDMaaPLegacy.json");
        Map<String, PublisherConfig> publisherConfigs = DMaaPConfigurationParser.parseDMaaPConfig(exemplaryConfig);

        PublisherConfig urlFirstThenHosts = publisherConfigs.get("url-precedes-hosts").getOrNull();
        assertThat(urlFirstThenHosts.destinations()).isEqualTo(List("127.0.0.1:3904"));
        assertThat(urlFirstThenHosts.topic()).isEqualTo("DCAE-SE-COLLECTOR-EVENTS-DEV");

        PublisherConfig urlKeyMissing = publisherConfigs.get("url-key-missing").getOrNull();
        assertThat(urlKeyMissing.destinations()).isEqualTo(List("h1.att.com", "h2.att.com"));
        assertThat(urlKeyMissing.topic()).isEqualTo("DCAE-SE-COLLECTOR-EVENTS-DEV");

        PublisherConfig urlIsMissing = publisherConfigs.get("url-is-null").getOrNull();
        assertThat(urlIsMissing.destinations()).isEqualTo(List("h1.att.com", "h2.att.com"));
        assertThat(urlIsMissing.topic()).isEqualTo("DCAE-SE-COLLECTOR-EVENTS-DEV");
    }
}
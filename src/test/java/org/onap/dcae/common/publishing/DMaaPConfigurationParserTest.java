/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2017-2018,2023 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2018,2021 Nokia. All rights reserved.
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
package org.onap.dcae.common.publishing;

import static io.vavr.API.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onap.dcae.common.publishing.DMaaPConfigurationParser.parseToDomainMapping;

import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Try;
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
        Try<Map<String, PublisherConfig>> publisherConfigs = parseToDomainMapping(path);

        PublisherConfig authCredentialsNulls = publisherConfigs.get().get("auth-credentials-null").getOrNull();
        assertThat(authCredentialsNulls.userName().isEmpty()).isTrue();
        assertThat(authCredentialsNulls.password().isEmpty()).isTrue();
        assertThat(authCredentialsNulls.isSecured()).isFalse();

        PublisherConfig authCredentialsPresent = publisherConfigs.get().get("auth-credentials-present").getOrNull();
        assertThat(authCredentialsPresent.userName().getOrNull()).isEqualTo("sampleUser");
        assertThat(authCredentialsPresent.password().getOrNull()).isEqualTo("samplePassword");
        assertThat(authCredentialsPresent.isSecured()).isTrue();

        PublisherConfig authCredentialsKeysMissing = publisherConfigs.get().get("auth-credentials-missing").getOrNull();
        assertThat(authCredentialsKeysMissing.userName().isEmpty()).isTrue();
        assertThat(authCredentialsKeysMissing.password().isEmpty()).isTrue();
        assertThat(authCredentialsKeysMissing.isSecured()).isFalse();
    }

    @Test
    public void testParseGen2() {
        Path path = Paths.get("src/test/resources/testParseDMaaPGen2.json");
        Try<Map<String, PublisherConfig>> publisherConfigs = parseToDomainMapping(path);

        PublisherConfig withEventsSegment = publisherConfigs.get().get("event-segments-with-port").getOrNull();
        assertThat(withEventsSegment.destinations()).isEqualTo(List("UEBHOST:3904"));
        assertThat(withEventsSegment.topic()).isEqualTo("DCAE-SE-COLLECTOR-EVENTS-DEV");

        PublisherConfig withOtherSegment = publisherConfigs.get().get("other-segments-without-ports").getOrNull();
        assertThat(withOtherSegment.destinations()).isEqualTo(List("UEBHOST"));
        assertThat(withOtherSegment.topic()).isEqualTo("DCAE-SE-COLLECTOR-EVENTS-DEV");
    }

    @Test
    public void testPubConfigOverrideFunction() {
        List<String> dmaapHost = List.of("dmaapHost:3904");
        PublisherConfig pubConfig = new PublisherConfig(dmaapHost,"VES-OUTPUT", "TEST","TEST");
        assertTrue(pubConfig.equals(pubConfig));
        //negative tests
        assertFalse(pubConfig.equals(null));
        assertFalse(pubConfig.equals(new PublisherConfig(dmaapHost,"VES-OUTPUT1", "TEST1","TEST1")));
    }
}

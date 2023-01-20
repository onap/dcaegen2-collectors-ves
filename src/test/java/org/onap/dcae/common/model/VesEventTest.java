/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
 * Copyright (C) 2023 AT&T Intellectual Property. All rights reserved.
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
package org.onap.dcae.common.model;

import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.Test;
import org.onap.dcae.common.JsonDataLoader;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class VesEventTest {

    private static final String FAULT_DOMAIN = "fault";
    private static final String FAULT_STREAM_ID = "fault";
    private static final String STND_DEFINED_DOMAIN = "stndDefined";
  private static final String STND_DEFINED_STREAM_ID = "3GPP-FaultSupervision";

    @Test
    public void shouldReturnsOriginalDomainForNonStdEvent() throws IOException, URISyntaxException {
        // given
        final VesEvent vesEvent = createVesEvent("/eventsAfterTransformation/ves7_valid_event.json");

        // when/then
        Assertions.assertThat(vesEvent.getDomain()).isEqualTo(FAULT_DOMAIN);
        Assertions.assertThat(vesEvent.getStreamId()).isEqualTo(FAULT_STREAM_ID);
    }

    @Test
    public void shouldReturnsDomainStoredInStndDefinedNamespaceParameterForNonStdEvent() throws IOException, URISyntaxException {
        // given
        final VesEvent vesEvent = createVesEvent("/eventsAfterTransformation/ves_stdnDefined_valid.json");

        // when/then
        Assertions.assertThat(vesEvent.getDomain()).isEqualTo(STND_DEFINED_DOMAIN);
        Assertions.assertThat(vesEvent.getStreamId()).isEqualTo(STND_DEFINED_STREAM_ID);
    }


    @Test
    public void shouldReportThatStndDefinedNamespaceParameterIsNotDefinedInEvent() throws IOException, URISyntaxException {
        // given
        final VesEvent vesEvent = createVesEvent(
                "/eventsAfterTransformation/ves_stdnDefined_missing_namespace_invalid.json"
        );

        // when/then
        // when
        assertThatExceptionOfType(StndDefinedNamespaceParameterNotDefinedException.class)
                .isThrownBy(() -> {
                    vesEvent.getStreamId();
                });
    }

    @Test
    public void shouldReportThatStndDefinedNamespaceParameterHasEmptyValue() throws IOException, URISyntaxException {
        // given
        final VesEvent vesEvent = createVesEvent(
                "/eventsAfterTransformation/ves_stdnDefined_empty_namespace_invalid.json"
        );

        // when/then
        assertThatExceptionOfType(StndDefinedNamespaceParameterHasEmptyValueException.class)
                .isThrownBy(() -> {
                    vesEvent.getStreamId();
                });
    }

    private VesEvent createVesEvent(String path) throws IOException, URISyntaxException {
        String event = JsonDataLoader.loadContent(path);
        return new VesEvent(new JSONObject(event));
    }
}

/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2021 Nokia. All rights reserved.
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
package org.onap.dcae.common.validator;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.onap.dcae.ApplicationSettings;
import org.onap.dcae.common.EventUpdater;
import org.onap.dcae.common.model.VesEvent;
import org.onap.dcae.restapi.EventValidatorException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.onap.dcae.common.validator.BatchEventValidator.executeBatchEventValidation;

class BatchEventValidatorTest {

    private final ApplicationSettings settings = mock(ApplicationSettings.class);
    private final EventUpdater eventUpdater = new EventUpdater(settings);
    private static final String EVENT = "event";
    private static final String EVENT_LIST = "eventList";

    @Test
    void shouldThrowException_whenDomainFieldsHaveDifferentValues() throws IOException {
        //given
        final List<VesEvent> eventList = prepareEventList("src/test/resources/ves7_batch_valid_two_different_domain.json", EVENT_LIST);

        //when
        //then
        assertThrows(EventValidatorException.class, () -> executeBatchEventValidation(eventList));
    }

    @Test
    void shouldNotThrowException_whenDomainFieldsHaveSameValues() throws IOException {
        //given
        final List<VesEvent> eventList = prepareEventList("src/test/resources/ves7_batch_valid.json", EVENT_LIST);

        //when
        //then
        assertDoesNotThrow(() -> executeBatchEventValidation(eventList));
    }

    @Test
    void shouldThrowException_whenStndDefinedNamespaceFieldsHaveDifferentValuesAndDomainsAreStndDefined() throws IOException {
        //given
        final List<VesEvent> eventList = prepareEventList("src/test/resources/ves7_batch_stdnDefined_withDifferentStndDefinedNamespace.json", EVENT_LIST);

        //when
        //then
        assertThrows(EventValidatorException.class, () -> executeBatchEventValidation(eventList));
    }

    @Test
    void shouldNotThrowException_whenStndDefinedNamespaceFieldsHaveSameValuesAndDomainsAreStndDefined() throws IOException {
        //given
        final List<VesEvent> eventList = prepareEventList("src/test/resources/ves7_batch_stdnDefined_withSameStndDefinedNamespace.json", EVENT_LIST);

        //when
        //then
        assertDoesNotThrow(() -> executeBatchEventValidation(eventList));
    }

    @Test
    void shouldNotThrowException_whenSendValidNotBatchEvent() throws IOException {
        //given
        final List<VesEvent> eventList = prepareEventList("src/test/resources/ves_stdnDefined_valid.json", EVENT);

        //when
        //then
        assertDoesNotThrow(() -> executeBatchEventValidation(eventList));
    }

    private List<VesEvent> prepareEventList(String pathToFile, String eventType) throws IOException {
        final VesEvent vesEventFromJson = createVesEventFromJson(pathToFile);
        return eventUpdater.convert(vesEventFromJson, "v7", UUID.randomUUID(), eventType);
    }

    private VesEvent createVesEventFromJson(String pathToFile) throws IOException {
        Path path = Paths.get(pathToFile);
        final List<String> lines = Files.readAllLines(path);
        String str = String.join("", lines);
        return new VesEvent(new JSONObject(str));
    }

}

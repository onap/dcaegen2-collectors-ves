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

import io.vavr.control.Try;
import org.json.JSONException;
import org.onap.dcae.common.model.VesEvent;
import org.onap.dcae.restapi.ApiException;
import org.onap.dcae.restapi.EventValidatorException;

import java.util.List;

import static java.util.stream.Collectors.toSet;
import static org.onap.dcae.restapi.ApiException.DIFFERENT_DOMAIN_FIELDS_IN_BATCH_EVENT;
import static org.onap.dcae.restapi.ApiException.DIFFERENT_STND_DEFINED_NAMESPACE_WHEN_DOMAIN_STND_DEFINED;

public class BatchEventValidator {

    private BatchEventValidator() {
    }

    /**
     * Check if value of domain fields are the same in every event,
     * in case of stndDefined check stndDefinedNamespace fields
     *
     * @param events list of checked ves events
     * @throws EventValidatorException when domain fields value or stndDefinedNamespace fields value are note the same
     */
    public static void executeBatchEventValidation(List<VesEvent> events) throws EventValidatorException {
        if (hasNotEveryEventSameDomain(events)) {
            throw new EventValidatorException(DIFFERENT_DOMAIN_FIELDS_IN_BATCH_EVENT);
        }
        if (isDomainStndDefined(events) && hasNotSameStndDefinedNamespace(events)) {
            throw new EventValidatorException(DIFFERENT_STND_DEFINED_NAMESPACE_WHEN_DOMAIN_STND_DEFINED);
        }
    }

    private static boolean hasNotEveryEventSameDomain(List<VesEvent> events) {
        return events.stream()
                .map(VesEvent::getDomain)
                .collect(toSet())
                .size() != 1;
    }

    private static boolean hasNotSameStndDefinedNamespace(List<VesEvent> events)  {
        return Try.of(() -> isAllStndDefinedNamespace(events))
                .getOrElseThrow(() -> new EventValidatorException(ApiException.MISSING_NAMESPACE_PARAMETER));
    }

    private static boolean isAllStndDefinedNamespace(List<VesEvent> events) {
        return events.stream()
                .map(e -> e.getStndDefinedNamespace().orElse(""))
                .collect(toSet())
                .size() != 1;
    }

    private static boolean isDomainStndDefined(List<VesEvent> events) throws JSONException{
        return events.stream()
                .allMatch((e -> e.getDomain().equals("stndDefined")));
    }
}

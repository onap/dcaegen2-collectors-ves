/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.s
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

public class VesEvent {

    private static final String EVENT_LITERAL = "event";
    private static final String COMMON_EVENT_HEADER = "commonEventHeader";
    private static final String VES_UNIQUE_ID = "VESuniqueId";
    private static final String DOMAIN = "domain";
    private static final String STND_DEFINED_NAMESPACE = "stndDefinedNamespace";
    private static final String STND_DEFINED_DOMAIN = "stndDefined";
    private static final String STND_DEFINED_FIELDS = "stndDefinedFields";
    public static final String SCHEMA_REFERENCE = "schemaReference";

    private final JSONObject event;

    public VesEvent(JSONObject event) {
        this.event = event;
    }

    public String getStreamId() {
        String retVal = getDomain();

        if (isStdDefinedDomain(retVal)) {
            retVal = resolveDomainForStndDefinedEvent();
        }

        return retVal;
    }

    public String getDomain() {
        return getEventHeader().getString(DOMAIN);
    }

    public String getSchemaReference() {
        return getStndDefinedFields().getString(SCHEMA_REFERENCE);
    }

    private JSONObject getStndDefinedFields() {
        return event
                .getJSONObject(EVENT_LITERAL)
                .getJSONObject(STND_DEFINED_FIELDS);
    }

    private String resolveDomainForStndDefinedEvent() {
        final JSONObject eventHeader = getEventHeader();
        if(eventHeader.has(STND_DEFINED_NAMESPACE)) {
            final String domain = eventHeader
                    .getString(STND_DEFINED_NAMESPACE);
            if(domain.isEmpty()) {
                throw new StndDefinedNamespaceParameterHasEmptyValueException();
            }
            return domain;
        } else {
            throw new StndDefinedNamespaceParameterNotDefinedException();
        }
    }

    private JSONObject getEventHeader() {
        return event
                .getJSONObject(EVENT_LITERAL)
                .getJSONObject(COMMON_EVENT_HEADER);
    }

    private boolean isStdDefinedDomain(String domain) {
        return domain.equals(STND_DEFINED_DOMAIN);
    }

    public Object getUniqueId() {
        return event.get(VES_UNIQUE_ID);
    }

    public JSONObject asJsonObject() {
        return new JSONObject(event.toString());
    }

    public JsonNode asJsonNode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(event.toString());
    }

    public boolean hasType(String type) {
        return this.event.has(type);
    }

}
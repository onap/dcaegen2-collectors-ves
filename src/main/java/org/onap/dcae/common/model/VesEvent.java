/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2020-2021 Nokia. All rights reserved.
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
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Optional;

/**
 * This class is a wrapper for JSONObject, that represents VES event.
 * It contains Strings that represents key, that can be found in VES event.
 *
 * @author Zebek
 */
public class VesEvent {

    public static final String VES_UNIQUE_ID = "VESuniqueId";
    private static final String COMMON_EVENT_HEADER = "commonEventHeader";
    private static final String DOMAIN = "domain";
    private static final String STND_DEFINED_NAMESPACE = "stndDefinedNamespace";
    private static final String STND_DEFINED_DOMAIN = "stndDefined";
    private static final String STND_DEFINED_FIELDS = "stndDefinedFields";
    private static final String SCHEMA_REFERENCE = "schemaReference";
    private static final String EVENT = "event";
    private static final String PARTITION_KEY = "sourceName";

    private final JSONObject event;

    public VesEvent(JSONObject event) {
        this.event = event;
    }

    public JsonNode asJsonNode() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(event.toString());
    }

    /**
     * Returns VES event in form of JSON object.
     *
     * @return event in form of json Object
     */
    public JSONObject asJsonObject() {
        return new JSONObject(event.toString());
    }

    /**
     * Returns Domain name from VES event.
     *
     * @return domain
     */
    public String getDomain() {
        return getEventHeader().getString(DOMAIN);
    }

    /**
     * Returns event primary key.
     * @return a primary key
     */
    public String getPK() {
        return event.getJSONObject(EVENT).getJSONObject(COMMON_EVENT_HEADER).get(PARTITION_KEY).toString();
    }

    /**
     * Returns schema reference.
     * @return a schema reference.
     */
    public String getSchemaReference() {
        return getStndDefinedFields().getString(SCHEMA_REFERENCE);
    }

    /**
     * Returns stream ID from VES event.
     *
     * @return stream ID
     */
    public String getStreamId() {
        String retVal = getDomain();

        if (isStdDefinedDomain(retVal)) {
            retVal = resolveDomainForStndDefinedEvent();
        }

        return retVal;
    }

    /**
     * Returns unique ID of VES event.
     *
     * @return unique ID
     */
    public Object getUniqueId() {
        return event.get(VES_UNIQUE_ID);
    }

    /**
     * Returns optional stndDefinedNamespace name from VES event.
     *
     * @return Optional stndDefinedNamespace
     */
    public Optional<String> getStndDefinedNamespace() throws JSONException {
        return isStdDefinedDomain(getDomain()) ? Optional.ofNullable(getEventHeader())
                .map(header -> header.getString(STND_DEFINED_NAMESPACE)) : Optional.empty();
    }

    /**
     * Checks if type of event is same as given in paramaters.
     *
     * @param type name that will be compared with event type
     * @return true or false depending if type given in parameter is same as VES event type
     */
    public boolean hasType(String type) {
        return this.event.has(type);
    }

    /**
     * Remove Json element from event by key.
     * @param key
     */
    public void removeElement(String key) {
        this.event.remove(key);
    }

    @Override
    public String toString() {
        return event.toString();
    }

    private JSONObject getStndDefinedFields() {
        return event
                .getJSONObject(EVENT)
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
                .getJSONObject(EVENT)
                .getJSONObject(COMMON_EVENT_HEADER);
    }

    private boolean isStdDefinedDomain(String domain) {
        return domain.equals(STND_DEFINED_DOMAIN);
    }
}

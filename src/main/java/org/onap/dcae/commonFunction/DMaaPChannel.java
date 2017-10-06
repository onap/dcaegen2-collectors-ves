/*
 * Copyright (c) 2017 Konstantinos Kanonakis, Huawei Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onap.dcae.commonFunction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * DMaaP channel properties class allowing Jackson-assisted parsing from JSON files.
 */
public class DMaaPChannel {

    // TODO: Use appropriate type for each property
    private String name;
    private String basicAuthUsername;
    private String basicAuthPassword;
    private String cambriaTopic;
    private String streamClass;
    private String stripHpId;
    private String type;
    private String cambriaHosts;
    private String cambriaUrl;

    @JsonCreator
    public DMaaPChannel(@JsonProperty("name") String name,
                        @JsonProperty("basicAuthUsername") String basicAuthUsername,
                        @JsonProperty("basicAuthPassword") String basicAuthPassword,
                        @JsonProperty("cambria.topic") String cambriaTopic,
                        @JsonProperty("class") String streamClass,
                        @JsonProperty("stripHpId") String stripHpId,
                        @JsonProperty("type") String type,
                        @JsonProperty("cambria.hosts") String cambriaHosts,
                        @JsonProperty("cambria.url") String cambriaUrl) {
        // TODO: Check for null or indicate required fields in schema
        this.name = removeBackslashes(name);
        this.basicAuthUsername = removeBackslashes(basicAuthUsername);
        this.basicAuthPassword = removeBackslashes(basicAuthPassword);
        this.cambriaTopic = removeBackslashes(cambriaTopic);
        this.streamClass = streamClass;
        this.stripHpId = stripHpId;
        this.type = type;
        this.cambriaHosts = removeBackslashes(cambriaHosts);
        this.cambriaUrl = cambriaUrl != null ?
                removeBackslashes(cambriaUrl): this.cambriaHosts;
    }

    public String getName() {
        return name;
    }

    public String getBasicAuthUsername() {
        return basicAuthUsername;
    }

    public String getBasicAuthPassword() {
        return basicAuthPassword;
    }

    public String getCambriaTopic() {
        return cambriaTopic;
    }

    public String getStreamClass() {
        return streamClass;
    }

    public String getStripHpId() {
        return stripHpId;
    }

    public String getType() {
        return type;
    }

    public String getCambriaHosts() {
        return cambriaHosts;
    }

    public String getCambriaUrl() {
        return cambriaUrl;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("basicAuthUsername", basicAuthUsername)
                .add("basicAuthPassword", basicAuthPassword)
                .add("cambria.topic", cambriaTopic)
                .add("class", streamClass)
                .add("stripHpId", stripHpId)
                .add("type", type)
                .add("cambria.hosts", cambriaHosts)
                .add("cambria.url", cambriaUrl)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, basicAuthUsername, basicAuthPassword,
                            cambriaTopic, streamClass, stripHpId,
                            type, cambriaHosts, cambriaUrl);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DMaaPChannel) {
            final DMaaPChannel other = (DMaaPChannel) obj;
            return Objects.equals(this.name, other.getName())
                    && Objects.equals(this.basicAuthUsername, other.getBasicAuthUsername())
                    && Objects.equals(this.basicAuthPassword, other.getBasicAuthPassword())
                    && Objects.equals(this.cambriaTopic, other.getCambriaTopic())
                    && Objects.equals(this.streamClass, other.getStreamClass())
                    && Objects.equals(this.stripHpId, other.getStripHpId())
                    && Objects.equals(this.type, other.getType())
                    && Objects.equals(this.cambriaHosts, other.getCambriaHosts())
                    && Objects.equals(this.cambriaUrl, other.getCambriaUrl());
        }
        return false;
    }

    private String removeBackslashes(String original) {
        return original == null ? null : original.replace("\"", "");
    }

    /**
     * Returns a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of SubscriptionConfig entities.
     */
    public static final class Builder {

        private String name;
        private String basicAuthUsername;
        private String basicAuthPassword;
        private String cambriaTopic;
        private String streamClass;
        private String stripHpId;
        private String type;
        private String cambriaHosts;
        private String cambriaUrl;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder basicAuthUsername(String basicAuthUsername) {
            this.basicAuthUsername = basicAuthUsername;
            return this;
        }

        public Builder basicAuthPassword(String basicAuthPassword) {
            this.basicAuthPassword = basicAuthPassword;
            return this;
        }

        public Builder cambriaTopic(String cambriaTopic) {
            this.cambriaTopic = cambriaTopic;
            return this;
        }

        public Builder streamClass(String streamClass) {
            this.streamClass = streamClass;
            return this;
        }

        public Builder stripHpId(String stripHpId) {
            this.stripHpId = stripHpId;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder cambriaHosts(String cambriaHosts) {
            this.cambriaHosts = cambriaHosts;
            return this;
        }

        public Builder cambriaUrl(String cambriaUrl) {
            this.cambriaUrl = cambriaUrl;
            return this;
        }

        /**
         * Builds a new DMaaPChannel instance.
         * based on this builder's parameters
         *
         * @return a new DMaaPChannel instance
         */
        public DMaaPChannel build() {
            return new DMaaPChannel(name, basicAuthUsername, basicAuthPassword,
                                    cambriaTopic, streamClass, stripHpId,
                                    type, cambriaHosts, cambriaUrl);
        }
    }
}

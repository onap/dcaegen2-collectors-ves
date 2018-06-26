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
package org.onap.dcae.commonFunction.event.publishing;

import io.vavr.collection.List;
import io.vavr.control.Option;
import java.util.Objects;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
public final class PublisherConfig {

    private final List<String> destinations;
    private final String topic;
    private String userName;
    private String password;

    PublisherConfig(List<String> destinations, String topic) {
        this.destinations = destinations;
        this.topic = topic;
    }

    PublisherConfig(List<String> destinations, String topic, String userName, String password) {
        this.destinations = destinations;
        this.topic = topic;
        this.userName = userName;
        this.password = password;
    }

    List<String> destinations() {
        return destinations;
    }

    String topic() {
        return topic;
    }

    Option<String> userName() {
        return Option.of(userName);
    }

    Option<String> password() {
        return Option.of(password);
    }

    boolean isSecured() {
        return userName().isDefined() && password().isDefined();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PublisherConfig that = (PublisherConfig) o;
        return Objects.equals(destinations, that.destinations) &&
            Objects.equals(topic, that.topic) &&
            Objects.equals(userName, that.userName) &&
            Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destinations, topic, userName, password);
    }

    @Override
    public String toString() {
        return "PublisherConfig{" +
            "destinations=" + destinations +
            ", topic='" + topic + '\'' +
            ", userName='" + userName + '\'' +
            ", password='" + password + '\'' +
            '}';
    }
}

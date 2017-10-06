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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * Class used for Jackson-assisted parsing of a list of DMaaP channels from JSON files.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DMaaPChannels {

    private List<DMaaPChannel> channels;

    @JsonCreator
    public DMaaPChannels(@JsonProperty("channels") List<DMaaPChannel> channels) {
        this.channels = channels;
    }

    public List<DMaaPChannel> getChannels() {
        return channels;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("channels", channels)
                .toString();
    }
}

/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2018 Nokia. All rights reserved.
 * Copyright (C) 2026 Deutsche Telekom Intellectual Property. All rights reserved.
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

package org.onap.dcae.common;

import java.util.Map;
import java.util.function.BiConsumer;
import org.json.JSONObject;

class ConfigProcessorAdapter {

  private static final Map<String, BiConsumer<ConfigProcessors, JSONObject>> DISPATCH =
      Map.of(
          "getValue", ConfigProcessors::getValue,
          "setValue", ConfigProcessors::setValue,
          "suppressEvent", ConfigProcessors::suppressEvent,
          "addAttribute", ConfigProcessors::addAttribute,
          "updateAttribute", ConfigProcessors::updateAttribute,
          "removeAttribute", ConfigProcessors::removeAttribute,
          "map", ConfigProcessors::map,
          "mapAttribute", ConfigProcessors::mapAttribute,
          "concatenateValue", ConfigProcessors::concatenateValue,
          "subtractValue", ConfigProcessors::subtractValue
      );

  private final ConfigProcessors configProcessors;

    ConfigProcessorAdapter(ConfigProcessors configProcessors) {
        this.configProcessors = configProcessors;
    }

    boolean isFilterMet(JSONObject parameter) {
        return configProcessors.isFilterMet(parameter);
    }

    void runConfigProcessorFunctionByName(String functionName, JSONObject parameter) {
        BiConsumer<ConfigProcessors, JSONObject> handler = DISPATCH.get(functionName);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown config processor function: " + functionName);
        }
        handler.accept(configProcessors, parameter);
    }
}

/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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
package org.onap.dcae.controller;

import static io.vavr.API.Try;
import static org.onap.dcae.commonFunction.event.publishing.VavrUtils.enhanceError;
import static org.onap.dcae.commonFunction.event.publishing.VavrUtils.f;
import static org.onap.dcae.controller.Conversions.toJson;
import static org.onap.dcae.controller.Conversions.toJsonArray;

import com.mashape.unirest.http.Unirest;
import io.vavr.control.Try;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ConfigSource {

    private static final Logger log = LoggerFactory.getLogger(ConfigSource.class);

    static Try<JSONObject> getAppConfig(EnvProps envProps) {
        log.info("Fetching app configuration from CBS");
        return callConsulForCBSConfiguration(envProps)
            .peek(strBody -> log.info(f("Received following CBS configuration from Consul '%s'", strBody)))
            .flatMap(strBody -> toJsonArray(strBody))
            .flatMap(json -> withdrawCatalog(json))
            .flatMap(json -> constructFullCBSUrl(json))
            .flatMap(cbsUrl -> callCBSForAppConfig(envProps, cbsUrl))
            .flatMap(strBody -> toJson(strBody))
            .peek(jsonNode -> log.info(f("Received app configuration: '%s'", jsonNode)))
            .onFailure(exc -> log.error("Could not fetch application config", exc));
    }

    private static Try<String> callConsulForCBSConfiguration(EnvProps envProps) {
        return executeGet(envProps.consulProtocol + "://" + envProps.consulHost + ":" +
            envProps.consulPort + "/v1/catalog/service/" + envProps.cbsName)
            .mapFailure(enhanceError("Unable to retrieve CBS configuration from Consul"));
    }

    private static Try<String> constructFullCBSUrl(JSONObject json) {
        return Try(() -> "http://" + json.get("ServiceAddress").toString() + ":" + json.get("ServicePort").toString())
            .mapFailure(enhanceError("ServiceAddress / ServicePort missing from CBS conf: '%s'", json));
    }

    private static Try<JSONObject> withdrawCatalog(JSONArray json) {
        return Try(() -> new JSONObject(json.get(0).toString()))
            .mapFailure(enhanceError("CBS response '%s' is in invalid format,"
                + " most probably is it not a list of configuration objects", json));
    }

    private static Try<String> callCBSForAppConfig(EnvProps envProps, String cbsUrl) {
        log.info("Calling CBS for application config");
        return executeGet(cbsUrl + "/service_component/" + envProps.appName)
            .mapFailure(enhanceError("Unable to fetch configuration from CBS"));
    }


    private static Try<String> executeGet(String url) {
        log.info(f("Calling HTTP GET on url: '%s'", url));
        return Try(() -> Unirest.get(url).asString())
            .mapFailure(enhanceError("Http call (GET '%s') failed.", url))
            .filter(
                res -> res.getStatus() == 200,
                res -> new RuntimeException(f("HTTP call (GET '%s') failed with status %s and body '%s'",
                    url, res.getStatus(), res.getBody())))
            .map(res -> res.getBody())
            .peek(body -> log.info(f("HTTP GET on '%s' returned body '%s'", url, body)));
    }

}

/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
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
package org.onap.dcae.configuration.cbs;

import com.google.gson.JsonObject;
import io.vavr.control.Option;
import java.util.function.Consumer;
import org.json.JSONObject;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClientFactory;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsRequests;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsClientConfiguration;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.CbsRequest;
import org.onap.dcaegen2.services.sdk.rest.services.model.logging.RequestDiagnosticContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CbsClient {

    private static final Logger log = LoggerFactory.getLogger(CbsClient.class);
    private final CbsClientConfiguration cbsClientConfiguration;

    CbsClient(CbsClientConfiguration cbsClientConfiguration) {
        this.cbsClientConfiguration = cbsClientConfiguration;
    }

    public Option<JSONObject> getAppConfig() {
        JsonObject emptyJson = new JsonObject();
        JsonObject jsonObject = CbsClientFactory.createCbsClient(cbsClientConfiguration)
            .flatMap(cbsClient -> cbsClient.get(prepareCbsRequest()))
            .doOnError(handleError())
            .onErrorReturn(emptyJson)
            .block();

        return emptyJson.equals(jsonObject) ? Option.none() : Option.of(new JSONObject(jsonObject.toString()));
    }

    private Consumer<Throwable> handleError() {
        return error -> log.warn("Failed to fetch configuration from CBS " + error.getMessage());
    }

    private CbsRequest prepareCbsRequest() {
        final RequestDiagnosticContext diagnosticContext = RequestDiagnosticContext.create();
        return CbsRequests.getConfiguration(diagnosticContext);
    }
}
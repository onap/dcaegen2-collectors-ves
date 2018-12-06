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
package org.onap.dcae.common.publishing;

import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.cambria.client.CambriaClientBuilders;
import com.att.nsa.cambria.client.CambriaClientBuilders.PublisherBuilder;
import io.vavr.control.Try;

import static io.vavr.API.Try;
import static org.onap.dcae.common.publishing.VavrUtils.enhanceError;
import static org.onap.dcae.common.publishing.VavrUtils.f;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
final class DMaaPPublishersBuilder {

    static Try<CambriaBatchingPublisher> buildPublisher(PublisherConfig config) {
        return Try(() -> builder(config).build())
                .mapFailure(enhanceError(f("DMaaP client builder throws exception for this configuration: '%s'", config)));
    }

    private static PublisherBuilder builder(PublisherConfig config) {
        if (config.isSecured()) {
            return authenticatedBuilder(config);
        } else {
            return unAuthenticatedBuilder(config);
        }
    }

    private static PublisherBuilder authenticatedBuilder(PublisherConfig config) {
        return unAuthenticatedBuilder(config)
                .usingHttps()
                .authenticatedByHttp(config.userName().get(), config.password().get());
    }

    private static PublisherBuilder unAuthenticatedBuilder(PublisherConfig config) {
        return new CambriaClientBuilders.PublisherBuilder()
                .usingHosts(config.destinations().mkString(","))
                .onTopic(config.topic())
                .logSendFailuresAfter(5);
    }
}

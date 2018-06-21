package org.onap.dcae.commonFunction;
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
import com.att.nsa.cambria.client.CambriaBatchingPublisher;
import com.att.nsa.cambria.client.CambriaClientBuilders;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CambriaPublisherFactory {

    private final static Logger log = LoggerFactory.getLogger(CambriaPublisherFactory.class);

    CambriaBatchingPublisher createCambriaPublisher(String streamId)
            throws MalformedURLException, GeneralSecurityException {
        String authpwd = null;
        DmaapPropertyReader reader = DmaapPropertyReader.getInstance(CommonStartup.cambriaConfigFile);
        Map<String, String> dMaaPProperties  = reader.getDmaapProperties();
        String ueburl = dMaaPProperties.get(streamId + ".cambria.url");

        if (ueburl == null) {
            ueburl = dMaaPProperties.get(streamId + ".cambria.hosts");
        }
        String topic = reader.getKeyValue(streamId + ".cambria.topic");
        String authuser = reader.getKeyValue(streamId + ".basicAuthUsername");

        if (authuser != null) {
            authpwd = dMaaPProperties.get(streamId + ".basicAuthPassword");
        }

        if ((authuser != null) && (authpwd != null)) {
            log.debug(String.format("URL:%sTOPIC:%sAuthUser:%sAuthpwd:%s", ueburl, topic, authuser, authpwd));
            return new CambriaClientBuilders.PublisherBuilder().usingHosts(ueburl).onTopic(topic).usingHttps()
                    .authenticatedByHttp(authuser, authpwd).logSendFailuresAfter(5)
                    .build();
        } else {
            log.debug(String.format("URL:%sTOPIC:%s", ueburl, topic));
            return new CambriaClientBuilders.PublisherBuilder().usingHosts(ueburl).onTopic(topic)
                    .logSendFailuresAfter(5)
                    .build();
        }
    }
}

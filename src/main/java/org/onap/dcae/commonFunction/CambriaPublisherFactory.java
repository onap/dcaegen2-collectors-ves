package org.onap.dcae.commonFunction;
/*-
 * ============LICENSE_START=======================================================
 * PROJECT
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CambriaPublisherFactory {

    private static Logger log = LoggerFactory.getLogger(CambriaPublisherFactory.class);

    public CambriaBatchingPublisher createCambriaPublisher(String streamId)
            throws MalformedURLException, GeneralSecurityException {
        String authpwd = null;
        String ueburl = DmaapPropertyReader.getInstance(CommonStartup.cambriaConfigFile).dmaap_hash
                .get(streamId + ".cambria.url");

        if (ueburl == null) {
            ueburl = DmaapPropertyReader.getInstance(CommonStartup.cambriaConfigFile).dmaap_hash
                    .get(streamId + ".cambria.hosts");
        }
        String topic = DmaapPropertyReader.getInstance(CommonStartup.cambriaConfigFile)
                .getKeyValue(streamId + ".cambria.topic");
        String authuser = DmaapPropertyReader.getInstance(CommonStartup.cambriaConfigFile)
                .getKeyValue(streamId + ".basicAuthUsername");

        if (authuser != null) {
            authpwd = DmaapPropertyReader.getInstance(CommonStartup.cambriaConfigFile).dmaap_hash
                    .get(streamId + ".basicAuthPassword");
        }

        if ((authuser != null) && (authpwd != null)) {
            log.debug(String.format("URL:%sTOPIC:%sAuthUser:%sAuthpwd:%s", ueburl, topic, authuser, authpwd));
            return new CambriaClientBuilders.PublisherBuilder().usingHosts(ueburl).onTopic(topic).usingHttps()
                    .authenticatedByHttp(authuser, authpwd).logSendFailuresAfter(5)
                    // .logTo(log)
                    // .limitBatch(100, 10)
                    .build();
        } else {
            log.debug(String.format("URL:%sTOPIC:%s", ueburl, topic));
            return new CambriaClientBuilders.PublisherBuilder().usingHosts(ueburl).onTopic(topic)
                    // .logTo(log)
                    .logSendFailuresAfter(5)
                    // .limitBatch(100, 10)
                    .build();
        }
    }
}

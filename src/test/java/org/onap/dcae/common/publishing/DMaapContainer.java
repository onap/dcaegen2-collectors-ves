/*
 * ============LICENSE_START====================================
 * VES Collector
 * =========================================================
 * Copyright (C) 2019-2021 Nokia. All rights reserved.
 * =========================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=====================================
 */

package org.onap.dcae.common.publishing;

import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.net.URL;

final class DMaapContainer {
    private static final String MR_COMPOSE_RESOURCE_NAME = "dmaap-msg-router/message-router-compose.yml";
    private static final String DOCKER_COMPOSE_FILE_PATH = getDockerComposeFilePath(MR_COMPOSE_RESOURCE_NAME);
    static final int DMAAP_SERVICE_EXPOSED_PORT = 3904;
    static final String DMAAP_SERVICE_NAME = "onap-dmaap";

    private DMaapContainer() {}


    public static DockerComposeContainer createContainerInstance(){
        return new DockerComposeContainer(
                new File(DOCKER_COMPOSE_FILE_PATH))
                .withExposedService(DMAAP_SERVICE_NAME, DMAAP_SERVICE_EXPOSED_PORT)
                .withLocalCompose(true);
    }



    private static String getDockerComposeFilePath(String resourceName) {
        URL resource = DMaapContainer.class.getClassLoader()
                .getResource(resourceName);

        if (resource != null) return resource.getFile();
        else throw new RuntimeException(String
                .format("File %s does not exist", resourceName));
    }
}

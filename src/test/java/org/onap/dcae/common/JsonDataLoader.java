/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.
 * Copyright (C) 2023 AT&T Intellectual Property. All rights reserved.
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class is static and does not have public constructor.
 * It is responsible for data loading for test cases.
 *
 * @author Zebek
 */
public final class JsonDataLoader {

    private JsonDataLoader() {
    }

    /**
     * This method is validating given event using schema and throws exception when event is invalid
     *
     * @param path to file that will be loaded
     * @return contend of the file located under path, given in parameters, as string
     * @throws IOException when file under given path was not found
     * @throws URISyntaxException 
     */
    public static String loadContent(String path) throws IOException, URISyntaxException {
        URI resource = JsonDataLoader.class.getResource(path).toURI();
        Path resourcePath =  Paths.get(resource);
        return new String(Files.readAllBytes(resourcePath));
    }
}

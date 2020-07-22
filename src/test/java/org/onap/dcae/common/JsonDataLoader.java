/*
 * ============LICENSE_START=======================================================
 * VES Collector
 * ================================================================================
 * Copyright (C) 2020 Nokia. All rights reserved.s
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
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This class is static and does not have public constructor.
 * It is responsible for data loading fot test cases.
 *
 * @author Zebek
 */
public final class JsonDataLoader {

    private JsonDataLoader() {
    }

    /**
     * This method is validating given event using schema adn throws exception if event is not valid
     *
     * @param path to file that will be loaded
     * @return contend of the file located under path, given in parameters, as string
     * @throws IOException when file under given path was not found
     */
    public static String loadContent(String path) throws IOException {
        return new String(
                Files.readAllBytes(Paths.get(JsonDataLoader.class.getResource(path).getPath()))
        );
    }
}

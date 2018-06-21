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
package org.onap.dcae.vestest;

import static java.nio.file.Files.readAllBytes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
final class TestingUtilities {

    private TestingUtilities() {
        // utility class, no objects allowed
    }

    static JsonObject readJSONFromFile(Path path) {
        return rethrow(() -> (JsonObject) new JsonParser().parse(new String(readAllBytes(path))));
    }

    static Path createTemporaryFile() {
        return rethrow(() -> {
            Path temporaryDirectory = Files.createTempDirectory("temporaryDirectory");
            Path temporaryFile = TestingUtilities.createFile(temporaryDirectory + "/testFile");
            TestingUtilities.scheduleToBeDeletedAfterTests(temporaryDirectory);
            TestingUtilities.scheduleToBeDeletedAfterTests(temporaryFile);
            return temporaryFile;
        });
    }

    private static Path createFile(String path) {
        return rethrow(() -> Files.createFile(Paths.get(path)));
    }

    private static void scheduleToBeDeletedAfterTests(Path path) {
        path.toFile().deleteOnExit();
    }

    /**
     * Exception in test case usually means there is something wrong, it should never be catched, but rather thrown to
     * be handled by JUnit framework.
     */
    private static <T> T rethrow(CheckedSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @FunctionalInterface
    interface CheckedSupplier<T> {

        T get() throws Exception;
    }


}

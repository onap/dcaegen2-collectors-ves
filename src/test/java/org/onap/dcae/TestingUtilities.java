/*-
 * ============LICENSE_START=======================================================
 * org.onap.dcaegen2.collectors.ves
 * ================================================================================
 * Copyright (C) 2018 Nokia. All rights reserved.
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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
package org.onap.dcae;

import static java.nio.file.Files.readAllBytes;
import static org.assertj.core.api.Assertions.assertThat;

import io.vavr.control.Try;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Java6Assertions;
import org.json.JSONObject;

/**
 * @author Pawel Szalapski (pawel.szalapski@nokia.com)
 */
public final class TestingUtilities {

    private TestingUtilities() {
        // utility class, no objects allowed
    }

    public static void assertJSONObjectsEqual(JSONObject o1, JSONObject o2) {
        assertThat(o1.toString()).isEqualTo(o2.toString());
    }

    public static JSONObject readJSONFromFile(Path path) {
        return rethrow(() -> new JSONObject(readFile(path)));
    }

    public static String readFile(Path path) {
        return rethrow(() -> new String(readAllBytes(path)));
    }

    public static Path createTemporaryFile(String content) {
        return rethrow(() -> {
            File temp = File.createTempFile("ves-collector-tests-created-this-file", ".tmp");
            temp.deleteOnExit();
            Path filePath = Paths.get(temp.toString());
            Files.write(filePath, content.getBytes());
            return filePath;
        });
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

    public static void assertFailureHasInfo(Try any, String... msgPart) {
        Java6Assertions.assertThat(any.isFailure()).isTrue();
        AbstractThrowableAssert<?, ? extends Throwable> o = Java6Assertions.assertThat(any.getCause())
            .hasCauseInstanceOf(Exception.class);
        for (String s : msgPart) {
            o.hasStackTraceContaining(s);
        }
    }
}

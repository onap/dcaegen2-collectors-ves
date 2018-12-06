/*-
 * ============LICENSE_START=======================================================
 * PROJECT
 * ================================================================================
 * Copyright (C) 2018 Nokia Networks Intellectual Property. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dcae.common.AnyNode;

/**
 * Created by koblosz on 07.06.18.
 */
public class AnyNodeTest {

    private static final String SAMPLE_JSON_FILEPATH = "{\n"
        + "  \"channels\": [{\n"
        + "    \"one\": \"number1\", \"two\": \"number2\", \"three\": \"number3\"}],\n"
        + "  \"sampleStrList\": [\"1\", \"2\", \"3\", \"4\", \"5\"],\n"
        + "  \"sampleNestedObject\": {\"a\": 1, \"b\": 2},\n"
        + "  \"sampleInt\": 1,\n"
        + "  \"sampleString\": \"str\",\n"
        + "  \"sampleNull\": null\n"
        + "}\n";
    private static final Set<String> EXPECTED_JSON_KEYS = Sets
        .newHashSet("channels", "sampleStrList", "sampleNestedObject", "sampleInt", "sampleString", "sampleNull");
    private static AnyNode node;


    @BeforeClass
    public static void setUpClass() {
        node = AnyNode.fromString(SAMPLE_JSON_FILEPATH);
    }

    @Test
    public void testShouldReturnJsonObjectKeySet() {
        assertThat(node.keys()).containsOnlyElementsOf(EXPECTED_JSON_KEYS);
    }

    @Test(expected = ClassCastException.class)
    public void whenInvokedOnJsonObjInsteadOfJsonArrShouldRaiseRuntimeEx() {
        node.toList();
    }
}
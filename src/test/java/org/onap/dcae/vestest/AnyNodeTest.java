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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dcae.commonFunction.AnyNode;

import java.io.FileReader;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by koblosz on 07.06.18.
 */
public class AnyNodeTest {

    private static final String SAMPLE_JSON_FILEPATH = "src/test/resources/test_anynode_class.json";
    private static final Map<String, Object> EXPECTED_RAW_MAP = ImmutableMap.<String, Object>builder().put("a", 1).put("b", 2).build();
    public static final Set<String> EXPECTED_JSON_KEYS = Sets.newHashSet("channels", "sampleStrList", "sampleNestedObject", "sampleInt", "sampleString", "sampleNull");
    private static AnyNode node;

    @BeforeClass
    public static void setUpClass() throws Exception {
        node = AnyNode.parse(new FileReader(SAMPLE_JSON_FILEPATH));
    }

    @Test
    public void testShouldReturnJsonObjectKeySet() {
        assertThat(node.getKeys()).containsOnlyElementsOf(EXPECTED_JSON_KEYS);
    }

    @Test
    public void testShouldGetElementAsString() {
        assertThat(node.get("sampleStrList").get(0).asString()).isEqualTo("1");
    }

    @Test
    public void testShouldGetElementAsInt() {
        assertThat(node.get("sampleInt").asInt()).isSameAs(1);
    }

    @Test
    public void testWhenNullValuePresentShouldReturnJsonObjectNullAsString() {
        assertThat(node.get("sampleNull").asString()).isSameAs(JSONObject.NULL.toString());
    }

    @Test
    public void testShouldGetJsonObjectAsStringToObjectMap() {
        assertThat(node.get("sampleNestedObject").asRawMap()).containsAllEntriesOf(EXPECTED_RAW_MAP);
    }

    @Test
    public void testShouldGetAsMap() {
        assertThat(node.asMap().keySet()).containsOnlyElementsOf(EXPECTED_JSON_KEYS);
    }

    @Test
    public void testShouldGetAsList() {
        assertThat(node.get("sampleStrList").asList().stream().map(AnyNode::asString).collect(Collectors.toList())).containsExactly("1", "2", "3", "4", "5");
    }

    @Test
    public void testShouldGetAsOptional() {
        assertThat(node.getAsOptional("absentKey")).isNotPresent();
    }

    @Test
    public void testWhenChainMethodsShouldReturnValue() {
        assertThat(node.get("channels").get(0).get("two").asString()).isEqualTo("number2");
    }


    @Test(expected = ClassCastException.class)
    public void whenInvokedOnJsonObjInsteadOfJsonArrShouldRaiseRuntimeEx() {
        node.asList();
    }
}
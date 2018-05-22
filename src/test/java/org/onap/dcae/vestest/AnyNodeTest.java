package org.onap.dcae.vestest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.dcae.commonFunction.AnyNode;

import java.io.FileReader;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by koblosz on 07.06.18.
 */
public class AnyNodeTest {

    private static final String mypath = "src/test/resources/test_anynode_class.json";
    private static final Map<String, Object> EXPECTED_RAW_MAP = ImmutableMap.<String, Object>builder().put("a", 1).put("b", 2).build();
    public static final Set<String> EXPECTED_JSON_KEYS = Sets.newHashSet("channels", "sampleStrList", "sampleNestedObject", "sampleInt", "sampleString", "sampleNull");
    private static AnyNode node;

    @BeforeClass
    public static void setUpClass() throws Exception {
        node = AnyNode.parse(new FileReader(mypath));
    }

    @Test
    public void testShouldReturnJsonObjectKeySet() throws Exception {
        assertTrue(CollectionUtils.isEqualCollection(node.getKeys(), EXPECTED_JSON_KEYS));
    }

    @Test
    public void testShouldGetElementFromJsonObjectByKey() throws Exception {
    }

    @Test
    public void testShouldGetElementFromJsonArrayByIndex() throws Exception {
    }

    @Test
    public void testShouldGetElementAsInt() throws Exception {
        assertEquals(1, node.get("sampleInt").asInt());
    }

    @Test
    public void testShouldGetElementAsString() throws Exception {
        assertEquals("2", node.get("sampleStrList").get(1).asString());
    }

    @Test
    public void testWhenNullValuePresentShouldReturnJsonObjectNullAsString() throws Exception {
        assertEquals(node.get("sampleNull").asString(), JSONObject.NULL.toString());
    }

    @Test
    public void testShouldGetJsonObjectAsStringToObjectMap() throws Exception {
        assertTrue(Maps.difference(EXPECTED_RAW_MAP, node.get("sampleNestedObject").asRawMap()).areEqual());
    }

    @Test
    public void testShouldGetAsJsonObject() throws Exception {
    }

    @Test
    public void testShouldGetAsJsonArray() throws Exception {
    }

    @Test
    public void testShouldGetAsOptional() throws Exception {
        assertFalse(node.getAsOptional("absentKey").isPresent());
    }

    @Test
    public void testWhenChainMethodsShouldReturnValue() throws Exception {
        assertEquals(2, node.get("sampleNestedObject").asMap().get("b").asInt());
        assertEquals("number2", node.get("channels").get(0).get("two").asString());
    }
}
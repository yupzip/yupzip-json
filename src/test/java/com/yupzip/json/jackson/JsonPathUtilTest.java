package com.yupzip.json.jackson;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonPathUtilTest {

    @Test
    void parsesSimpleKey() {
        assertEquals(List.of("name"), JsonPathUtil.parsePath("name"));
    }

    @Test
    void parsesDotSeparatedPath() {
        assertEquals(List.of("a", "b", "c"), JsonPathUtil.parsePath("a.b.c"));
    }

    @Test
    void parsesBracketIndex() {
        assertEquals(List.of("items", "0"), JsonPathUtil.parsePath("items[0]"));
    }

    @Test
    void parsesMixedDotAndBracket() {
        assertEquals(List.of("a", "0", "b", "1"), JsonPathUtil.parsePath("a[0].b[1]"));
    }

    @Test
    void parsesNestedBrackets() {
        assertEquals(List.of("grid", "0", "1"), JsonPathUtil.parsePath("grid[0][1]"));
    }

    @Test
    void parsesLeadingBracket() {
        assertEquals(List.of("0", "name"), JsonPathUtil.parsePath("[0].name"));
    }

    @Test
    void parsesBacktickQuotedLiteral() {
        assertEquals(List.of("key.with.dots"), JsonPathUtil.parsePath("`key.with.dots`"));
    }

    @Test
    void parsesBacktickInTheMiddle() {
        assertEquals(List.of("a", "b.c", "d"), JsonPathUtil.parsePath("a.`b.c`.d"));
    }

    @Test
    void bracketsInsideBackticksAreLiteral() {
        assertEquals(List.of("a.b[0]"), JsonPathUtil.parsePath("`a.b[0]`"));
    }

    @Test
    void parsesEmptyStringAsEmptyList() {
        assertEquals(List.of(), JsonPathUtil.parsePath(""));
    }

    @Test
    void ignoresLeadingAndTrailingDots() {
        assertEquals(List.of("a", "b"), JsonPathUtil.parsePath(".a.b."));
    }

    @Test
    void collapsesConsecutiveDots() {
        assertEquals(List.of("a", "b"), JsonPathUtil.parsePath("a..b"));
    }

    @Test
    void resolveWalksNestedMaps() {
        Map<String, Object> root = Map.of(
                "user", Map.of(
                        "address", Map.of("postCode", "2000")));

        assertEquals("2000", JsonPathUtil.resolve(root, "user.address.postCode"));
    }

    @Test
    void resolveIndexesIntoLists() {
        Map<String, Object> root = Map.of(
                "items", List.of(
                        Map.of("sku", "A"),
                        Map.of("sku", "B")));

        assertEquals("B", JsonPathUtil.resolve(root, "items[1].sku"));
    }

    @Test
    void resolveReturnsNullForMissingPath() {
        Map<String, Object> root = Map.of("a", Map.of("b", "value"));

        assertNull(JsonPathUtil.resolve(root, "a.missing"));
        assertNull(JsonPathUtil.resolve(root, "nope.nope"));
        assertNull(JsonPathUtil.resolve(root, "a.b.tooDeep"));
    }

    @Test
    void resolveReturnsNullForOutOfBoundsIndex() {
        Map<String, Object> root = Map.of("items", List.of("a", "b"));

        assertNull(JsonPathUtil.resolve(root, "items[5]"));
        assertNull(JsonPathUtil.resolve(root, "items[-1]"));
    }

    @Test
    void resolveReturnsNullForNonNumericIndexOnList() {
        Map<String, Object> root = Map.of("items", List.of("a"));

        assertNull(JsonPathUtil.resolve(root, "items[abc]"));
    }

    @Test
    void resolveHandlesNullPath() {
        assertNull(JsonPathUtil.resolve(Map.of("a", 1), null));
    }

    @Test
    void hasPathDetectsExistingPaths() {
        Map<String, Object> root = Map.of("a", Map.of("b", "value"));

        assertTrue(JsonPathUtil.hasPath(root, "a"));
        assertTrue(JsonPathUtil.hasPath(root, "a.b"));
    }

    @Test
    void hasPathDistinguishesMissingFromNullValue() {
        // hasPath should return true for a key whose value is explicitly null.
        java.util.HashMap<String, Object> inner = new java.util.HashMap<>();
        inner.put("nullable", null);
        Map<String, Object> root = Map.of("a", inner);

        assertTrue(JsonPathUtil.hasPath(root, "a.nullable"));
        assertFalse(JsonPathUtil.hasPath(root, "a.missing"));
    }

    @Test
    void hasPathHandlesNullPath() {
        assertFalse(JsonPathUtil.hasPath(Map.of("a", 1), null));
    }
}

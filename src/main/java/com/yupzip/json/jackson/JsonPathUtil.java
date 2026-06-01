package com.yupzip.json.jackson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class JsonPathUtil {

    private JsonPathUtil() {}

    static Object resolve(Map<String, Object> properties, String path) {
        if (path == null) {
            return null;
        }
        if (isSimpleKey(path)) {
            return properties.get(path);
        }
        Object current = properties;
        for (String segment : parsePath(path)) {
            current = stepOne(current, segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    static boolean hasPath(Map<String, Object> properties, String path) {
        if (path == null) {
            return false;
        }
        if (isSimpleKey(path)) {
            return properties.containsKey(path);
        }
        List<String> segments = parsePath(path);
        Object current = properties;
        for (int i = 0; i < segments.size() - 1; i++) {
            current = stepOne(current, segments.get(i));
            if (current == null) {
                return false;
            }
        }
        return containsSegment(current, segments.get(segments.size() - 1));
    }

    static List<String> parsePath(String path) {
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        boolean inBracket = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '`' && !inBracket) {
                inQuote = !inQuote;
            } else if (inQuote) {
                current.append(c);
            } else if (c == '[') {
                if (!current.isEmpty()) {
                    segments.add(current.toString());
                    current.setLength(0);
                }
                inBracket = true;
            } else if (c == ']' && inBracket) {
                segments.add(current.toString());
                current.setLength(0);
                inBracket = false;
            } else if (c == '.' && !inBracket) {
                if (!current.isEmpty()) {
                    segments.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            segments.add(current.toString());
        }
        return segments;
    }

    private static boolean isSimpleKey(String path) {
        return path.indexOf('.') < 0 && path.indexOf('`') < 0 && path.indexOf('[') < 0;
    }

    private static Object stepOne(Object node, String segment) {
        if (node instanceof Map<?, ?> map) {
            return map.get(segment);
        }
        if (node instanceof List<?> list) {
            try {
                int idx = Integer.parseInt(segment);
                return idx >= 0 && idx < list.size() ? list.get(idx) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static boolean containsSegment(Object node, String segment) {
        if (node instanceof Map<?, ?> map) {
            return map.containsKey(segment);
        }
        if (node instanceof List<?> list) {
            try {
                int idx = Integer.parseInt(segment);
                return idx >= 0 && idx < list.size();
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}

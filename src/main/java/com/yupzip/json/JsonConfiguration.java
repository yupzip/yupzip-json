package com.yupzip.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class JsonConfiguration {

    public static final JsonParser JSON_PARSER;
    public static final MapType MAP_TYPE;

    static {
        Properties props = loadProperties();
        MAP_TYPE = MapType.valueOf(props.getProperty("yupzip.json.map-type", "HASH_MAP"));
        JSON_PARSER = JsonParser.valueOf(props.getProperty("yupzip.json.parser", "JACKSON"));
    }

    private JsonConfiguration() {}

    static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream inputStream = JsonConfiguration.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (null != inputStream) {
                props.load(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }

    public enum MapType {

        HASH_MAP, LINKED_HASH_MAP;

        public Map<String, Object> createMap() {
            if (this == LINKED_HASH_MAP) {
                return new LinkedHashMap<>();
            }
            return new HashMap<>();
        }
    }
}

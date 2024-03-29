package com.yupzip.json.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yupzip.json.JsonConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.yupzip.json.JsonParser.JACKSON;
import static java.lang.Boolean.parseBoolean;

public class JacksonConfiguration {

    static final ObjectMapper OBJECT_MAPPER;
    static final JavaType JSON_TYPE;
    static final ObjectReader JSON_READER;
    static final CollectionType LIST_TYPE_JSON;
    static final CollectionType LIST_TYPE_STRING;
    static final CollectionType LIST_TYPE_INTEGER;
    static final CollectionType LIST_TYPE_DOUBLE;

    static final Map<String, PropertyNamingStrategy> NAMING_STRATEGY_MAP;

    static {
        if (JsonConfiguration.JSON_PARSER == JACKSON) {
            NAMING_STRATEGY_MAP = new HashMap<>();
            NAMING_STRATEGY_MAP.put("SNAKE_CASE", PropertyNamingStrategies.SNAKE_CASE);
            NAMING_STRATEGY_MAP.put("KEBAB_CASE", PropertyNamingStrategies.KEBAB_CASE);
            NAMING_STRATEGY_MAP.put("LOWER_CAMEL_CASE", PropertyNamingStrategies.LOWER_CAMEL_CASE);
            NAMING_STRATEGY_MAP.put("UPPER_CAMEL_CASE", PropertyNamingStrategies.UPPER_CAMEL_CASE);
            NAMING_STRATEGY_MAP.put("LOWER_CASE", PropertyNamingStrategies.LOWER_CASE);
            Properties props = loadProperties();
            OBJECT_MAPPER = getObjectMapper(props);
            JSON_TYPE = OBJECT_MAPPER.reader().getTypeFactory().constructType(JJson.class);
            JSON_READER = OBJECT_MAPPER.reader().forType(JSON_TYPE);
            LIST_TYPE_JSON = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, JJson.class);
            LIST_TYPE_STRING = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class);
            LIST_TYPE_INTEGER = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Integer.class);
            LIST_TYPE_DOUBLE = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Double.class);
        } else {
            OBJECT_MAPPER = null;
            JSON_TYPE = null;
            JSON_READER = null;
            LIST_TYPE_JSON = null;
            LIST_TYPE_STRING = null;
            LIST_TYPE_INTEGER = null;
            LIST_TYPE_DOUBLE = null;
            NAMING_STRATEGY_MAP = null;

        }
    }

    private static ObjectMapper getObjectMapper(Properties props) {
        JsonMapper.Builder jsonMapperBuilder = JsonMapper.builder()
                .configure(FAIL_ON_EMPTY_BEANS, parseBoolean(props.getProperty("jackson.serialization.fail-on-empty-beans", "false")))
                .configure(WRITE_DATES_AS_TIMESTAMPS, parseBoolean(props.getProperty("jackson.serialization.write-dates-as-timestamps", "false")))
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, parseBoolean(props.getProperty("jackson.deserialization.fail-on-unknown-properties", "false")))
                .serializationInclusion(JsonInclude.Include.valueOf(props.getProperty("jackson.default-property-inclusion", "ALWAYS")));

        setPropertyNamingStrategy(jsonMapperBuilder, props);
        enableFeatures(jsonMapperBuilder, props);
        disableFeatures(jsonMapperBuilder, props);
        configureVisibility(jsonMapperBuilder, props);
        jsonMapperBuilder.addModule(new JavaTimeModule());
        return jsonMapperBuilder.build();
    }

    private JacksonConfiguration() {}


    static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream inputStream = JacksonConfiguration.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (null != inputStream) {
                props.load(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }

    private static void setPropertyNamingStrategy(JsonMapper.Builder jsonMapperBuilder, Properties props) {
        String namingStrategy = props.getProperty("jackson.property-naming-strategy", "");
        if (!"".equals(namingStrategy)) {
            PropertyNamingStrategy strategy = NAMING_STRATEGY_MAP.get(namingStrategy);
            if (null != strategy) {
                jsonMapperBuilder.propertyNamingStrategy(strategy);
            }
        }
    }

    private static void configureVisibility(JsonMapper.Builder jsonMapperBuilder, Properties props) {
        jsonMapperBuilder.visibility(new ObjectMapper()
                .getSerializationConfig()
                .getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.valueOf(props.getProperty("jackson.visibility.field", "ANY")))
                .withGetterVisibility(JsonAutoDetect.Visibility.valueOf(props.getProperty("jackson.visibility.getter", "NONE")))
                .withIsGetterVisibility(JsonAutoDetect.Visibility.valueOf(props.getProperty("jackson.visibility.is-getter", "NONE")))
                .withSetterVisibility(JsonAutoDetect.Visibility.valueOf(props.getProperty("jackson.visibility.setter", "NONE"))));
    }

    private static void disableFeatures(JsonMapper.Builder jsonMapperBuilder, Properties props) {
        String[] disabledFeatures = props.getProperty("jackson.disabled-features", "").split(",");
        Arrays.stream(disabledFeatures).forEach(feature -> {
            seekDeserializationFeature(feature).ifPresent(jsonMapperBuilder::disable);
            seekSerializationFeature(feature).ifPresent(jsonMapperBuilder::disable);
            seekMapperFeature(feature).ifPresent(jsonMapperBuilder::disable);
        });
    }

    private static void enableFeatures(JsonMapper.Builder jsonMapperBuilder, Properties props) {
        String[] disabledFeatures = props.getProperty("jackson.enabled-features", "").split(",");
        Arrays.stream(disabledFeatures).forEach(feature -> {
            seekDeserializationFeature(feature).ifPresent(jsonMapperBuilder::enable);
            seekSerializationFeature(feature).ifPresent(jsonMapperBuilder::enable);
            seekMapperFeature(feature).ifPresent(jsonMapperBuilder::enable);
        });
    }

    private static Optional<SerializationFeature> seekSerializationFeature(String feature) {
        try {
            return Optional.of(SerializationFeature.valueOf(feature));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<DeserializationFeature> seekDeserializationFeature(String feature) {
        try {
            return Optional.of(DeserializationFeature.valueOf(feature));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<MapperFeature> seekMapperFeature(String feature) {
        try {
            return Optional.of(MapperFeature.valueOf(feature));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

package com.yupzip.json.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.yupzip.json.JsonConfiguration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.CollectionType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static com.yupzip.json.JsonParser.JACKSON;
import static java.lang.Boolean.parseBoolean;
import static tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static tools.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static tools.jackson.databind.cfg.DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS;

public class JacksonConfiguration {

    static final JsonMapper JSON_MAPPER;
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
            JSON_MAPPER = getJsonMapper(props);
            JSON_TYPE = JSON_MAPPER.reader().typeFactory().constructType(JJson.class);
            JSON_READER = JSON_MAPPER.reader().forType(JSON_TYPE);
            LIST_TYPE_JSON = JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, JJson.class);
            LIST_TYPE_STRING = JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class);
            LIST_TYPE_INTEGER = JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, Integer.class);
            LIST_TYPE_DOUBLE = JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, Double.class);
        } else {
            JSON_MAPPER = null;
            JSON_TYPE = null;
            JSON_READER = null;
            LIST_TYPE_JSON = null;
            LIST_TYPE_STRING = null;
            LIST_TYPE_INTEGER = null;
            LIST_TYPE_DOUBLE = null;
            NAMING_STRATEGY_MAP = null;

        }
    }

    private static JsonMapper getJsonMapper(Properties props) {
        JsonMapper.Builder jsonMapperBuilder = JsonMapper.builder()
                .configure(FAIL_ON_EMPTY_BEANS, parseBoolean(props.getProperty("jackson.serialization.fail-on-empty-beans", "false")))
                .configure(WRITE_DATES_AS_TIMESTAMPS, parseBoolean(props.getProperty("jackson.serialization.write-dates-as-timestamps", "false")))
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, parseBoolean(props.getProperty("jackson.deserialization.fail-on-unknown-properties", "false")))
                .changeDefaultPropertyInclusion(v -> JsonInclude.Value.construct(
                        JsonInclude.Include.valueOf(props.getProperty("jackson.default-property-inclusion", "ALWAYS")),
                        JsonInclude.Include.valueOf(props.getProperty("jackson.default-property-inclusion", "ALWAYS"))
                ));

        setPropertyNamingStrategy(jsonMapperBuilder, props);
        enableFeatures(jsonMapperBuilder, props);
        disableFeatures(jsonMapperBuilder, props);
        configureVisibility(jsonMapperBuilder, props);
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
        jsonMapperBuilder.changeDefaultVisibility(v -> new JsonMapper()
                .serializationConfig()
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

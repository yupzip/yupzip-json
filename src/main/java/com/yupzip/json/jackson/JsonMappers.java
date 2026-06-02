package com.yupzip.json.jackson;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.CollectionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Central access point for the {@link JsonMapper} used by the library.
 *
 * <p>By default the mapper is built from {@code application.properties} via
 * {@link JacksonConfiguration#buildDefaultMapper()}. External code (typically a Spring Boot
 * starter) may swap it via {@link #configure(JsonMapper)} during application startup so that
 * yupzip-json shares the same mapper as the rest of the application.
 *
 * <p>Derived types ({@link JavaType}, {@link ObjectReader}, {@link CollectionType}) are computed
 * once per mapper and exposed as accessor methods. Reconfiguring the mapper atomically rebuilds
 * the derived types — readers always observe a consistent snapshot.
 */
public final class JsonMappers {

    private static volatile Snapshot snapshot = Snapshot.of(JacksonConfiguration.buildDefaultMapper());

    private JsonMappers() {}

    /** Returns the currently configured {@link JsonMapper}. */
    public static JsonMapper current() {
        return snapshot.mapper;
    }

    /**
     * Replaces the current mapper. Intended for one-time configuration at application startup
     * (e.g. a Spring Boot starter handing in the Spring-managed mapper).
     */
    public static void configure(JsonMapper override) {
        Objects.requireNonNull(override, "JsonMapper must not be null");
        snapshot = Snapshot.of(override);
    }

    static JavaType jsonType() {
        return snapshot.jsonType;
    }

    static ObjectReader jsonReader() {
        return snapshot.jsonReader;
    }

    static CollectionType listTypeJson() {
        return snapshot.listTypeJson;
    }

    static CollectionType listTypeString() {
        return snapshot.listTypeString;
    }

    static CollectionType listTypeInteger() {
        return snapshot.listTypeInteger;
    }

    static CollectionType listTypeLong() {
        return snapshot.listTypeLong;
    }

    static CollectionType listTypeDouble() {
        return snapshot.listTypeDouble;
    }

    static CollectionType listTypeBigDecimal() {
        return snapshot.listTypeBigDecimal;
    }

    private static final class Snapshot {
        final JsonMapper mapper;
        final JavaType jsonType;
        final ObjectReader jsonReader;
        final CollectionType listTypeJson;
        final CollectionType listTypeString;
        final CollectionType listTypeInteger;
        final CollectionType listTypeLong;
        final CollectionType listTypeDouble;
        final CollectionType listTypeBigDecimal;

        private Snapshot(JsonMapper mapper) {
            this.mapper = mapper;
            this.jsonType = mapper.reader().typeFactory().constructType(JJson.class);
            this.jsonReader = mapper.reader().forType(jsonType);
            var tf = mapper.getTypeFactory();
            this.listTypeJson = tf.constructCollectionType(List.class, JJson.class);
            this.listTypeString = tf.constructCollectionType(List.class, String.class);
            this.listTypeInteger = tf.constructCollectionType(List.class, Integer.class);
            this.listTypeLong = tf.constructCollectionType(List.class, Long.class);
            this.listTypeDouble = tf.constructCollectionType(List.class, Double.class);
            this.listTypeBigDecimal = tf.constructCollectionType(List.class, BigDecimal.class);
        }

        static Snapshot of(JsonMapper mapper) {
            return new Snapshot(mapper);
        }
    }
}

package com.yupzip.json.jackson;

import com.yupzip.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonMappersTest {

    private final JsonMapper original = JsonMappers.current();

    @AfterEach
    void restore() {
        JsonMappers.configure(original);
    }

    @Test
    void currentReturnsTheConfiguredMapper() {
        assertNotNull(JsonMappers.current());
    }

    @Test
    void configureSwapsTheMapper() {
        JsonMapper override = JsonMapper.builder().build();

        JsonMappers.configure(override);

        assertSame(override, JsonMappers.current());
        assertNotSame(original, JsonMappers.current());
    }

    @Test
    void configureRebuildsDerivedTypes() {
        var firstListType = JsonMappers.listTypeJson();

        JsonMapper override = JsonMapper.builder().build();
        JsonMappers.configure(override);

        assertNotSame(firstListType, JsonMappers.listTypeJson(),
                "derived types must be recomputed when the mapper changes");
    }

    @Test
    void configureRejectsNull() {
        assertThrows(NullPointerException.class, () -> JsonMappers.configure(null));
    }

    @Test
    void swappedMapperAffectsJsonSerialization() {
        // Default mapper has no naming strategy. Override with SNAKE_CASE and verify yupzip uses it.
        JsonMapper snakeCaseMapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();

        JsonMappers.configure(snakeCaseMapper);

        // POJOs converted via Json.parse(...) should now reflect the snake_case strategy.
        Person person = new Person();
        person.firstName = "John";

        Json json = Json.parse(person);

        assertEquals("John", json.string("first_name"));
    }

    static class Person {
        public String firstName;
    }
}

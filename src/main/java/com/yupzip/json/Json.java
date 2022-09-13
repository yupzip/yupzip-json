package com.yupzip.json;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.yupzip.json.jackson.JJson;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface Json {

    @JsonCreator
    static Json create() {
        return JJson.create();
    }

    static Optional<Json> from(Object object) {
        return JJson.from(object);
    }

    static boolean isValid(String jsonString) {
        try {
            parse(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static Json parse(Object object) {
        return JJson.parse(object);
    }

    static Json parse(String jsonString) {
        if (null == jsonString) {
            return Json.create();
        }
        return JJson.parse(jsonString);
    }

    static Json parse(byte[] jsonData) {
        return JJson.parse(jsonData);
    }

    static <T> T parseAs(String jsonString, Class<T> clazz) {
        return JJson.parseAs(jsonString, clazz);
    }

    static <T> T parseAs(byte[] jsonData, Class<T> clazz) {
        return JJson.parseAs(jsonData, clazz);
    }

    static List<Json> array(Object object) {
        return JJson.array(object);
    }

    static String asString(Object object) {
        return JJson.asString(object);
    }

    @JsonAnySetter
    Json put(String key, Object value);

    @JsonAnyGetter
    Map<String, Object> asMap();

    Json put(String key, Json value);

    Json put(String key, Iterable<Json> iterable);

    Json put(Map<String, Object> map);

    Json add(String key, Object value);

    Json add(String key, Json value);

    Json add(String key, Iterable<Json> iterable);

    Json append(String key, String value);

    Json append(String key, Integer value);

    Json append(String key, Double value);

    Json append(String key, Json value);

    boolean hasKey(String key);

    boolean hasValueFor(String key);

    boolean valueEquals(String key, Object value);

    boolean isEmpty();

    boolean remove(String key);

    <T> T get(String key, Class<T> type);

    <T> T convertTo(Class<T> type);

    Json object(String key);

    Json objectOrThrow(String key);

    Json objectOrThrow(String key, RuntimeException e);

    Optional<Json> seek(String key);

    Stream<Json> stream(String key);

    List<Json> array(String key);

    Optional<List<Json>> seekArray(String key);

    String string(String key);

    String stringOr(String key, String defaultValue);

    String stringOrThrow(String key);

    String stringOrThrow(String key, RuntimeException e);

    List<String> strings(String key);

    Integer integer(String key);

    int integerOr(String key, int defaultValue);

    Integer integerOrThrow(String key);

    Integer integerOrThrow(String key, RuntimeException e);

    List<Integer> integers(String key);

    Double decimal(String key);

    double decimalOr(String key, double defaultValue);

    Double decimalOrThrow(String key);

    Double decimalOrThrow(String key, RuntimeException e);

    List<Double> decimals(String key);

    Boolean bool(String key);

    boolean boolOr(String key, boolean defaultValue);

    Boolean boolOrThrow(String key);

    Boolean boolOrThrow(String key, RuntimeException e);

    boolean isTrue(String key);

    boolean isFalse(String key);

    boolean anyTrue(String... keys);

    boolean anyFalse(String... keys);

    boolean allTrue(String... keys);

    boolean allFalse(String... keys);

    Date date(String key, String format);

    Date dateOrNow(String key, String format);

    Date dateOrThrow(String key, String format);

    Date dateOrThrow(String key, String format, RuntimeException e);

    Date date(String key, String format, String timeZone);

    Date date(String dateKey, String timeKey, String joinString, String format);

    Json string(String key, Consumer<String> consumer);

    Json strings(String key, Consumer<List<String>> consumer);

    Json integer(String key, Consumer<Integer> consumer);

    Json integers(String key, Consumer<List<Integer>> consumer);

    Json decimal(String key, Consumer<Double> consumer);

    Json decimals(String key, Consumer<List<Double>> consumer);

    Json bool(String key, Consumer<Boolean> consumer);

    Json object(String key, Consumer<Json> consumer);

    Json array(String key, Consumer<List<Json>> consumer);

    <T> Json map(String key, Consumer<T> consumer);

    <T> T find(String key, Class<T> type);
}

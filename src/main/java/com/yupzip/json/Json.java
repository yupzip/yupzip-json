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

    static Json parse(Object object) {
        return JJson.parse(object);
    }

    static Json parse(String jsonString) {
        return JJson.parse(jsonString);
    }

    static List<Json> array(Object object) {
        return JJson.array(object);
    }

    @JsonAnySetter
    Json put(String key, Object value);

    @JsonAnyGetter
    Map<String,Object> asMap();

    Json put(String key, Json value);

    Json put(String key, Iterable<Json> iterable);

    Json put(Map<String,Object> map);

    Json add(String key, Object value);

    Json add(String key, Json value);

    Json add(String key, Iterable<Json> iterable);

    Json append(String key, String value);

    Json append(String key, Integer value);

    Json append(String key, Double value);

    boolean hasKey(String key);

    boolean isEmpty();

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

    Integer integerOr(String key, int defaultValue);

    Integer integerOrThrow(String key);

    Integer integerOrThrow(String key, RuntimeException e);

    List<Integer> integers(String key);

    Double decimal(String key);

    Double decimalOr(String key, double defaultValue);

    Double decimalOrThrow(String key);

    Double decimalOrThrow(String key, RuntimeException e);

    List<Double> decimals(String key);

    Boolean bool(String key);

    Boolean boolOr(String key, boolean defaultValue);

    Boolean boolOrThrow(String key);

    Boolean boolOrThrow(String key, RuntimeException e);

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

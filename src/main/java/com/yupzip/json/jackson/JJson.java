package com.yupzip.json.jackson;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.yupzip.json.Json;
import com.yupzip.json.JsonParseException;
import com.yupzip.json.PropertyRequiredException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.yupzip.json.JsonConfiguration.MAP_TYPE;
import static com.yupzip.json.jackson.JacksonConfiguration.JSON_READER;
import static com.yupzip.json.jackson.JacksonConfiguration.JSON_TYPE;
import static com.yupzip.json.jackson.JacksonConfiguration.LIST_TYPE_DOUBLE;
import static com.yupzip.json.jackson.JacksonConfiguration.LIST_TYPE_INTEGER;
import static com.yupzip.json.jackson.JacksonConfiguration.LIST_TYPE_JSON;
import static com.yupzip.json.jackson.JacksonConfiguration.LIST_TYPE_STRING;
import static com.yupzip.json.jackson.JacksonConfiguration.OBJECT_MAPPER;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class JJson implements Json {

    @JsonIgnore
    private final ObjectWriter objectWriter;

    @JsonIgnore
    private final Map<String,Object> properties;


    private JJson() {
        this.properties = MAP_TYPE.createMap();
        this.objectWriter = OBJECT_MAPPER.writer();
    }

    public static Json create() {
        return new JJson();
    }

    public static Optional<Json> from(Object object) {
        return Optional.ofNullable(OBJECT_MAPPER.convertValue(object, JSON_TYPE));
    }

    public static Json parse(Object object) {
        try {
            return OBJECT_MAPPER.convertValue(object, JSON_TYPE);
        } catch(Exception e){
            throw new JsonParseException("Error parsing object ", e);
        }
    }

    public static Json parse(String jsonString) {
        try {
            return JSON_READER.readValue(jsonString);
        } catch(Exception e){
            throw new JsonParseException("Error parsing JSON string ", e);
        }
    }

    public static <T> T parseAs(String jsonString, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(jsonString, clazz);
        } catch(Exception e){
            throw new JsonParseException("Error parsing JSON string ", e);
        }
    }

    public static List<Json> array(Object object) {
        try {
            return OBJECT_MAPPER.convertValue(object, LIST_TYPE_JSON);
        } catch(Exception e){
            throw new JsonParseException("Error parsing JSON array ", e);
        }
    }

    public static String asString(Object object) {
        try {
            return OBJECT_MAPPER.writer().writeValueAsString(object);
        } catch(JsonProcessingException e){
            throw new JsonParseException(e);
        }
    }

    @JsonAnySetter
    public Json put(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    @JsonAnyGetter
    public Map<String,Object> asMap() {
        return properties;
    }

    public Json put(String key, Json value) {
        properties.put(key, value == null ? null : value.asMap());
        return this;
    }

    public Json put(String key, Iterable<Json> iterable) {
        if(iterable != null){
            Stream<Json> jsonStream = StreamSupport.stream(iterable.spliterator(), false);
            properties.put(key, jsonStream.map(Json::asMap).collect(Collectors.toList()));
        } else {
            properties.put(key, null);
        }
        return this;
    }

    public Json put(Map<String,Object> map) {
        properties.putAll(map);
        return this;
    }

    public Json add(String key, Object value) {
        if(null != value){
            properties.put(key, value);
        }
        return this;
    }

    public Json add(String key, Json value) {
        if(null != value){
            properties.put(key, value.asMap());
        }
        return this;
    }

    public Json add(String key, Iterable<Json> iterable) {
        if(iterable != null){
            Stream<Json> jsonStream = StreamSupport.stream(iterable.spliterator(), false);
            properties.put(key, jsonStream.map(Json::asMap).collect(Collectors.toList()));
        }
        return this;
    }

    public Json append(String key, String value) {
        if(null != value){
            List<String> values = properties.containsKey(key) ? strings(key) : new ArrayList<>();
            values.add(value);
            properties.put(key, values);
        }
        return this;
    }

    public Json append(String key, Integer value) {
        if(null != value){
            List<Integer> values = properties.containsKey(key) ? integers(key) : new ArrayList<>();
            values.add(value);
            properties.put(key, values);
        }
        return this;
    }

    public Json append(String key, Double value) {
        if(null != value){
            List<Double> values = properties.containsKey(key) ? decimals(key) : new ArrayList<>();
            values.add(value);
            properties.put(key, values);
        }
        return this;
    }

    public boolean hasKey(String key) {
        return this.properties.containsKey(key);
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    public <T> T get(String key, Class<T> type) {
        return OBJECT_MAPPER.convertValue(properties.get(key), type);
    }

    public <T> T convertTo(Class<T> type) {
        try {
            return OBJECT_MAPPER.convertValue(this, type);
        } catch(Exception e){
            throw new JsonParseException(e);
        }
    }

    public Json object(String key) {
        return OBJECT_MAPPER.convertValue(properties.get(key), JSON_TYPE);
    }

    public Json objectOrThrow(String key) {
        if(!properties.containsKey(key) || null == properties.get(key)){
            throw new PropertyRequiredException();
        }
        return OBJECT_MAPPER.convertValue(properties.get(key), JSON_TYPE);
    }

    public Json objectOrThrow(String key, RuntimeException e) {
        if(!properties.containsKey(key) || null == properties.get(key)){
            throw e;
        }
        return OBJECT_MAPPER.convertValue(properties.get(key), JSON_TYPE);
    }

    public Optional<Json> seek(String key) {
        return Optional.ofNullable(object(key));
    }

    public Stream<Json> stream(String key) {
        return seekArray(key).map(Collection::stream).orElseGet(Stream::empty);
    }

    public List<Json> array(String key) {
        return OBJECT_MAPPER.convertValue(properties.get(key), LIST_TYPE_JSON);
    }

    public Optional<List<Json>> seekArray(String key) {
        return Optional.ofNullable(array(key));
    }

    public String string(String key) {
        try {
            return (String) properties.get(key);
        } catch(Exception e){
            throw new JsonParseException("Error parsing value to string for key " + key, e);
        }
    }

    public String stringOr(String key, String defaultValue) {
        if(properties.containsKey(key) && null != properties.get(key)){
            return string(key);
        }
        return defaultValue;
    }

    public String stringOrThrow(String key) {
        if(!properties.containsKey(key) || null == properties.get(key)){
            throw new PropertyRequiredException();
        }
        return string(key);
    }

    public String stringOrThrow(String key, RuntimeException e) {
        if(!properties.containsKey(key) || null == properties.get(key)){
            throw e;
        }
        return string(key);
    }

    public List<String> strings(String key) {
        return OBJECT_MAPPER.convertValue(properties.get(key), LIST_TYPE_STRING);
    }

    public Integer integer(String key) {
        try {
            return (Integer) properties.get(key);
        } catch(Exception e){
            throw new JsonParseException("Error parsing value to integer for key " + key, e);
        }
    }

    public Integer integerOr(String key, int defaultValue) {
        if(properties.containsKey(key) && null != properties.get(key)){
            return integer(key);
        }
        return defaultValue;
    }

    public Integer integerOrThrow(String key) {
        if(!properties.containsKey(key) || null == properties.get(key)){
            throw new PropertyRequiredException();
        }
        return integer(key);
    }

    public Integer integerOrThrow(String key, RuntimeException e) {
        if(!properties.containsKey(key) || null == properties.get(key)){
            throw e;
        }
        return integer(key);
    }

    public List<Integer> integers(String key) {
        try {
            return OBJECT_MAPPER.convertValue(properties.get(key), LIST_TYPE_INTEGER);
        } catch(Exception e){
            throw new JsonParseException("Error parsing value to integer list for key " + key, e);
        }
    }

    public Double decimal(String key) {
        try {
            return (Double) properties.get(key);
        } catch(Exception e){
            throw new JsonParseException("Error parsing value to double for key " + key, e);
        }
    }

    public Double decimalOr(String key, double defaultValue) {
        if(properties.containsKey(key) && null != properties.get(key)){
            return decimal(key);
        }
        return defaultValue;
    }

    public Double decimalOrThrow(String key) {
        if(!properties.containsKey(key) || null == properties.get(key)){
            throw new PropertyRequiredException();
        }
        return decimal(key);
    }

    public Double decimalOrThrow(String key, RuntimeException e) {
        if(!properties.containsKey(key) || null == properties.get(key)){
            throw e;
        }
        return decimal(key);
    }

    public List<Double> decimals(String key) {
        try {
            return OBJECT_MAPPER.convertValue(properties.get(key), LIST_TYPE_DOUBLE);
        } catch(Exception e){
            throw new JsonParseException("Error parsing value to double list for key " + key, e);
        }
    }

    public Boolean bool(String key) {
        try {
            return (Boolean) properties.get(key);
        } catch(Exception e){
            throw new JsonParseException("Error parsing value to boolean for key " + key, e);
        }
    }

    public Boolean boolOr(String key, boolean defaultValue) {
        if(properties.containsKey(key) && null != properties.get(key)){
            return bool(key);
        }
        return defaultValue;
    }

    public Boolean boolOrThrow(String key) {
        if(!properties.containsKey(key) || null == properties.get(key)){
            throw new PropertyRequiredException();
        }
        return bool(key);
    }

    public Boolean boolOrThrow(String key, RuntimeException e) {
        if(!properties.containsKey(key) || null == properties.get(key)){
            throw e;
        }
        return bool(key);
    }

    public Date date(String key, String format) {
        return parseDate(new SimpleDateFormat(format), string(key));
    }

    public Date dateOrNow(String key, String format) {
        if(properties.containsKey(key) && null != string(key)){
            return parseDate(new SimpleDateFormat(format), string(key));
        }
        return new Date();
    }

    public Date dateOrThrow(String key, String format) {
        if(!properties.containsKey(key) || null == properties.get(key)){
            throw new PropertyRequiredException();
        }
        return date(key, format);
    }

    public Date dateOrThrow(String key, String format, RuntimeException e) {
        if(!properties.containsKey(key) || null == properties.get(key)){
            throw e;
        }
        return date(key, format);
    }

    public Date date(String key, String format, String timeZone) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        return parseDate(dateFormat, string(key));
    }

    public Date date(String dateKey, String timeKey, String joinString, String format) {
        String dateTime = string(dateKey).concat(joinString).concat(string(timeKey));
        return parseDate(new SimpleDateFormat(format), dateTime);
    }

    public Json string(String key, Consumer<String> consumer) {
        consumer.accept(string(key));
        return this;
    }

    public Json integer(String key, Consumer<Integer> consumer) {
        consumer.accept(integer(key));
        return this;
    }

    public Json decimal(String key, Consumer<Double> consumer) {
        consumer.accept(decimal(key));
        return this;
    }

    public Json bool(String key, Consumer<Boolean> consumer) {
        consumer.accept(bool(key));
        return this;
    }

    public Json object(String key, Consumer<Json> consumer) {
        consumer.accept(object(key));
        return this;
    }

    public Json array(String key, Consumer<List<Json>> consumer) {
        consumer.accept(OBJECT_MAPPER.convertValue(properties.get(key), LIST_TYPE_JSON));
        return this;
    }

    public Json strings(String key, Consumer<List<String>> consumer) {
        consumer.accept(OBJECT_MAPPER.convertValue(properties.get(key), LIST_TYPE_STRING));
        return this;
    }

    public Json integers(String key, Consumer<List<Integer>> consumer) {
        consumer.accept(OBJECT_MAPPER.convertValue(properties.get(key), LIST_TYPE_INTEGER));
        return this;
    }

    public Json decimals(String key, Consumer<List<Double>> consumer) {
        consumer.accept(OBJECT_MAPPER.convertValue(properties.get(key), LIST_TYPE_DOUBLE));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> Json map(String key, Consumer<T> consumer) {
        consumer.accept((T) properties.get(key));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T find(String key, Class<T> type) {
        if(properties.keySet().stream().anyMatch(k -> k.equals(key))){
            return get(key, type);
        }
        for(Map.Entry<String,Object> entry : properties.entrySet()){
            if(entry.getValue() instanceof Map){
                T value = JJson.from(entry.getValue()).orElse(JJson.create()).find(key, type);
                if(null != value){
                    return value;
                }
            } else if(entry.getValue() instanceof List && !((List) entry.getValue()).isEmpty() && ((List) entry.getValue()).get(0) instanceof Map){
                T value = ((List<Map<String,Object>>) entry.getValue())
                        .stream()
                        .map(map -> JJson.create().put(map).find(key, type))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
                if(null != value){
                    return value;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        try {
            return objectWriter.writeValueAsString(this);
        } catch(JsonProcessingException e){
            throw new JsonParseException(e);
        }
    }

    @Override
    public boolean equals(Object other) {
        if(this == other){
            return true;
        }
        if(other == null || getClass() != other.getClass()){
            return false;
        }
        JJson json = (JJson) other;
        return Objects.equals(properties, json.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties);
    }

    private Date parseDate(SimpleDateFormat dateFormat, String dateString) {
        try {
            return dateFormat.parse(dateString);
        } catch(ParseException e){
            throw new JsonParseException("Error parsing value to date " + dateString, e);
        }
    }
}

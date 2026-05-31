package com.yupzip.json.jackson;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yupzip.json.Json;
import com.yupzip.json.JsonParseException;
import com.yupzip.json.PropertyRequiredException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectWriter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.yupzip.json.JsonConfiguration.MAP_TYPE;
import static com.yupzip.json.jackson.JacksonConfiguration.JSON_READER;
import static com.yupzip.json.jackson.JacksonConfiguration.JSON_TYPE;
import static com.yupzip.json.jackson.JacksonConfiguration.LIST_TYPE_DOUBLE;
import static com.yupzip.json.jackson.JacksonConfiguration.LIST_TYPE_INTEGER;
import static com.yupzip.json.jackson.JacksonConfiguration.LIST_TYPE_BIG_DECIMAL;
import static com.yupzip.json.jackson.JacksonConfiguration.LIST_TYPE_LONG;
import static com.yupzip.json.jackson.JacksonConfiguration.LIST_TYPE_JSON;
import static com.yupzip.json.jackson.JacksonConfiguration.LIST_TYPE_STRING;
import static com.yupzip.json.jackson.JacksonConfiguration.JSON_MAPPER;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
public class JJson implements Json {

    @JsonIgnore
    private final ObjectWriter objectWriter;

    @JsonIgnore
    private final Map<String, Object> properties;


    private JJson() {
        this.properties = MAP_TYPE.createMap();
        this.objectWriter = JSON_MAPPER.writer();
    }

    public static Json create() {
        return new JJson();
    }

    public static Optional<Json> from(Object object) {
        return Optional.ofNullable(JSON_MAPPER.convertValue(object, JSON_TYPE));
    }

    public static Json parse(Object object) {
        try {
            return JSON_MAPPER.convertValue(object, JSON_TYPE);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing object ", e);
        }
    }

    public static Json parse(String jsonString) {
        try {
            return JSON_READER.readValue(jsonString);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing JSON string ", e);
        }
    }

    public static Json parse(byte[] jsonData) {
        try {
            return JSON_READER.readValue(jsonData);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing JSON string ", e);
        }
    }

    public static <T> T parseAs(String jsonString, Class<T> clazz) {
        try {
            return JSON_MAPPER.readValue(jsonString, clazz);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing JSON string ", e);
        }
    }

    public static <T> T parseAs(byte[] jsonData, Class<T> clazz) {
        try {
            return JSON_MAPPER.readValue(jsonData, clazz);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing JSON byte array ", e);
        }
    }

    public static List<Json> array(Object object) {
        try {
            return JSON_MAPPER.convertValue(object, LIST_TYPE_JSON);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing JSON array ", e);
        }
    }

    public static String asString(Object object) {
        try {
            return JSON_MAPPER.writer().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new JsonParseException(e);
        }
    }

    @JsonAnySetter
    public Json put(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> asMap() {
        return properties;
    }

    public Json put(String key, Json value) {
        properties.put(key, value == null ? null : value.asMap());
        return this;
    }

    public Json put(String key, Iterable<Json> iterable) {
        if (iterable != null) {
            Stream<Json> jsonStream = StreamSupport.stream(iterable.spliterator(), false);
            properties.put(key, jsonStream.map(Json::asMap).collect(Collectors.toList()));
        } else {
            properties.put(key, null);
        }
        return this;
    }

    public Json put(Map<String, Object> map) {
        properties.putAll(map);
        return this;
    }

    public Json add(String key, Object value) {
        if (null != value) {
            properties.put(key, value);
        }
        return this;
    }

    public Json add(String key, Json value) {
        if (null != value) {
            properties.put(key, value.asMap());
        }
        return this;
    }

    public Json add(String key, Iterable<Json> iterable) {
        if (iterable != null) {
            Stream<Json> jsonStream = StreamSupport.stream(iterable.spliterator(), false);
            properties.put(key, jsonStream.map(Json::asMap).collect(Collectors.toList()));
        }
        return this;
    }

    public Json append(String key, String value) {
        if (null != value) {
            List<String> values = properties.containsKey(key) ? strings(key) : new ArrayList<>();
            values.add(value);
            properties.put(key, values);
        }
        return this;
    }

    public Json append(String key, Integer value) {
        if (null != value) {
            List<Integer> values = properties.containsKey(key) ? integers(key) : new ArrayList<>();
            values.add(value);
            properties.put(key, values);
        }
        return this;
    }

    public Json append(String key, Double value) {
        if (null != value) {
            List<Double> values = properties.containsKey(key) ? decimals(key) : new ArrayList<>();
            values.add(value);
            properties.put(key, values);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public Json append(String key, Json value) {
        if (null != value) {
            List<Map<String, Object>> values = properties.containsKey(key) ? (List<Map<String, Object>>) properties.get(key) : new ArrayList<>();
            values.add(value.asMap());
            properties.put(key, values);
        }
        return this;
    }

    public boolean hasKey(String key) {
        return this.properties.containsKey(key);
    }

    public boolean hasValueFor(String key) {
        return null != this.properties.get(key);
    }

    public boolean valueEquals(String key, Object value) {
        return hasValueFor(key) && Objects.equals(get(key, value.getClass()), value);
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    public boolean remove(String key) {
        return properties.remove(key) != null;
    }

    public boolean remove(String... keys) {
        return Arrays.stream(keys)
                .map(this::remove)
                .toList()
                .stream()
                .anyMatch(result -> result);
    }

    public boolean remove(List<String> keys) {
        if (keys == null) {
            return false;
        }
        return keys.stream()
                .map(this::remove)
                .toList()
                .stream()
                .anyMatch(result -> result);
    }

    public <T> T get(String key, Class<T> type) {
        return JSON_MAPPER.convertValue(properties.get(key), type);
    }

    public <T> T convertTo(Class<T> type) {
        try {
            return JSON_MAPPER.convertValue(this, type);
        } catch (Exception e) {
            throw new JsonParseException(e);
        }
    }

    public Json object(String key) {
        return JSON_MAPPER.convertValue(properties.get(key), JSON_TYPE);
    }

    public Json objectOr(String key, Json object) {
        Json result = object(key);
        return result == null ? object : result;
    }

    public Json objectOrThrow(String key) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw new PropertyRequiredException();
        }
        return JSON_MAPPER.convertValue(properties.get(key), JSON_TYPE);
    }

    public Json objectOrThrow(String key, RuntimeException e) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw e;
        }
        return JSON_MAPPER.convertValue(properties.get(key), JSON_TYPE);
    }

    public Optional<Json> seek(String key) {
        return Optional.ofNullable(object(key));
    }

    public Stream<Json> stream(String key) {
        return seekArray(key).stream().flatMap(Collection::stream);
    }

    public List<Json> array(String key) {
        return JSON_MAPPER.convertValue(properties.get(key), LIST_TYPE_JSON);
    }

    public Optional<List<Json>> seekArray(String key) {
        return Optional.ofNullable(array(key));
    }

    public String string(String key) {
        Object value = properties.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    public String stringOr(String key, String defaultValue) {
        try {
            if (properties.containsKey(key) && null != properties.get(key)) {
                return string(key);
            }
        } catch (Exception e) {
            // return default on parsing error
        }
        return defaultValue;
    }

    public String stringOrThrow(String key) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw new PropertyRequiredException();
        }
        return string(key);
    }

    public String stringOrThrow(String key, RuntimeException e) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw e;
        }
        return string(key);
    }

    public List<String> strings(String key) {
        return JSON_MAPPER.convertValue(properties.get(key), LIST_TYPE_STRING);
    }

    public Integer integer(String key) {
        try {
            return get(key, Integer.class);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing value to integer for key " + key, e);
        }
    }

    public int integerOr(String key, int defaultValue) {
        try {
            if (properties.containsKey(key) && null != properties.get(key)) {
                return integer(key);
            }
        } catch (Exception e) {
            // return default on parsing error
        }
        return defaultValue;
    }

    public Integer integerOrThrow(String key) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw new PropertyRequiredException();
        }
        return integer(key);
    }

    public Integer integerOrThrow(String key, RuntimeException e) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw e;
        }
        return integer(key);
    }

    public List<Integer> integers(String key) {
        try {
            return JSON_MAPPER.convertValue(properties.get(key), LIST_TYPE_INTEGER);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing value to integer list for key " + key, e);
        }
    }

    public Long longInt(String key) {
        try {
            return get(key, Long.class);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing value to long for key " + key, e);
        }
    }

    public long longOr(String key, long defaultValue) {
        try {
            if (properties.containsKey(key) && null != properties.get(key)) {
                return longInt(key);
            }
        } catch (Exception e) {
            // return default on parsing error
        }
        return defaultValue;
    }

    public Long longOrThrow(String key) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw new PropertyRequiredException();
        }
        return longInt(key);
    }

    public Long longOrThrow(String key, RuntimeException e) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw e;
        }
        return longInt(key);
    }

    public List<Long> longs(String key) {
        try {
            return JSON_MAPPER.convertValue(properties.get(key), LIST_TYPE_LONG);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing value to long list for key " + key, e);
        }
    }

    public Double decimal(String key) {
        try {
            return get(key, Double.class);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing value to double for key " + key, e);
        }
    }

    public double decimalOr(String key, double defaultValue) {
        try {
            if (properties.containsKey(key) && null != properties.get(key)) {
                return decimal(key);
            }
        } catch (Exception e) {
            // return default on parsing error
        }
        return defaultValue;
    }

    public Double decimalOrThrow(String key) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw new PropertyRequiredException();
        }
        return decimal(key);
    }

    public Double decimalOrThrow(String key, RuntimeException e) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw e;
        }
        return decimal(key);
    }

    public List<Double> decimals(String key) {
        try {
            return JSON_MAPPER.convertValue(properties.get(key), LIST_TYPE_DOUBLE);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing value to double list for key " + key, e);
        }
    }

    public BigDecimal bigDecimal(String key) {
        try {
            return get(key, BigDecimal.class);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing value to big decimal for key " + key, e);
        }
    }

    public BigDecimal bigDecimalOr(String key, BigDecimal defaultValue) {
        try {
            if (properties.containsKey(key) && null != properties.get(key)) {
                return bigDecimal(key);
            }
        } catch (Exception e) {
            // return default on parsing error
        }
        return defaultValue;
    }

    public BigDecimal bigDecimalOrThrow(String key) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw new PropertyRequiredException();
        }
        return bigDecimal(key);
    }

    public BigDecimal bigDecimalOrThrow(String key, RuntimeException e) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw e;
        }
        return bigDecimal(key);
    }

    public List<BigDecimal> bigDecimals(String key) {
        try {
            return JSON_MAPPER.convertValue(properties.get(key), LIST_TYPE_BIG_DECIMAL);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing value to big decimal list for key " + key, e);
        }
    }

    public Boolean bool(String key) {
        try {
            return get(key, Boolean.class);
        } catch (Exception e) {
            throw new JsonParseException("Error parsing value to boolean for key " + key, e);
        }
    }

    public boolean boolOr(String key, boolean defaultValue) {
        try {
            if (properties.containsKey(key) && null != properties.get(key)) {
                return bool(key);
            }
        } catch (Exception e) {
            // return default on parsing error
        }
        return defaultValue;
    }

    public Boolean boolOrThrow(String key) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw new PropertyRequiredException();
        }
        return bool(key);
    }

    public Boolean boolOrThrow(String key, RuntimeException e) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw e;
        }
        return bool(key);
    }

    public boolean isTrue(String key) {
        return Boolean.TRUE.equals(bool(key));
    }

    public boolean isFalse(String key) {
        return Boolean.FALSE.equals(bool(key));
    }

    public boolean anyTrue(String... keys) {
        return Arrays.stream(keys).anyMatch(this::isTrue);
    }

    public boolean anyFalse(String... keys) {
        return !allTrue(keys);
    }

    public boolean allTrue(String... keys) {
        return Arrays.stream(keys).allMatch(this::isTrue);
    }

    public boolean allFalse(String... keys) {
        return Arrays.stream(keys).noneMatch(this::isTrue);
    }

    public Date date(String key, String format) {
        return parseDate(string(key), format, ZoneId.systemDefault());
    }

    public Date dateOrNow(String key, String format) {
        if (properties.containsKey(key) && null != string(key)) {
            return parseDate(string(key), format, ZoneId.systemDefault());
        }
        return new Date();
    }

    public Date dateOrThrow(String key, String format) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw new PropertyRequiredException();
        }
        return date(key, format);
    }

    public Date dateOrThrow(String key, String format, RuntimeException e) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw e;
        }
        return date(key, format);
    }

    public Date date(String key, String format, String timeZone) {
        return parseDate(string(key), format, ZoneId.of(timeZone));
    }

    public Date date(String dateKey, String timeKey, String joinString, String format) {
        String dateTime = string(dateKey).concat(joinString).concat(string(timeKey));
        return parseDate(dateTime, format, ZoneId.systemDefault());
    }

    public LocalDate localDate(String key, String format) {
        return LocalDate.parse(string(key), DateTimeFormatter.ofPattern(format));
    }

    public LocalDate localDateOr(String key, String format, LocalDate defaultValue) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            return defaultValue;
        }
        return LocalDate.parse(string(key), DateTimeFormatter.ofPattern(format));
    }

    public LocalDate localDateOrToday(String key, String format) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            return LocalDate.now();
        }
        return LocalDate.parse(string(key), DateTimeFormatter.ofPattern(format));
    }

    public LocalDate localDateOrThrow(String key, String format, RuntimeException e) {
        if (!properties.containsKey(key) || null == properties.get(key)) {
            throw e;
        }
        return localDate(key, format);
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
        consumer.accept(JSON_MAPPER.convertValue(properties.get(key), LIST_TYPE_JSON));
        return this;
    }

    public Json strings(String key, Consumer<List<String>> consumer) {
        consumer.accept(JSON_MAPPER.convertValue(properties.get(key), LIST_TYPE_STRING));
        return this;
    }

    public Json integers(String key, Consumer<List<Integer>> consumer) {
        consumer.accept(JSON_MAPPER.convertValue(properties.get(key), LIST_TYPE_INTEGER));
        return this;
    }

    public Json longInt(String key, Consumer<Long> consumer) {
        consumer.accept(longInt(key));
        return this;
    }

    public Json longs(String key, Consumer<List<Long>> consumer) {
        consumer.accept(JSON_MAPPER.convertValue(properties.get(key), LIST_TYPE_LONG));
        return this;
    }

    public Json decimals(String key, Consumer<List<Double>> consumer) {
        consumer.accept(JSON_MAPPER.convertValue(properties.get(key), LIST_TYPE_DOUBLE));
        return this;
    }

    public Json bigDecimal(String key, Consumer<BigDecimal> consumer) {
        consumer.accept(bigDecimal(key));
        return this;
    }

    public Json bigDecimals(String key, Consumer<List<BigDecimal>> consumer) {
        consumer.accept(JSON_MAPPER.convertValue(properties.get(key), LIST_TYPE_BIG_DECIMAL));
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> Json map(String key, Consumer<T> consumer) {
        consumer.accept((T) properties.get(key));
        return this;
    }

    @SuppressWarnings({"unchecked", "parameters"})
    public <T> T find(String key, Class<T> type) {
        if (properties.keySet().stream().anyMatch(k -> k.equals(key))) {
            return get(key, type);
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getValue() instanceof Map) {
                T value = JJson.from(entry.getValue()).orElse(JJson.create()).find(key, type);
                if (null != value) {
                    return value;
                }
            } else if (entry.getValue() instanceof List && !((List<?>) entry.getValue()).isEmpty() && ((List<?>) entry.getValue()).getFirst() instanceof Map) {
                T value = ((List<Map<String, Object>>) entry.getValue())
                        .stream()
                        .map(map -> JJson.create().put(map).find(key, type))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
                if (null != value) {
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
        } catch (JacksonException e) {
            throw new JsonParseException(e);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        JJson json = (JJson) other;
        return Objects.equals(properties, json.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties);
    }

    private static final ConcurrentMap<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    private static DateTimeFormatter formatterFor(String pattern) {
        return FORMATTER_CACHE.computeIfAbsent(pattern, p -> {
            DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().appendPattern(p);
            if (needsAmPmDefault(p)) {
                builder.parseDefaulting(ChronoField.AMPM_OF_DAY, 0);
            }
            return builder.toFormatter();
        });
    }

    private static boolean needsAmPmDefault(String pattern) {
        boolean inLiteral = false;
        boolean hasClockHour = false;
        boolean hasAmPm = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                inLiteral = !inLiteral;
            } else if (!inLiteral) {
                if (c == 'h' || c == 'K') hasClockHour = true;
                if (c == 'a' || c == 'B') hasAmPm = true;
            }
        }
        return hasClockHour && !hasAmPm;
    }

    private static Date parseDate(String dateString, String format, ZoneId fallbackZone) {
        try {
            TemporalAccessor ta = formatterFor(format).parseBest(dateString,
                    ZonedDateTime::from,
                    OffsetDateTime::from,
                    LocalDateTime::from,
                    LocalDate::from);
            return switch (ta) {
                case ZonedDateTime zdt -> Date.from(zdt.toInstant());
                case OffsetDateTime odt -> Date.from(odt.toInstant());
                case LocalDateTime ldt -> Date.from(ldt.atZone(fallbackZone).toInstant());
                case LocalDate ld -> Date.from(ld.atStartOfDay(fallbackZone).toInstant());
                default -> throw new IllegalStateException("Unexpected temporal type: " + ta);
            };
        } catch (DateTimeParseException e) {
            throw new JsonParseException("Error parsing value to date " + dateString, e);
        }
    }
}

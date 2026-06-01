package com.yupzip.json.jackson;

import com.yupzip.json.JsonParseException;

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
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class JsonDateUtil {

    private static final ConcurrentMap<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    private JsonDateUtil() {}

    static Date parseDate(String dateString, String format, ZoneId fallbackZone) {
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
                if (c == 'h' || c == 'K') {
                    hasClockHour = true;
                }
                if (c == 'a' || c == 'B') {
                    hasAmPm = true;
                }
            }
        }
        return hasClockHour && !hasAmPm;
    }
}

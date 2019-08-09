package org.springframework.format.datetime.joda;

import org.joda.time.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.format.datetime.DateFormatterRegistrar;

import java.util.Calendar;
import java.util.Date;

final class JodaTimeConverters {

    private JodaTimeConverters() {
    }

    @SuppressWarnings("deprecation")
    public static void registerConverters(ConverterRegistry registry) {
        DateFormatterRegistrar.addDateConverters(registry);
        registry.addConverter(new DateTimeToLocalDateConverter());
        registry.addConverter(new DateTimeToLocalTimeConverter());
        registry.addConverter(new DateTimeToLocalDateTimeConverter());
        registry.addConverter(new DateTimeToDateMidnightConverter());
        registry.addConverter(new DateTimeToMutableDateTimeConverter());
        registry.addConverter(new DateTimeToInstantConverter());
        registry.addConverter(new DateTimeToDateConverter());
        registry.addConverter(new DateTimeToCalendarConverter());
        registry.addConverter(new DateTimeToLongConverter());
        registry.addConverter(new DateToReadableInstantConverter());
        registry.addConverter(new CalendarToReadableInstantConverter());
        registry.addConverter(new LongToReadableInstantConverter());
        registry.addConverter(new LocalDateTimeToLocalDateConverter());
        registry.addConverter(new LocalDateTimeToLocalTimeConverter());
    }

    private static class DateTimeToLocalDateConverter implements Converter<DateTime, LocalDate> {

        @Override
        public LocalDate convert(DateTime source) {
            return source.toLocalDate();
        }

    }

    private static class DateTimeToLocalTimeConverter implements Converter<DateTime, LocalTime> {

        @Override
        public LocalTime convert(DateTime source) {
            return source.toLocalTime();
        }

    }

    private static class DateTimeToLocalDateTimeConverter implements Converter<DateTime, LocalDateTime> {

        @Override
        public LocalDateTime convert(DateTime source) {
            return source.toLocalDateTime();
        }

    }

    @Deprecated
    private static class DateTimeToDateMidnightConverter implements Converter<DateTime, org.joda.time.DateMidnight> {

        @Override
        public org.joda.time.DateMidnight convert(DateTime source) {
            return source.toDateMidnight();
        }

    }

    private static class DateTimeToMutableDateTimeConverter implements Converter<DateTime, MutableDateTime> {

        @Override
        public MutableDateTime convert(DateTime source) {
            return source.toMutableDateTime();
        }

    }

    private static class DateTimeToInstantConverter implements Converter<DateTime, Instant> {

        @Override
        public Instant convert(DateTime source) {
            return source.toInstant();
        }

    }

    private static class DateTimeToDateConverter implements Converter<DateTime, Date> {

        @Override
        public Date convert(DateTime source) {
            return source.toDate();
        }

    }

    private static class DateTimeToCalendarConverter implements Converter<DateTime, Calendar> {

        @Override
        public Calendar convert(DateTime source) {
            return source.toGregorianCalendar();
        }

    }

    private static class DateTimeToLongConverter implements Converter<DateTime, Long> {

        @Override
        public Long convert(DateTime source) {
            return source.getMillis();
        }

    }

    private static class DateToReadableInstantConverter implements Converter<Date, ReadableInstant> {

        @Override
        public ReadableInstant convert(Date source) {
            return new DateTime(source);
        }

    }

    private static class CalendarToReadableInstantConverter implements Converter<Calendar, ReadableInstant> {

        @Override
        public ReadableInstant convert(Calendar source) {
            return new DateTime(source);
        }

    }

    private static class LongToReadableInstantConverter implements Converter<Long, ReadableInstant> {

        @Override
        public ReadableInstant convert(Long source) {
            return new DateTime(source.longValue());
        }

    }

    private static class LocalDateTimeToLocalDateConverter implements Converter<LocalDateTime, LocalDate> {

        @Override
        public LocalDate convert(LocalDateTime source) {
            return source.toLocalDate();
        }

    }

    private static class LocalDateTimeToLocalTimeConverter implements Converter<LocalDateTime, LocalTime> {

        @Override
        public LocalTime convert(LocalDateTime source) {
            return source.toLocalTime();
        }

    }

}

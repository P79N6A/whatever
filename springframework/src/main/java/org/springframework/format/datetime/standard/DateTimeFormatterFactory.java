package org.springframework.format.datetime.standard;

import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;
import java.util.TimeZone;

public class DateTimeFormatterFactory {

    @Nullable
    private String pattern;

    @Nullable
    private ISO iso;

    @Nullable
    private FormatStyle dateStyle;

    @Nullable
    private FormatStyle timeStyle;

    @Nullable
    private TimeZone timeZone;

    public DateTimeFormatterFactory() {
    }

    public DateTimeFormatterFactory(String pattern) {
        this.pattern = pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setIso(ISO iso) {
        this.iso = iso;
    }

    public void setDateStyle(FormatStyle dateStyle) {
        this.dateStyle = dateStyle;
    }

    public void setTimeStyle(FormatStyle timeStyle) {
        this.timeStyle = timeStyle;
    }

    public void setDateTimeStyle(FormatStyle dateTimeStyle) {
        this.dateStyle = dateTimeStyle;
        this.timeStyle = dateTimeStyle;
    }

    public void setStylePattern(String style) {
        Assert.isTrue(style.length() == 2, "Style pattern must consist of two characters");
        this.dateStyle = convertStyleCharacter(style.charAt(0));
        this.timeStyle = convertStyleCharacter(style.charAt(1));
    }

    @Nullable
    private FormatStyle convertStyleCharacter(char c) {
        switch (c) {
            case 'S':
                return FormatStyle.SHORT;
            case 'M':
                return FormatStyle.MEDIUM;
            case 'L':
                return FormatStyle.LONG;
            case 'F':
                return FormatStyle.FULL;
            case '-':
                return null;
            default:
                throw new IllegalArgumentException("Invalid style character '" + c + "'");
        }
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public DateTimeFormatter createDateTimeFormatter() {
        return createDateTimeFormatter(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
    }

    public DateTimeFormatter createDateTimeFormatter(DateTimeFormatter fallbackFormatter) {
        DateTimeFormatter dateTimeFormatter = null;
        if (StringUtils.hasLength(this.pattern)) {
            // Using strict parsing to align with Joda-Time and standard DateFormat behavior:
            // otherwise, an overflow like e.g. Feb 29 for a non-leap-year wouldn't get rejected.
            // However, with strict parsing, a year digit needs to be specified as 'u'...
            String patternToUse = StringUtils.replace(this.pattern, "yy", "uu");
            dateTimeFormatter = DateTimeFormatter.ofPattern(patternToUse).withResolverStyle(ResolverStyle.STRICT);
        } else if (this.iso != null && this.iso != ISO.NONE) {
            switch (this.iso) {
                case DATE:
                    dateTimeFormatter = DateTimeFormatter.ISO_DATE;
                    break;
                case TIME:
                    dateTimeFormatter = DateTimeFormatter.ISO_TIME;
                    break;
                case DATE_TIME:
                    dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                    break;
                default:
                    throw new IllegalStateException("Unsupported ISO format: " + this.iso);
            }
        } else if (this.dateStyle != null && this.timeStyle != null) {
            dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(this.dateStyle, this.timeStyle);
        } else if (this.dateStyle != null) {
            dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(this.dateStyle);
        } else if (this.timeStyle != null) {
            dateTimeFormatter = DateTimeFormatter.ofLocalizedTime(this.timeStyle);
        }
        if (dateTimeFormatter != null && this.timeZone != null) {
            dateTimeFormatter = dateTimeFormatter.withZone(this.timeZone.toZoneId());
        }
        return (dateTimeFormatter != null ? dateTimeFormatter : fallbackFormatter);
    }

}

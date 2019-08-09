package org.springframework.format.datetime;

import org.springframework.format.Formatter;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateFormatter implements Formatter<Date> {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static final Map<ISO, String> ISO_PATTERNS;

    static {
        Map<ISO, String> formats = new EnumMap<>(ISO.class);
        formats.put(ISO.DATE, "yyyy-MM-dd");
        formats.put(ISO.TIME, "HH:mm:ss.SSSXXX");
        formats.put(ISO.DATE_TIME, "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        ISO_PATTERNS = Collections.unmodifiableMap(formats);
    }

    @Nullable
    private String pattern;

    private int style = DateFormat.DEFAULT;

    @Nullable
    private String stylePattern;

    @Nullable
    private ISO iso;

    @Nullable
    private TimeZone timeZone;

    private boolean lenient = false;

    public DateFormatter() {
    }

    public DateFormatter(String pattern) {
        this.pattern = pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setIso(ISO iso) {
        this.iso = iso;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public void setStylePattern(String stylePattern) {
        this.stylePattern = stylePattern;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }

    @Override
    public String print(Date date, Locale locale) {
        return getDateFormat(locale).format(date);
    }

    @Override
    public Date parse(String text, Locale locale) throws ParseException {
        return getDateFormat(locale).parse(text);
    }

    protected DateFormat getDateFormat(Locale locale) {
        DateFormat dateFormat = createDateFormat(locale);
        if (this.timeZone != null) {
            dateFormat.setTimeZone(this.timeZone);
        }
        dateFormat.setLenient(this.lenient);
        return dateFormat;
    }

    private DateFormat createDateFormat(Locale locale) {
        if (StringUtils.hasLength(this.pattern)) {
            return new SimpleDateFormat(this.pattern, locale);
        }
        if (this.iso != null && this.iso != ISO.NONE) {
            String pattern = ISO_PATTERNS.get(this.iso);
            if (pattern == null) {
                throw new IllegalStateException("Unsupported ISO format " + this.iso);
            }
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            format.setTimeZone(UTC);
            return format;
        }
        if (StringUtils.hasLength(this.stylePattern)) {
            int dateStyle = getStylePatternForChar(0);
            int timeStyle = getStylePatternForChar(1);
            if (dateStyle != -1 && timeStyle != -1) {
                return DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
            }
            if (dateStyle != -1) {
                return DateFormat.getDateInstance(dateStyle, locale);
            }
            if (timeStyle != -1) {
                return DateFormat.getTimeInstance(timeStyle, locale);
            }
            throw new IllegalStateException("Unsupported style pattern '" + this.stylePattern + "'");

        }
        return DateFormat.getDateInstance(this.style, locale);
    }

    private int getStylePatternForChar(int index) {
        if (this.stylePattern != null && this.stylePattern.length() > index) {
            switch (this.stylePattern.charAt(index)) {
                case 'S':
                    return DateFormat.SHORT;
                case 'M':
                    return DateFormat.MEDIUM;
                case 'L':
                    return DateFormat.LONG;
                case 'F':
                    return DateFormat.FULL;
                case '-':
                    return -1;
            }
        }
        throw new IllegalStateException("Unsupported style pattern '" + this.stylePattern + "'");
    }

}

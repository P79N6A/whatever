package org.springframework.format.datetime.joda;

import org.joda.time.format.DateTimeFormatter;
import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

import java.util.Locale;

public final class JodaTimeContextHolder {

    private static final ThreadLocal<JodaTimeContext> jodaTimeContextHolder = new NamedThreadLocal<>("JodaTimeContext");

    private JodaTimeContextHolder() {
    }

    public static void resetJodaTimeContext() {
        jodaTimeContextHolder.remove();
    }

    public static void setJodaTimeContext(@Nullable JodaTimeContext jodaTimeContext) {
        if (jodaTimeContext == null) {
            resetJodaTimeContext();
        } else {
            jodaTimeContextHolder.set(jodaTimeContext);
        }
    }

    @Nullable
    public static JodaTimeContext getJodaTimeContext() {
        return jodaTimeContextHolder.get();
    }

    public static DateTimeFormatter getFormatter(DateTimeFormatter formatter, @Nullable Locale locale) {
        DateTimeFormatter formatterToUse = (locale != null ? formatter.withLocale(locale) : formatter);
        JodaTimeContext context = getJodaTimeContext();
        return (context != null ? context.getFormatter(formatterToUse) : formatterToUse);
    }

}

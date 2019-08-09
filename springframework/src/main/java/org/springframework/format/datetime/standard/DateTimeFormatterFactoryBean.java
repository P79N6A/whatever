package org.springframework.format.datetime.standard;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import java.time.format.DateTimeFormatter;

public class DateTimeFormatterFactoryBean extends DateTimeFormatterFactory implements FactoryBean<DateTimeFormatter>, InitializingBean {

    @Nullable
    private DateTimeFormatter dateTimeFormatter;

    @Override
    public void afterPropertiesSet() {
        this.dateTimeFormatter = createDateTimeFormatter();
    }

    @Override
    @Nullable
    public DateTimeFormatter getObject() {
        return this.dateTimeFormatter;
    }

    @Override
    public Class<?> getObjectType() {
        return DateTimeFormatter.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}

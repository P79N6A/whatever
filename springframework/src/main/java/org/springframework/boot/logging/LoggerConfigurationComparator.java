package org.springframework.boot.logging;

import org.springframework.util.Assert;

import java.util.Comparator;

class LoggerConfigurationComparator implements Comparator<LoggerConfiguration> {

    private final String rootLoggerName;

    LoggerConfigurationComparator(String rootLoggerName) {
        Assert.notNull(rootLoggerName, "RootLoggerName must not be null");
        this.rootLoggerName = rootLoggerName;
    }

    @Override
    public int compare(LoggerConfiguration o1, LoggerConfiguration o2) {
        if (this.rootLoggerName.equals(o1.getName())) {
            return -1;
        }
        if (this.rootLoggerName.equals(o2.getName())) {
            return 1;
        }
        return o1.getName().compareTo(o2.getName());
    }

}

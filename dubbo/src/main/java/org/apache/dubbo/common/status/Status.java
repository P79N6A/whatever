package org.apache.dubbo.common.status;

public class Status {

    private final Level level;

    private final String message;

    private final String description;

    public Status(Level level) {
        this(level, null, null);
    }

    public Status(Level level, String message) {
        this(level, message, null);
    }

    public Status(Level level, String message, String description) {
        this.level = level;
        this.message = message;
        this.description = description;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

    public enum Level {

        OK,

        WARN,

        ERROR,

        UNKNOWN
    }

}
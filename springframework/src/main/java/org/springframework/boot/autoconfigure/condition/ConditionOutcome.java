package org.springframework.boot.autoconfigure.condition;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

public class ConditionOutcome {

    private final boolean match;

    private final ConditionMessage message;

    public ConditionOutcome(boolean match, String message) {
        this(match, ConditionMessage.of(message));
    }

    public ConditionOutcome(boolean match, ConditionMessage message) {
        Assert.notNull(message, "ConditionMessage must not be null");
        this.match = match;
        this.message = message;
    }

    public static ConditionOutcome match() {
        return match(ConditionMessage.empty());
    }

    public static ConditionOutcome match(String message) {
        return new ConditionOutcome(true, message);
    }

    public static ConditionOutcome match(ConditionMessage message) {
        return new ConditionOutcome(true, message);
    }

    public static ConditionOutcome noMatch(String message) {
        return new ConditionOutcome(false, message);
    }

    public static ConditionOutcome noMatch(ConditionMessage message) {
        return new ConditionOutcome(false, message);
    }

    public boolean isMatch() {
        return this.match;
    }

    public String getMessage() {
        return this.message.isEmpty() ? null : this.message.toString();
    }

    public ConditionMessage getConditionMessage() {
        return this.message;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() == obj.getClass()) {
            ConditionOutcome other = (ConditionOutcome) obj;
            return (this.match == other.match && ObjectUtils.nullSafeEquals(this.message, other.message));
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(this.match) * 31 + ObjectUtils.nullSafeHashCode(this.message);
    }

    @Override
    public String toString() {
        return (this.message != null) ? this.message.toString() : "";
    }

    public static ConditionOutcome inverse(ConditionOutcome outcome) {
        return new ConditionOutcome(!outcome.isMatch(), outcome.getConditionMessage());
    }

}

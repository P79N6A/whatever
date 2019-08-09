package org.springframework.web.servlet.mvc.condition;

import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.StringJoiner;

public abstract class AbstractRequestCondition<T extends AbstractRequestCondition<T>> implements RequestCondition<T> {

    public boolean isEmpty() {
        return getContent().isEmpty();
    }

    protected abstract Collection<?> getContent();

    protected abstract String getToStringInfix();

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return getContent().equals(((AbstractRequestCondition<?>) other).getContent());
    }

    @Override
    public int hashCode() {
        return getContent().hashCode();
    }

    @Override
    public String toString() {
        String infix = getToStringInfix();
        StringJoiner joiner = new StringJoiner(infix, "[", "]");
        for (Object expression : getContent()) {
            joiner.add(expression.toString());
        }
        return joiner.toString();
    }

}

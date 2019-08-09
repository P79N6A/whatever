package org.springframework.web.servlet.mvc.condition;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;

public final class RequestConditionHolder extends AbstractRequestCondition<RequestConditionHolder> {

    @Nullable
    private final RequestCondition<Object> condition;

    @SuppressWarnings("unchecked")
    public RequestConditionHolder(@Nullable RequestCondition<?> requestCondition) {
        this.condition = (RequestCondition<Object>) requestCondition;
    }

    @Nullable
    public RequestCondition<?> getCondition() {
        return this.condition;
    }

    @Override
    protected Collection<?> getContent() {
        return (this.condition != null ? Collections.singleton(this.condition) : Collections.emptyList());
    }

    @Override
    protected String getToStringInfix() {
        return " ";
    }

    @Override
    public RequestConditionHolder combine(RequestConditionHolder other) {
        if (this.condition == null && other.condition == null) {
            return this;
        } else if (this.condition == null) {
            return other;
        } else if (other.condition == null) {
            return this;
        } else {
            assertEqualConditionTypes(this.condition, other.condition);
            RequestCondition<?> combined = (RequestCondition<?>) this.condition.combine(other.condition);
            return new RequestConditionHolder(combined);
        }
    }

    private void assertEqualConditionTypes(RequestCondition<?> thisCondition, RequestCondition<?> otherCondition) {
        Class<?> clazz = thisCondition.getClass();
        Class<?> otherClazz = otherCondition.getClass();
        if (!clazz.equals(otherClazz)) {
            throw new ClassCastException("Incompatible request conditions: " + clazz + " and " + otherClazz);
        }
    }

    @Override
    @Nullable
    public RequestConditionHolder getMatchingCondition(HttpServletRequest request) {
        if (this.condition == null) {
            return this;
        }
        RequestCondition<?> match = (RequestCondition<?>) this.condition.getMatchingCondition(request);
        return (match != null ? new RequestConditionHolder(match) : null);
    }

    @Override
    public int compareTo(RequestConditionHolder other, HttpServletRequest request) {
        if (this.condition == null && other.condition == null) {
            return 0;
        } else if (this.condition == null) {
            return 1;
        } else if (other.condition == null) {
            return -1;
        } else {
            assertEqualConditionTypes(this.condition, other.condition);
            return this.condition.compareTo(other.condition, request);
        }
    }

}

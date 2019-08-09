package org.springframework.web.servlet.mvc.condition;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.cors.CorsUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class HeadersRequestCondition extends AbstractRequestCondition<HeadersRequestCondition> {

    private static final HeadersRequestCondition PRE_FLIGHT_MATCH = new HeadersRequestCondition();

    private final Set<HeaderExpression> expressions;

    public HeadersRequestCondition(String... headers) {
        this(parseExpressions(headers));
    }

    private HeadersRequestCondition(Set<HeaderExpression> conditions) {
        this.expressions = conditions;
    }

    private static Set<HeaderExpression> parseExpressions(String... headers) {
        Set<HeaderExpression> expressions = new LinkedHashSet<>();
        for (String header : headers) {
            HeaderExpression expr = new HeaderExpression(header);
            if ("Accept".equalsIgnoreCase(expr.name) || "Content-Type".equalsIgnoreCase(expr.name)) {
                continue;
            }
            expressions.add(expr);
        }
        return expressions;
    }

    public Set<NameValueExpression<String>> getExpressions() {
        return new LinkedHashSet<>(this.expressions);
    }

    @Override
    protected Collection<HeaderExpression> getContent() {
        return this.expressions;
    }

    @Override
    protected String getToStringInfix() {
        return " && ";
    }

    @Override
    public HeadersRequestCondition combine(HeadersRequestCondition other) {
        Set<HeaderExpression> set = new LinkedHashSet<>(this.expressions);
        set.addAll(other.expressions);
        return new HeadersRequestCondition(set);
    }

    @Override
    @Nullable
    public HeadersRequestCondition getMatchingCondition(HttpServletRequest request) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return PRE_FLIGHT_MATCH;
        }
        for (HeaderExpression expression : this.expressions) {
            if (!expression.match(request)) {
                return null;
            }
        }
        return this;
    }

    @Override
    public int compareTo(HeadersRequestCondition other, HttpServletRequest request) {
        int result = other.expressions.size() - this.expressions.size();
        if (result != 0) {
            return result;
        }
        return (int) (getValueMatchCount(other.expressions) - getValueMatchCount(this.expressions));
    }

    private long getValueMatchCount(Set<HeaderExpression> expressions) {
        long count = 0;
        for (HeaderExpression e : expressions) {
            if (e.getValue() != null && !e.isNegated()) {
                count++;
            }
        }
        return count;
    }

    static class HeaderExpression extends AbstractNameValueExpression<String> {

        public HeaderExpression(String expression) {
            super(expression);
        }

        @Override
        protected boolean isCaseSensitiveName() {
            return false;
        }

        @Override
        protected String parseValue(String valueExpression) {
            return valueExpression;
        }

        @Override
        protected boolean matchName(HttpServletRequest request) {
            return (request.getHeader(this.name) != null);
        }

        @Override
        protected boolean matchValue(HttpServletRequest request) {
            return ObjectUtils.nullSafeEquals(this.value, request.getHeader(this.name));
        }

    }

}

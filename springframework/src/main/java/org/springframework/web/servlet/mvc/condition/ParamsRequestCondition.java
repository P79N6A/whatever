package org.springframework.web.servlet.mvc.condition;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public final class ParamsRequestCondition extends AbstractRequestCondition<ParamsRequestCondition> {

    private final Set<ParamExpression> expressions;

    public ParamsRequestCondition(String... params) {
        this(parseExpressions(params));
    }

    private ParamsRequestCondition(Collection<ParamExpression> conditions) {
        this.expressions = Collections.unmodifiableSet(new LinkedHashSet<>(conditions));
    }

    private static Collection<ParamExpression> parseExpressions(String... params) {
        Set<ParamExpression> expressions = new LinkedHashSet<>();
        for (String param : params) {
            expressions.add(new ParamExpression(param));
        }
        return expressions;
    }

    public Set<NameValueExpression<String>> getExpressions() {
        return new LinkedHashSet<>(this.expressions);
    }

    @Override
    protected Collection<ParamExpression> getContent() {
        return this.expressions;
    }

    @Override
    protected String getToStringInfix() {
        return " && ";
    }

    @Override
    public ParamsRequestCondition combine(ParamsRequestCondition other) {
        Set<ParamExpression> set = new LinkedHashSet<>(this.expressions);
        set.addAll(other.expressions);
        return new ParamsRequestCondition(set);
    }

    @Override
    @Nullable
    public ParamsRequestCondition getMatchingCondition(HttpServletRequest request) {
        for (ParamExpression expression : this.expressions) {
            if (!expression.match(request)) {
                return null;
            }
        }
        return this;
    }

    @Override
    public int compareTo(ParamsRequestCondition other, HttpServletRequest request) {
        int result = other.expressions.size() - this.expressions.size();
        if (result != 0) {
            return result;
        }
        return (int) (getValueMatchCount(other.expressions) - getValueMatchCount(this.expressions));
    }

    private long getValueMatchCount(Set<ParamExpression> expressions) {
        long count = 0;
        for (ParamExpression e : expressions) {
            if (e.getValue() != null && !e.isNegated()) {
                count++;
            }
        }
        return count;
    }

    static class ParamExpression extends AbstractNameValueExpression<String> {

        private final Set<String> namesToMatch = new HashSet<>(WebUtils.SUBMIT_IMAGE_SUFFIXES.length + 1);

        ParamExpression(String expression) {
            super(expression);
            this.namesToMatch.add(getName());
            for (String suffix : WebUtils.SUBMIT_IMAGE_SUFFIXES) {
                this.namesToMatch.add(getName() + suffix);
            }
        }

        @Override
        protected boolean isCaseSensitiveName() {
            return true;
        }

        @Override
        protected String parseValue(String valueExpression) {
            return valueExpression;
        }

        @Override
        protected boolean matchName(HttpServletRequest request) {
            for (String current : this.namesToMatch) {
                if (request.getParameterMap().get(current) != null) {
                    return true;
                }
            }
            return request.getParameterMap().containsKey(this.name);
        }

        @Override
        protected boolean matchValue(HttpServletRequest request) {
            return ObjectUtils.nullSafeEquals(this.value, request.getParameter(this.name));
        }

    }

}

package org.springframework.web.servlet.mvc.condition;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CompositeRequestCondition extends AbstractRequestCondition<CompositeRequestCondition> {

    private final RequestConditionHolder[] requestConditions;

    public CompositeRequestCondition(RequestCondition<?>... requestConditions) {
        this.requestConditions = wrap(requestConditions);
    }

    private CompositeRequestCondition(RequestConditionHolder[] requestConditions) {
        this.requestConditions = requestConditions;
    }

    private RequestConditionHolder[] wrap(RequestCondition<?>... rawConditions) {
        RequestConditionHolder[] wrappedConditions = new RequestConditionHolder[rawConditions.length];
        for (int i = 0; i < rawConditions.length; i++) {
            wrappedConditions[i] = new RequestConditionHolder(rawConditions[i]);
        }
        return wrappedConditions;
    }

    @Override
    public boolean isEmpty() {
        return ObjectUtils.isEmpty(this.requestConditions);
    }

    public List<RequestCondition<?>> getConditions() {
        return unwrap();
    }

    private List<RequestCondition<?>> unwrap() {
        List<RequestCondition<?>> result = new ArrayList<>();
        for (RequestConditionHolder holder : this.requestConditions) {
            result.add(holder.getCondition());
        }
        return result;
    }

    @Override
    protected Collection<?> getContent() {
        return (!isEmpty() ? getConditions() : Collections.emptyList());
    }

    @Override
    protected String getToStringInfix() {
        return " && ";
    }

    private int getLength() {
        return this.requestConditions.length;
    }

    @Override
    public CompositeRequestCondition combine(CompositeRequestCondition other) {
        if (isEmpty() && other.isEmpty()) {
            return this;
        } else if (other.isEmpty()) {
            return this;
        } else if (isEmpty()) {
            return other;
        } else {
            assertNumberOfConditions(other);
            RequestConditionHolder[] combinedConditions = new RequestConditionHolder[getLength()];
            for (int i = 0; i < getLength(); i++) {
                combinedConditions[i] = this.requestConditions[i].combine(other.requestConditions[i]);
            }
            return new CompositeRequestCondition(combinedConditions);
        }
    }

    private void assertNumberOfConditions(CompositeRequestCondition other) {
        Assert.isTrue(getLength() == other.getLength(), "Cannot combine CompositeRequestConditions with a different number of conditions. " + ObjectUtils.nullSafeToString(this.requestConditions) + " and  " + ObjectUtils.nullSafeToString(other.requestConditions));
    }

    @Override
    @Nullable
    public CompositeRequestCondition getMatchingCondition(HttpServletRequest request) {
        if (isEmpty()) {
            return this;
        }
        RequestConditionHolder[] matchingConditions = new RequestConditionHolder[getLength()];
        for (int i = 0; i < getLength(); i++) {
            matchingConditions[i] = this.requestConditions[i].getMatchingCondition(request);
            if (matchingConditions[i] == null) {
                return null;
            }
        }
        return new CompositeRequestCondition(matchingConditions);
    }

    @Override
    public int compareTo(CompositeRequestCondition other, HttpServletRequest request) {
        if (isEmpty() && other.isEmpty()) {
            return 0;
        } else if (isEmpty()) {
            return 1;
        } else if (other.isEmpty()) {
            return -1;
        } else {
            assertNumberOfConditions(other);
            for (int i = 0; i < getLength(); i++) {
                int result = this.requestConditions[i].compareTo(other.requestConditions[i], request);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        }
    }

}

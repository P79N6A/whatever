package org.springframework.expression;

import org.springframework.lang.Nullable;

public interface TypeComparator {

    boolean canCompare(@Nullable Object firstObject, @Nullable Object secondObject);

    int compare(@Nullable Object firstObject, @Nullable Object secondObject) throws EvaluationException;

}

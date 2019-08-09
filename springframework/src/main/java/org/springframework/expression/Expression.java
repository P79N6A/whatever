package org.springframework.expression;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

public interface Expression {

    String getExpressionString();

    @Nullable
    Object getValue() throws EvaluationException;

    @Nullable
    <T> T getValue(@Nullable Class<T> desiredResultType) throws EvaluationException;

    @Nullable
    Object getValue(Object rootObject) throws EvaluationException;

    @Nullable
    <T> T getValue(Object rootObject, @Nullable Class<T> desiredResultType) throws EvaluationException;

    @Nullable
    Object getValue(EvaluationContext context) throws EvaluationException;

    @Nullable
    Object getValue(EvaluationContext context, Object rootObject) throws EvaluationException;

    @Nullable
    <T> T getValue(EvaluationContext context, @Nullable Class<T> desiredResultType) throws EvaluationException;

    @Nullable
    <T> T getValue(EvaluationContext context, Object rootObject, @Nullable Class<T> desiredResultType) throws EvaluationException;

    @Nullable
    Class<?> getValueType() throws EvaluationException;

    @Nullable
    Class<?> getValueType(Object rootObject) throws EvaluationException;

    @Nullable
    Class<?> getValueType(EvaluationContext context) throws EvaluationException;

    @Nullable
    Class<?> getValueType(EvaluationContext context, Object rootObject) throws EvaluationException;

    @Nullable
    TypeDescriptor getValueTypeDescriptor() throws EvaluationException;

    @Nullable
    TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException;

    @Nullable
    TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException;

    @Nullable
    TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject) throws EvaluationException;

    boolean isWritable(Object rootObject) throws EvaluationException;

    boolean isWritable(EvaluationContext context) throws EvaluationException;

    boolean isWritable(EvaluationContext context, Object rootObject) throws EvaluationException;

    void setValue(Object rootObject, @Nullable Object value) throws EvaluationException;

    void setValue(EvaluationContext context, @Nullable Object value) throws EvaluationException;

    void setValue(EvaluationContext context, Object rootObject, @Nullable Object value) throws EvaluationException;

}

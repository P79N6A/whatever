package org.springframework.expression.spel.ast;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.lang.Nullable;

import java.util.StringJoiner;

public class CompoundExpression extends SpelNodeImpl {

    public CompoundExpression(int startPos, int endPos, SpelNodeImpl... expressionComponents) {
        super(startPos, endPos, expressionComponents);
        if (expressionComponents.length < 2) {
            throw new IllegalStateException("Do not build compound expressions with less than two entries: " + expressionComponents.length);
        }
    }

    @Override
    protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
        if (getChildCount() == 1) {
            return this.children[0].getValueRef(state);
        }
        SpelNodeImpl nextNode = this.children[0];
        try {
            TypedValue result = nextNode.getValueInternal(state);
            int cc = getChildCount();
            for (int i = 1; i < cc - 1; i++) {
                try {
                    state.pushActiveContextObject(result);
                    nextNode = this.children[i];
                    result = nextNode.getValueInternal(state);
                } finally {
                    state.popActiveContextObject();
                }
            }
            try {
                state.pushActiveContextObject(result);
                nextNode = this.children[cc - 1];
                return nextNode.getValueRef(state);
            } finally {
                state.popActiveContextObject();
            }
        } catch (SpelEvaluationException ex) {
            // Correct the position for the error before re-throwing
            ex.setPosition(nextNode.getStartPosition());
            throw ex;
        }
    }

    @Override
    public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
        ValueRef ref = getValueRef(state);
        TypedValue result = ref.getValue();
        this.exitTypeDescriptor = this.children[this.children.length - 1].exitTypeDescriptor;
        return result;
    }

    @Override
    public void setValue(ExpressionState state, @Nullable Object value) throws EvaluationException {
        getValueRef(state).setValue(value);
    }

    @Override
    public boolean isWritable(ExpressionState state) throws EvaluationException {
        return getValueRef(state).isWritable();
    }

    @Override
    public String toStringAST() {
        StringJoiner sj = new StringJoiner(".");
        for (int i = 0; i < getChildCount(); i++) {
            sj.add(getChild(i).toStringAST());
        }
        return sj.toString();
    }

    @Override
    public boolean isCompilable() {
        for (SpelNodeImpl child : this.children) {
            if (!child.isCompilable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void generateCode(MethodVisitor mv, CodeFlow cf) {
        for (SpelNodeImpl child : this.children) {
            child.generateCode(mv, cf);
        }
        cf.pushDescriptor(this.exitTypeDescriptor);
    }

}

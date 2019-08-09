package org.springframework.expression.spel.ast;

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.lang.Nullable;

public interface ValueRef {

    TypedValue getValue();

    void setValue(@Nullable Object newValue);

    boolean isWritable();

    class NullValueRef implements ValueRef {

        static final NullValueRef INSTANCE = new NullValueRef();

        @Override
        public TypedValue getValue() {
            return TypedValue.NULL;
        }

        @Override
        public void setValue(@Nullable Object newValue) {
            // The exception position '0' isn't right but the overhead of creating
            // instances of this per node (where the node is solely for error reporting)
            // would be unfortunate.
            throw new SpelEvaluationException(0, SpelMessage.NOT_ASSIGNABLE, "null");
        }

        @Override
        public boolean isWritable() {
            return false;
        }

    }

    class TypedValueHolderValueRef implements ValueRef {

        private final TypedValue typedValue;

        private final SpelNodeImpl node;  // used only for error reporting

        public TypedValueHolderValueRef(TypedValue typedValue, SpelNodeImpl node) {
            this.typedValue = typedValue;
            this.node = node;
        }

        @Override
        public TypedValue getValue() {
            return this.typedValue;
        }

        @Override
        public void setValue(@Nullable Object newValue) {
            throw new SpelEvaluationException(this.node.getStartPosition(), SpelMessage.NOT_ASSIGNABLE, this.node.toStringAST());
        }

        @Override
        public boolean isWritable() {
            return false;
        }

    }

}

package org.springframework.expression.spel.ast;

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.*;
import org.springframework.lang.Nullable;

public abstract class Literal extends SpelNodeImpl {

    @Nullable
    private final String originalValue;

    public Literal(@Nullable String originalValue, int startPos, int endPos) {
        super(startPos, endPos);
        this.originalValue = originalValue;
    }

    @Nullable
    public final String getOriginalValue() {
        return this.originalValue;
    }

    @Override
    public final TypedValue getValueInternal(ExpressionState state) throws SpelEvaluationException {
        return getLiteralValue();
    }

    @Override
    public String toString() {
        return String.valueOf(getLiteralValue().getValue());
    }

    @Override
    public String toStringAST() {
        return toString();
    }

    public abstract TypedValue getLiteralValue();

    public static Literal getIntLiteral(String numberToken, int startPos, int endPos, int radix) {
        try {
            int value = Integer.parseInt(numberToken, radix);
            return new IntLiteral(numberToken, startPos, endPos, value);
        } catch (NumberFormatException ex) {
            throw new InternalParseException(new SpelParseException(startPos, ex, SpelMessage.NOT_AN_INTEGER, numberToken));
        }
    }

    public static Literal getLongLiteral(String numberToken, int startPos, int endPos, int radix) {
        try {
            long value = Long.parseLong(numberToken, radix);
            return new LongLiteral(numberToken, startPos, endPos, value);
        } catch (NumberFormatException ex) {
            throw new InternalParseException(new SpelParseException(startPos, ex, SpelMessage.NOT_A_LONG, numberToken));
        }
    }

    public static Literal getRealLiteral(String numberToken, int startPos, int endPos, boolean isFloat) {
        try {
            if (isFloat) {
                float value = Float.parseFloat(numberToken);
                return new FloatLiteral(numberToken, startPos, endPos, value);
            } else {
                double value = Double.parseDouble(numberToken);
                return new RealLiteral(numberToken, startPos, endPos, value);
            }
        } catch (NumberFormatException ex) {
            throw new InternalParseException(new SpelParseException(startPos, ex, SpelMessage.NOT_A_REAL, numberToken));
        }
    }

}

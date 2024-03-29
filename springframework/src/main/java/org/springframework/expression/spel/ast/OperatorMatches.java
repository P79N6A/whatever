package org.springframework.expression.spel.ast;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class OperatorMatches extends Operator {

    private static final int PATTERN_ACCESS_THRESHOLD = 1000000;

    private final ConcurrentMap<String, Pattern> patternCache = new ConcurrentHashMap<>();

    public OperatorMatches(int startPos, int endPos, SpelNodeImpl... operands) {
        super("matches", startPos, endPos, operands);
    }

    @Override
    public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
        SpelNodeImpl leftOp = getLeftOperand();
        SpelNodeImpl rightOp = getRightOperand();
        String left = leftOp.getValue(state, String.class);
        Object right = getRightOperand().getValue(state);
        if (left == null) {
            throw new SpelEvaluationException(leftOp.getStartPosition(), SpelMessage.INVALID_FIRST_OPERAND_FOR_MATCHES_OPERATOR, (Object) null);
        }
        if (!(right instanceof String)) {
            throw new SpelEvaluationException(rightOp.getStartPosition(), SpelMessage.INVALID_SECOND_OPERAND_FOR_MATCHES_OPERATOR, right);
        }
        try {
            String rightString = (String) right;
            Pattern pattern = this.patternCache.get(rightString);
            if (pattern == null) {
                pattern = Pattern.compile(rightString);
                this.patternCache.putIfAbsent(rightString, pattern);
            }
            Matcher matcher = pattern.matcher(new MatcherInput(left, new AccessCount()));
            return BooleanTypedValue.forValue(matcher.matches());
        } catch (PatternSyntaxException ex) {
            throw new SpelEvaluationException(rightOp.getStartPosition(), ex, SpelMessage.INVALID_PATTERN, right);
        } catch (IllegalStateException ex) {
            throw new SpelEvaluationException(rightOp.getStartPosition(), ex, SpelMessage.FLAWED_PATTERN, right);
        }
    }

    private static class AccessCount {

        private int count;

        public void check() throws IllegalStateException {
            if (this.count++ > PATTERN_ACCESS_THRESHOLD) {
                throw new IllegalStateException("Pattern access threshold exceeded");
            }
        }

    }

    private static class MatcherInput implements CharSequence {

        private final CharSequence value;

        private AccessCount access;

        public MatcherInput(CharSequence value, AccessCount access) {
            this.value = value;
            this.access = access;
        }

        public char charAt(int index) {
            this.access.check();
            return this.value.charAt(index);
        }

        public CharSequence subSequence(int start, int end) {
            return new MatcherInput(this.value.subSequence(start, end), this.access);
        }

        public int length() {
            return this.value.length();
        }

        @Override
        public String toString() {
            return this.value.toString();
        }

    }

}

package org.springframework.expression.spel;

@SuppressWarnings("serial")
public class InternalParseException extends RuntimeException {

    public InternalParseException(SpelParseException cause) {
        super(cause);
    }

    @Override
    public SpelParseException getCause() {
        return (SpelParseException) super.getCause();
    }

}

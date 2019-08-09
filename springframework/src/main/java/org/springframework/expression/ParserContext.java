package org.springframework.expression;

public interface ParserContext {

    boolean isTemplate();

    String getExpressionPrefix();

    String getExpressionSuffix();

    ParserContext TEMPLATE_EXPRESSION = new ParserContext() {

        @Override
        public boolean isTemplate() {
            return true;
        }

        @Override
        public String getExpressionPrefix() {
            return "#{";
        }

        @Override
        public String getExpressionSuffix() {
            return "}";
        }
    };

}

package org.apache.ibatis.scripting.xmltags;

public class IfSqlNode implements SqlNode {
    /**
     * 表达式执行器
     */
    private final ExpressionEvaluator evaluator;
    /**
     * 条件表达式
     */
    private final String test;
    private final SqlNode contents;

    public IfSqlNode(SqlNode contents, String test) {
        this.test = test;
        this.contents = contents;
        this.evaluator = new ExpressionEvaluator();
    }

    @Override
    public boolean apply(DynamicContext context) {
        if (evaluator.evaluateBoolean(test, context.getBindings())) {
            contents.apply(context);
            return true;
        }
        return false;
    }

}

package org.apache.ibatis.scripting.xmltags;

public class VarDeclSqlNode implements SqlNode {

    private final String name;
    private final String expression;

    public VarDeclSqlNode(String var, String exp) {
        name = var;
        expression = exp;
    }

    @Override
    public boolean apply(DynamicContext context) {
        final Object value = OgnlCache.getValue(expression, context.getBindings());
        // 将OGNL表达式加到当前映射语句的上下文中，这样就可以直接获取到了
        context.bind(name, value);
        return true;
    }

}

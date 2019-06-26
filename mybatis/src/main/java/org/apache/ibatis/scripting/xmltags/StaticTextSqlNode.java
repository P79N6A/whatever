package org.apache.ibatis.scripting.xmltags;

public class StaticTextSqlNode implements SqlNode {
    private final String text;

    public StaticTextSqlNode(String text) {
        this.text = text;
    }

    @Override
    public boolean apply(DynamicContext context) {
        // 静态文本节点不做任何处理
        context.appendSql(text);
        return true;
    }

}
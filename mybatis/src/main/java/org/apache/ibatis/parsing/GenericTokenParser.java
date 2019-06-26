package org.apache.ibatis.parsing;

import org.apache.ibatis.scripting.xmltags.TextSqlNode;

/**
 * 普通记号解析器，处理#{}和${}参数
 */
public class GenericTokenParser {

    /**
     * 开始记号
     */
    private final String openToken;

    /**
     * 结束记号
     */
    private final String closeToken;
    /**
     * 记号处理器
     */
    private final TokenHandler handler;

    public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
        this.openToken = openToken;
        this.closeToken = closeToken;
        this.handler = handler;
    }

    public String parse(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        int start = text.indexOf(openToken);
        if (start == -1) {
            return text;
        }
        char[] src = text.toCharArray();
        int offset = 0;
        final StringBuilder builder = new StringBuilder();
        StringBuilder expression = null;
        while (start > -1) {
            if (start > 0 && src[start - 1] == '\\') {

                builder.append(src, offset, start - offset - 1).append(openToken);
                offset = start + openToken.length();
            } else {

                if (expression == null) {
                    expression = new StringBuilder();
                } else {
                    expression.setLength(0);
                }
                builder.append(src, offset, start - offset);
                offset = start + openToken.length();
                int end = text.indexOf(closeToken, offset);
                // 循环解析参数，可以解析${first_name} ${initial} ${last_name}这样的字符串
                while (end > -1) {
                    if (end > offset && src[end - 1] == '\\') {

                        expression.append(src, offset, end - offset - 1).append(closeToken);
                        offset = end + closeToken.length();
                        end = text.indexOf(closeToken, offset);
                    } else {
                        expression.append(src, offset, end - offset);
                        offset = end + closeToken.length();
                        break;
                    }
                }
                if (end == -1) {

                    builder.append(src, start, src.length - start);
                    offset = src.length;
                } else {
                    // expression为open与close间的字符串
                    // 得到一对大括号里的字符串后，调用handler.handleToken，比如替换变量
                    builder.append(handler.handleToken(expression.toString()));
                    offset = end + closeToken.length();
                }
            }
            start = text.indexOf(openToken, offset);
        }
        if (offset < src.length) {
            builder.append(src, offset, src.length - offset);
        }
        return builder.toString();
    }


}

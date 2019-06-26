package org.apache.ibatis.scripting.xmltags;

import java.util.List;

/**
 * 解析动态标签(包含了${}动态变量或等元素的sql节点）然后根据是否动态sql决定实例化的SqlSource为DynamicSqlSource或RawSqlSource
 * 对于动态标签的解析需要递归直到解析至文本节点
 * 最后返回一个MixedSqlNode，其中有个List类型的属性，包含树状层次嵌套的多种SqlNode实现类型的列表
 */
public class MixedSqlNode implements SqlNode {

    private final List<SqlNode> contents;

    public MixedSqlNode(List<SqlNode> contents) {
        this.contents = contents;
    }

    @Override
    public boolean apply(DynamicContext context) {

        // 遍历每个根SqlNode
        contents.forEach(node -> node.apply(context));
        return true;
    }
}

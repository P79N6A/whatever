package org.apache.ibatis.scripting.xmltags;

/**
 * 处理CRUD节点下的各类动态标签
 */
public interface SqlNode {
    boolean apply(DynamicContext context);
}

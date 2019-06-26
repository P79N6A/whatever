package org.apache.ibatis.builder.xml;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class XMLIncludeTransformer {

    private final Configuration configuration;
    private final MapperBuilderAssistant builderAssistant;

    public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
        this.configuration = configuration;
        this.builderAssistant = builderAssistant;
    }

    public void applyIncludes(Node source) {
        Properties variablesContext = new Properties();
        Properties configurationVariables = configuration.getVariables();
        Optional.ofNullable(configurationVariables).ifPresent(variablesContext::putAll);
        applyIncludes(source, variablesContext, false);
    }

    /**
     * 将节点分为文本节点、include、非include三类处理
     */
    private void applyIncludes(Node source, final Properties variablesContext, boolean included) {
        // include节点
        if (source.getNodeName().equals("include")) {
            // 找到sql片段，对节点中包含的占位符进行替换解析
            Node toInclude = findSqlFragment(getStringAttribute(source, "refid"), variablesContext);
            Properties toIncludeContext = getVariablesContext(source, variablesContext);
            // 递归解析
            applyIncludes(toInclude, toIncludeContext, true);
            // 判断include的sql片段是否和包含它的节点是同一个文档，如果不是，则把它从原来的文档包含进来
            if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
                toInclude = source.getOwnerDocument().importNode(toInclude, true);
            }
            // 使用include指向的sql节点替换include节点
            source.getParentNode().replaceChild(toInclude, source);
            while (toInclude.hasChildNodes()) {
                toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
            }
            // 最后剥掉sql节点本身，也就是把sql下的节点上移一层，这样就合法了
            toInclude.getParentNode().removeChild(toInclude);
            /*
             *
             * <sql id=”userColumns”> id,username,password </sql>
             *
             * <select id=”selectUsers” parameterType=”int” resultType=”hashmap”>
             * select <include refid=”userColumns”/> from some_table where id = #{id}
             * </select>
             *
             * 转换：
             *
             * <select id=”selectUsers” parameterType=”int” resultType=”hashmap”>
             * select id,username,password from some_table where id = #{id}
             * </select>
             *
             */
        }
        // 第一次执行，传递进来的是CRUD节点本身
        else if (source.getNodeType() == Node.ELEMENT_NODE) {
            // 首先判断是否为根节点，如果是非根且变量上下文不为空，则先解析属性值上的占位符
            if (included && !variablesContext.isEmpty()) {

                NamedNodeMap attributes = source.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attr = attributes.item(i);
                    attr.setNodeValue(PropertyParser.parse(attr.getNodeValue(), variablesContext));
                }
            }
            NodeList children = source.getChildNodes();
            // 遍历所有的子节点
            for (int i = 0; i < children.getLength(); i++) {
                // 递归
                applyIncludes(children.item(i), variablesContext, included);
            }
        }
        // 文本节点
        else if (included && source.getNodeType() == Node.TEXT_NODE && !variablesContext.isEmpty()) {
            // 根据入参变量上下文将变量设置替换进去
            source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
        }
    }

    private Node findSqlFragment(String refid, Properties variables) {
        refid = PropertyParser.parse(refid, variables);
        refid = builderAssistant.applyCurrentNamespace(refid, true);
        try {
            XNode nodeToInclude = configuration.getSqlFragments().get(refid);
            return nodeToInclude.getNode().cloneNode(true);
        } catch (IllegalArgumentException e) {
            throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'", e);
        }
    }

    private String getStringAttribute(Node node, String name) {
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }

    private Properties getVariablesContext(Node node, Properties inheritedVariablesContext) {
        Map<String, String> declaredProperties = null;
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                String name = getStringAttribute(n, "name");

                String value = PropertyParser.parse(getStringAttribute(n, "value"), inheritedVariablesContext);
                if (declaredProperties == null) {
                    declaredProperties = new HashMap<>();
                }
                if (declaredProperties.put(name, value) != null) {
                    throw new BuilderException("Variable " + name + " defined twice in the same include definition");
                }
            }
        }
        if (declaredProperties == null) {
            return inheritedVariablesContext;
        } else {
            Properties newProperties = new Properties();
            newProperties.putAll(inheritedVariablesContext);
            newProperties.putAll(declaredProperties);
            return newProperties;
        }
    }
}

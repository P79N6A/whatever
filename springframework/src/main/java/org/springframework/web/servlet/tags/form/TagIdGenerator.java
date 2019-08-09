package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.PageContext;

abstract class TagIdGenerator {

    private static final String PAGE_CONTEXT_ATTRIBUTE_PREFIX = TagIdGenerator.class.getName() + ".";

    public static String nextId(String name, PageContext pageContext) {
        String attributeName = PAGE_CONTEXT_ATTRIBUTE_PREFIX + name;
        Integer currentCount = (Integer) pageContext.getAttribute(attributeName);
        currentCount = (currentCount != null ? currentCount + 1 : 1);
        pageContext.setAttribute(attributeName, currentCount);
        return (name + currentCount);
    }

}

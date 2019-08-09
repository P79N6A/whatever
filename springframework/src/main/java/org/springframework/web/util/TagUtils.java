package org.springframework.web.util;

import org.springframework.util.Assert;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

public abstract class TagUtils {

    public static final String SCOPE_PAGE = "page";

    public static final String SCOPE_REQUEST = "request";

    public static final String SCOPE_SESSION = "session";

    public static final String SCOPE_APPLICATION = "application";

    public static int getScope(String scope) {
        Assert.notNull(scope, "Scope to search for cannot be null");
        if (scope.equals(SCOPE_REQUEST)) {
            return PageContext.REQUEST_SCOPE;
        } else if (scope.equals(SCOPE_SESSION)) {
            return PageContext.SESSION_SCOPE;
        } else if (scope.equals(SCOPE_APPLICATION)) {
            return PageContext.APPLICATION_SCOPE;
        } else {
            return PageContext.PAGE_SCOPE;
        }
    }

    public static boolean hasAncestorOfType(Tag tag, Class<?> ancestorTagClass) {
        Assert.notNull(tag, "Tag cannot be null");
        Assert.notNull(ancestorTagClass, "Ancestor tag class cannot be null");
        if (!Tag.class.isAssignableFrom(ancestorTagClass)) {
            throw new IllegalArgumentException("Class '" + ancestorTagClass.getName() + "' is not a valid Tag type");
        }
        Tag ancestor = tag.getParent();
        while (ancestor != null) {
            if (ancestorTagClass.isAssignableFrom(ancestor.getClass())) {
                return true;
            }
            ancestor = ancestor.getParent();
        }
        return false;
    }

    public static void assertHasAncestorOfType(Tag tag, Class<?> ancestorTagClass, String tagName, String ancestorTagName) {
        Assert.hasText(tagName, "'tagName' must not be empty");
        Assert.hasText(ancestorTagName, "'ancestorTagName' must not be empty");
        if (!TagUtils.hasAncestorOfType(tag, ancestorTagClass)) {
            throw new IllegalStateException("The '" + tagName + "' tag can only be used inside a valid '" + ancestorTagName + "' tag.");
        }
    }

}

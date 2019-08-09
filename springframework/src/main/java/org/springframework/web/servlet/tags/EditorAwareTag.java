package org.springframework.web.servlet.tags;

import org.springframework.lang.Nullable;

import javax.servlet.jsp.JspException;
import java.beans.PropertyEditor;

public interface EditorAwareTag {

    @Nullable
    PropertyEditor getEditor() throws JspException;

}

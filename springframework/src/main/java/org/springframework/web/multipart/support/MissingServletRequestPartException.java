package org.springframework.web.multipart.support;

import javax.servlet.ServletException;

@SuppressWarnings("serial")
public class MissingServletRequestPartException extends ServletException {

    private final String partName;

    public MissingServletRequestPartException(String partName) {
        super("Required request part '" + partName + "' is not present");
        this.partName = partName;
    }

    public String getRequestPartName() {
        return this.partName;
    }

}

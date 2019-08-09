package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;

public interface LastModified {

    long getLastModified(HttpServletRequest request);

}

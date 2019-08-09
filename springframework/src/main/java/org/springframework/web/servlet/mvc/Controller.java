package org.springframework.web.servlet.mvc;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@FunctionalInterface
public interface Controller {

    @Nullable
    ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception;

}

package org.springframework.boot.autoconfigure.web.servlet.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@FunctionalInterface
public interface ErrorViewResolver {

    ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model);

}

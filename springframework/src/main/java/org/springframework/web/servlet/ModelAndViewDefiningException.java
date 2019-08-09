package org.springframework.web.servlet;

import org.springframework.util.Assert;

import javax.servlet.ServletException;

@SuppressWarnings("serial")
public class ModelAndViewDefiningException extends ServletException {

    private final ModelAndView modelAndView;

    public ModelAndViewDefiningException(ModelAndView modelAndView) {
        Assert.notNull(modelAndView, "ModelAndView must not be null in ModelAndViewDefiningException");
        this.modelAndView = modelAndView;
    }

    public ModelAndView getModelAndView() {
        return this.modelAndView;
    }

}

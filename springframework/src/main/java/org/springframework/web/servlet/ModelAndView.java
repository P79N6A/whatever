package org.springframework.web.servlet;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;

import java.util.Map;

public class ModelAndView {

    @Nullable
    private Object view;

    @Nullable
    private ModelMap model;

    @Nullable
    private HttpStatus status;

    private boolean cleared = false;

    public ModelAndView() {
    }

    public ModelAndView(String viewName) {
        this.view = viewName;
    }

    public ModelAndView(View view) {
        this.view = view;
    }

    public ModelAndView(String viewName, @Nullable Map<String, ?> model) {
        this.view = viewName;
        if (model != null) {
            getModelMap().addAllAttributes(model);
        }
    }

    public ModelAndView(View view, @Nullable Map<String, ?> model) {
        this.view = view;
        if (model != null) {
            getModelMap().addAllAttributes(model);
        }
    }

    public ModelAndView(String viewName, HttpStatus status) {
        this.view = viewName;
        this.status = status;
    }

    public ModelAndView(@Nullable String viewName, @Nullable Map<String, ?> model, @Nullable HttpStatus status) {
        this.view = viewName;
        if (model != null) {
            getModelMap().addAllAttributes(model);
        }
        this.status = status;
    }

    public ModelAndView(String viewName, String modelName, Object modelObject) {
        this.view = viewName;
        addObject(modelName, modelObject);
    }

    public ModelAndView(View view, String modelName, Object modelObject) {
        this.view = view;
        addObject(modelName, modelObject);
    }

    public void setViewName(@Nullable String viewName) {
        this.view = viewName;
    }

    @Nullable
    public String getViewName() {
        return (this.view instanceof String ? (String) this.view : null);
    }

    public void setView(@Nullable View view) {
        this.view = view;
    }

    @Nullable
    public View getView() {
        return (this.view instanceof View ? (View) this.view : null);
    }

    public boolean hasView() {
        return (this.view != null);
    }

    public boolean isReference() {
        return (this.view instanceof String);
    }

    @Nullable
    protected Map<String, Object> getModelInternal() {
        return this.model;
    }

    public ModelMap getModelMap() {
        if (this.model == null) {
            this.model = new ModelMap();
        }
        return this.model;
    }

    public Map<String, Object> getModel() {
        return getModelMap();
    }

    public void setStatus(@Nullable HttpStatus status) {
        this.status = status;
    }

    @Nullable
    public HttpStatus getStatus() {
        return this.status;
    }

    public ModelAndView addObject(String attributeName, @Nullable Object attributeValue) {
        getModelMap().addAttribute(attributeName, attributeValue);
        return this;
    }

    public ModelAndView addObject(Object attributeValue) {
        getModelMap().addAttribute(attributeValue);
        return this;
    }

    public ModelAndView addAllObjects(@Nullable Map<String, ?> modelMap) {
        getModelMap().addAllAttributes(modelMap);
        return this;
    }

    public void clear() {
        this.view = null;
        this.model = null;
        this.cleared = true;
    }

    public boolean isEmpty() {
        return (this.view == null && CollectionUtils.isEmpty(this.model));
    }

    public boolean wasCleared() {
        return (this.cleared && isEmpty());
    }

    @Override
    public String toString() {
        return "ModelAndView [view=" + formatView() + "; model=" + this.model + "]";
    }

    private String formatView() {
        return isReference() ? "\"" + this.view + "\"" : "[" + this.view + "]";
    }

}

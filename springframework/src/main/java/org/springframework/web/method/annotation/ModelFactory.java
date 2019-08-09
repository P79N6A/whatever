package org.springframework.web.method.annotation;

import org.springframework.beans.BeanUtils;
import org.springframework.core.Conventions;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.util.*;

public final class ModelFactory {

    private final List<ModelMethod> modelMethods = new ArrayList<>();

    private final WebDataBinderFactory dataBinderFactory;

    private final SessionAttributesHandler sessionAttributesHandler;

    public ModelFactory(@Nullable List<InvocableHandlerMethod> handlerMethods, WebDataBinderFactory binderFactory, SessionAttributesHandler attributeHandler) {
        if (handlerMethods != null) {
            for (InvocableHandlerMethod handlerMethod : handlerMethods) {
                this.modelMethods.add(new ModelMethod(handlerMethod));
            }
        }
        this.dataBinderFactory = binderFactory;
        this.sessionAttributesHandler = attributeHandler;
    }

    public void initModel(NativeWebRequest request, ModelAndViewContainer container, HandlerMethod handlerMethod) throws Exception {
        Map<String, ?> sessionAttributes = this.sessionAttributesHandler.retrieveAttributes(request);
        container.mergeAttributes(sessionAttributes);
        invokeModelAttributeMethods(request, container);
        for (String name : findSessionAttributeArguments(handlerMethod)) {
            if (!container.containsAttribute(name)) {
                Object value = this.sessionAttributesHandler.retrieveAttribute(request, name);
                if (value == null) {
                    throw new HttpSessionRequiredException("Expected session attribute '" + name + "'", name);
                }
                container.addAttribute(name, value);
            }
        }
    }

    private void invokeModelAttributeMethods(NativeWebRequest request, ModelAndViewContainer container) throws Exception {
        while (!this.modelMethods.isEmpty()) {
            InvocableHandlerMethod modelMethod = getNextModelMethod(container).getHandlerMethod();
            ModelAttribute ann = modelMethod.getMethodAnnotation(ModelAttribute.class);
            Assert.state(ann != null, "No ModelAttribute annotation");
            if (container.containsAttribute(ann.name())) {
                if (!ann.binding()) {
                    container.setBindingDisabled(ann.name());
                }
                continue;
            }
            Object returnValue = modelMethod.invokeForRequest(request, container);
            if (!modelMethod.isVoid()) {
                String returnValueName = getNameForReturnValue(returnValue, modelMethod.getReturnType());
                if (!ann.binding()) {
                    container.setBindingDisabled(returnValueName);
                }
                if (!container.containsAttribute(returnValueName)) {
                    container.addAttribute(returnValueName, returnValue);
                }
            }
        }
    }

    private ModelMethod getNextModelMethod(ModelAndViewContainer container) {
        for (ModelMethod modelMethod : this.modelMethods) {
            if (modelMethod.checkDependencies(container)) {
                this.modelMethods.remove(modelMethod);
                return modelMethod;
            }
        }
        ModelMethod modelMethod = this.modelMethods.get(0);
        this.modelMethods.remove(modelMethod);
        return modelMethod;
    }

    private List<String> findSessionAttributeArguments(HandlerMethod handlerMethod) {
        List<String> result = new ArrayList<>();
        for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
            if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
                String name = getNameForParameter(parameter);
                Class<?> paramType = parameter.getParameterType();
                if (this.sessionAttributesHandler.isHandlerSessionAttribute(name, paramType)) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    public void updateModel(NativeWebRequest request, ModelAndViewContainer container) throws Exception {
        ModelMap defaultModel = container.getDefaultModel();
        if (container.getSessionStatus().isComplete()) {
            this.sessionAttributesHandler.cleanupAttributes(request);
        } else {
            this.sessionAttributesHandler.storeAttributes(request, defaultModel);
        }
        if (!container.isRequestHandled() && container.getModel() == defaultModel) {
            updateBindingResult(request, defaultModel);
        }
    }

    private void updateBindingResult(NativeWebRequest request, ModelMap model) throws Exception {
        List<String> keyNames = new ArrayList<>(model.keySet());
        for (String name : keyNames) {
            Object value = model.get(name);
            if (value != null && isBindingCandidate(name, value)) {
                String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + name;
                if (!model.containsAttribute(bindingResultKey)) {
                    WebDataBinder dataBinder = this.dataBinderFactory.createBinder(request, value, name);
                    model.put(bindingResultKey, dataBinder.getBindingResult());
                }
            }
        }
    }

    private boolean isBindingCandidate(String attributeName, Object value) {
        if (attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
            return false;
        }
        if (this.sessionAttributesHandler.isHandlerSessionAttribute(attributeName, value.getClass())) {
            return true;
        }
        return (!value.getClass().isArray() && !(value instanceof Collection) && !(value instanceof Map) && !BeanUtils.isSimpleValueType(value.getClass()));
    }

    public static String getNameForParameter(MethodParameter parameter) {
        ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
        String name = (ann != null ? ann.value() : null);
        return (StringUtils.hasText(name) ? name : Conventions.getVariableNameForParameter(parameter));
    }

    public static String getNameForReturnValue(@Nullable Object returnValue, MethodParameter returnType) {
        ModelAttribute ann = returnType.getMethodAnnotation(ModelAttribute.class);
        if (ann != null && StringUtils.hasText(ann.value())) {
            return ann.value();
        } else {
            Method method = returnType.getMethod();
            Assert.state(method != null, "No handler method");
            Class<?> containingClass = returnType.getContainingClass();
            Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, containingClass);
            return Conventions.getVariableNameForReturnType(method, resolvedType, returnValue);
        }
    }

    private static class ModelMethod {

        private final InvocableHandlerMethod handlerMethod;

        private final Set<String> dependencies = new HashSet<>();

        public ModelMethod(InvocableHandlerMethod handlerMethod) {
            this.handlerMethod = handlerMethod;
            for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
                if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
                    this.dependencies.add(getNameForParameter(parameter));
                }
            }
        }

        public InvocableHandlerMethod getHandlerMethod() {
            return this.handlerMethod;
        }

        public boolean checkDependencies(ModelAndViewContainer mavContainer) {
            for (String name : this.dependencies) {
                if (!mavContainer.containsAttribute(name)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return this.handlerMethod.getMethod().toGenericString();
        }

    }

}

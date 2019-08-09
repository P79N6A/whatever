package org.springframework.web.servlet.tags;

import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.EnvironmentAccessor;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.*;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.io.IOException;

@SuppressWarnings("serial")
public class EvalTag extends HtmlEscapingAwareTag {

    private static final String EVALUATION_CONTEXT_PAGE_ATTRIBUTE = "org.springframework.web.servlet.tags.EVALUATION_CONTEXT";

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Nullable
    private Expression expression;

    @Nullable
    private String var;

    private int scope = PageContext.PAGE_SCOPE;

    private boolean javaScriptEscape = false;

    public void setExpression(String expression) {
        this.expression = this.expressionParser.parseExpression(expression);
    }

    public void setVar(String var) {
        this.var = var;
    }

    public void setScope(String scope) {
        this.scope = TagUtils.getScope(scope);
    }

    public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
        this.javaScriptEscape = javaScriptEscape;
    }

    @Override
    public int doStartTagInternal() throws JspException {
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        EvaluationContext evaluationContext = (EvaluationContext) this.pageContext.getAttribute(EVALUATION_CONTEXT_PAGE_ATTRIBUTE);
        if (evaluationContext == null) {
            evaluationContext = createEvaluationContext(this.pageContext);
            this.pageContext.setAttribute(EVALUATION_CONTEXT_PAGE_ATTRIBUTE, evaluationContext);
        }
        if (this.var != null) {
            Object result = (this.expression != null ? this.expression.getValue(evaluationContext) : null);
            this.pageContext.setAttribute(this.var, result, this.scope);
        } else {
            try {
                String result = (this.expression != null ? this.expression.getValue(evaluationContext, String.class) : null);
                result = ObjectUtils.getDisplayString(result);
                result = htmlEscape(result);
                result = (this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(result) : result);
                this.pageContext.getOut().print(result);
            } catch (IOException ex) {
                throw new JspException(ex);
            }
        }
        return EVAL_PAGE;
    }

    private EvaluationContext createEvaluationContext(PageContext pageContext) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.addPropertyAccessor(new JspPropertyAccessor(pageContext));
        context.addPropertyAccessor(new MapAccessor());
        context.addPropertyAccessor(new EnvironmentAccessor());
        context.setBeanResolver(new BeanFactoryResolver(getRequestContext().getWebApplicationContext()));
        ConversionService conversionService = getConversionService(pageContext);
        if (conversionService != null) {
            context.setTypeConverter(new StandardTypeConverter(conversionService));
        }
        return context;
    }

    @Nullable
    private ConversionService getConversionService(PageContext pageContext) {
        return (ConversionService) pageContext.getRequest().getAttribute(ConversionService.class.getName());
    }

    @SuppressWarnings("deprecation")
    private static class JspPropertyAccessor implements PropertyAccessor {

        private final PageContext pageContext;

        @Nullable
        private final javax.servlet.jsp.el.VariableResolver variableResolver;

        public JspPropertyAccessor(PageContext pageContext) {
            this.pageContext = pageContext;
            this.variableResolver = pageContext.getVariableResolver();
        }

        @Override
        @Nullable
        public Class<?>[] getSpecificTargetClasses() {
            return null;
        }

        @Override
        public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
            return (target == null && (resolveImplicitVariable(name) != null || this.pageContext.findAttribute(name) != null));
        }

        @Override
        public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
            Object implicitVar = resolveImplicitVariable(name);
            if (implicitVar != null) {
                return new TypedValue(implicitVar);
            }
            return new TypedValue(this.pageContext.findAttribute(name));
        }

        @Override
        public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) {
            return false;
        }

        @Override
        public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) {
            throw new UnsupportedOperationException();
        }

        @Nullable
        private Object resolveImplicitVariable(String name) throws AccessException {
            if (this.variableResolver == null) {
                return null;
            }
            try {
                return this.variableResolver.resolveVariable(name);
            } catch (Exception ex) {
                throw new AccessException("Unexpected exception occurred accessing '" + name + "' as an implicit variable", ex);
            }
        }

    }

}

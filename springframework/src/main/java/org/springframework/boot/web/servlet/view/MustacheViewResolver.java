package org.springframework.boot.web.servlet.view;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Compiler;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

public class MustacheViewResolver extends AbstractTemplateViewResolver {

    private final Mustache.Compiler compiler;

    private String charset;

    public MustacheViewResolver() {
        this.compiler = Mustache.compiler();
        setViewClass(requiredViewClass());
    }

    public MustacheViewResolver(Compiler compiler) {
        this.compiler = compiler;
        setViewClass(requiredViewClass());
    }

    @Override
    protected Class<?> requiredViewClass() {
        return MustacheView.class;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    protected AbstractUrlBasedView buildView(String viewName) throws Exception {
        MustacheView view = (MustacheView) super.buildView(viewName);
        view.setCompiler(this.compiler);
        view.setCharset(this.charset);
        return view;
    }

}

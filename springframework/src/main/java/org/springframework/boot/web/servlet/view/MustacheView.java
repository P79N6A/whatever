package org.springframework.boot.web.servlet.view;

import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Template;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.view.AbstractTemplateView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import java.util.Map;

public class MustacheView extends AbstractTemplateView {

    private Compiler compiler;

    private String charset;

    public void setCompiler(Compiler compiler) {
        this.compiler = compiler;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    public boolean checkResource(Locale locale) throws Exception {
        Resource resource = getApplicationContext().getResource(this.getUrl());
        return (resource != null && resource.exists());
    }

    @Override
    protected void renderMergedTemplateModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Template template = createTemplate(getApplicationContext().getResource(this.getUrl()));
        if (template != null) {
            template.execute(model, response.getWriter());
        }
    }

    private Template createTemplate(Resource resource) throws IOException {
        try (Reader reader = getReader(resource)) {
            return this.compiler.compile(reader);
        }
    }

    private Reader getReader(Resource resource) throws IOException {
        if (this.charset != null) {
            return new InputStreamReader(resource.getInputStream(), this.charset);
        }
        return new InputStreamReader(resource.getInputStream());
    }

}

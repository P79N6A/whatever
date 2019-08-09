package org.springframework.web.servlet.theme;

import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ThemeChangeInterceptor extends HandlerInterceptorAdapter {

    public static final String DEFAULT_PARAM_NAME = "theme";

    private String paramName = DEFAULT_PARAM_NAME;

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getParamName() {
        return this.paramName;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws ServletException {
        String newTheme = request.getParameter(this.paramName);
        if (newTheme != null) {
            ThemeResolver themeResolver = RequestContextUtils.getThemeResolver(request);
            if (themeResolver == null) {
                throw new IllegalStateException("No ThemeResolver found: not in a DispatcherServlet request?");
            }
            themeResolver.setThemeName(request, response, newTheme);
        }
        // Proceed in any case.
        return true;
    }

}

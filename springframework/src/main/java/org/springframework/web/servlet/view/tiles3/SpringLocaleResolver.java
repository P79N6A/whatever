package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.locale.impl.DefaultLocaleResolver;
import org.apache.tiles.request.Request;
import org.apache.tiles.request.servlet.NotAServletEnvironmentException;
import org.apache.tiles.request.servlet.ServletUtil;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

public class SpringLocaleResolver extends DefaultLocaleResolver {

    @Override
    public Locale resolveLocale(Request request) {
        try {
            HttpServletRequest servletRequest = ServletUtil.getServletRequest(request).getRequest();
            if (servletRequest != null) {
                return RequestContextUtils.getLocale(servletRequest);
            }
        } catch (NotAServletEnvironmentException ex) {
            // ignore
        }
        return super.resolveLocale(request);
    }

}

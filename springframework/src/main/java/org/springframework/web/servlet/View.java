package org.springframework.web.servlet;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface View {

    String RESPONSE_STATUS_ATTRIBUTE = View.class.getName() + ".responseStatus";

    String PATH_VARIABLES = View.class.getName() + ".pathVariables";

    String SELECTED_CONTENT_TYPE = View.class.getName() + ".selectedContentType";

    @Nullable
    default String getContentType() {
        return null;
    }

    void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception;

}

package org.springframework.web.cors;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface CorsProcessor {

    boolean processRequest(@Nullable CorsConfiguration configuration, HttpServletRequest request, HttpServletResponse response) throws IOException;

}

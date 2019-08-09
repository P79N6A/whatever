package org.springframework.web.context.support;

import org.springframework.context.support.LiveBeansView;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuppressWarnings("serial")
public class LiveBeansViewServlet extends HttpServlet {

    @Nullable
    private LiveBeansView liveBeansView;

    @Override
    public void init() throws ServletException {
        this.liveBeansView = buildLiveBeansView();
    }

    protected LiveBeansView buildLiveBeansView() {
        return new ServletContextLiveBeansView(getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Assert.state(this.liveBeansView != null, "No LiveBeansView available");
        String content = this.liveBeansView.getSnapshotAsJson();
        response.setContentType("application/json");
        response.setContentLength(content.length());
        response.getWriter().write(content);
    }

}

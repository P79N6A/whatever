package org.springframework.web.servlet.view.feed;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.WireFeedOutput;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.util.Map;

public abstract class AbstractFeedView<T extends WireFeed> extends AbstractView {

    @Override
    protected final void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        T wireFeed = newFeed();
        buildFeedMetadata(model, wireFeed, request);
        buildFeedEntries(model, wireFeed, request, response);
        setResponseContentType(request, response);
        if (!StringUtils.hasText(wireFeed.getEncoding())) {
            wireFeed.setEncoding("UTF-8");
        }
        WireFeedOutput feedOutput = new WireFeedOutput();
        ServletOutputStream out = response.getOutputStream();
        feedOutput.output(wireFeed, new OutputStreamWriter(out, wireFeed.getEncoding()));
        out.flush();
    }

    protected abstract T newFeed();

    protected void buildFeedMetadata(Map<String, Object> model, T feed, HttpServletRequest request) {
    }

    protected abstract void buildFeedEntries(Map<String, Object> model, T feed, HttpServletRequest request, HttpServletResponse response) throws Exception;

}

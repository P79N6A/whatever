package org.springframework.web.servlet.view.feed;

import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Item;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public abstract class AbstractRssFeedView extends AbstractFeedView<Channel> {

    public AbstractRssFeedView() {
        setContentType(MediaType.APPLICATION_RSS_XML_VALUE);
    }

    @Override
    protected Channel newFeed() {
        return new Channel("rss_2.0");
    }

    @Override
    protected final void buildFeedEntries(Map<String, Object> model, Channel channel, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<Item> items = buildFeedItems(model, request, response);
        channel.setItems(items);
    }

    protected abstract List<Item> buildFeedItems(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;

}

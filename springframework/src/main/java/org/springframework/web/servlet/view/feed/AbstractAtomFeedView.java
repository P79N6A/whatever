package org.springframework.web.servlet.view.feed;

import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Feed;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public abstract class AbstractAtomFeedView extends AbstractFeedView<Feed> {

    public static final String DEFAULT_FEED_TYPE = "atom_1.0";

    private String feedType = DEFAULT_FEED_TYPE;

    public AbstractAtomFeedView() {
        setContentType("application/atom+xml");
    }

    public void setFeedType(String feedType) {
        this.feedType = feedType;
    }

    @Override
    protected Feed newFeed() {
        return new Feed(this.feedType);
    }

    @Override
    protected final void buildFeedEntries(Map<String, Object> model, Feed feed, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<Entry> entries = buildFeedEntries(model, request, response);
        feed.setEntries(entries);
    }

    protected abstract List<Entry> buildFeedEntries(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;

}

package org.springframework.web.filter;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CompositeFilter implements Filter {

    private List<? extends Filter> filters = new ArrayList<>();

    public void setFilters(List<? extends Filter> filters) {
        this.filters = new ArrayList<>(filters);
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        for (Filter filter : this.filters) {
            filter.init(config);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new VirtualFilterChain(chain, this.filters).doFilter(request, response);
    }

    @Override
    public void destroy() {
        for (int i = this.filters.size(); i-- > 0; ) {
            Filter filter = this.filters.get(i);
            filter.destroy();
        }
    }

    private static class VirtualFilterChain implements FilterChain {

        private final FilterChain originalChain;

        private final List<? extends Filter> additionalFilters;

        private int currentPosition = 0;

        public VirtualFilterChain(FilterChain chain, List<? extends Filter> additionalFilters) {
            this.originalChain = chain;
            this.additionalFilters = additionalFilters;
        }

        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response) throws IOException, ServletException {
            if (this.currentPosition == this.additionalFilters.size()) {
                this.originalChain.doFilter(request, response);
            } else {
                this.currentPosition++;
                Filter nextFilter = this.additionalFilters.get(this.currentPosition - 1);
                nextFilter.doFilter(request, response, this);
            }
        }

    }

}

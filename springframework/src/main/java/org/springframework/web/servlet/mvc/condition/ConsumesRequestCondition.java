package org.springframework.web.servlet.mvc.condition;

import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition.HeaderExpression;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public final class ConsumesRequestCondition extends AbstractRequestCondition<ConsumesRequestCondition> {

    private static final ConsumesRequestCondition EMPTY_CONDITION = new ConsumesRequestCondition();

    private final List<ConsumeMediaTypeExpression> expressions;

    private boolean bodyRequired = true;

    public ConsumesRequestCondition(String... consumes) {
        this(consumes, null);
    }

    public ConsumesRequestCondition(String[] consumes, @Nullable String[] headers) {
        this.expressions = new ArrayList<>(parseExpressions(consumes, headers));
        Collections.sort(this.expressions);
    }

    private ConsumesRequestCondition(List<ConsumeMediaTypeExpression> expressions) {
        this.expressions = expressions;
    }

    private static Set<ConsumeMediaTypeExpression> parseExpressions(String[] consumes, @Nullable String[] headers) {
        Set<ConsumeMediaTypeExpression> result = new LinkedHashSet<>();
        if (headers != null) {
            for (String header : headers) {
                HeaderExpression expr = new HeaderExpression(header);
                if ("Content-Type".equalsIgnoreCase(expr.name) && expr.value != null) {
                    for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
                        result.add(new ConsumeMediaTypeExpression(mediaType, expr.isNegated));
                    }
                }
            }
        }
        for (String consume : consumes) {
            result.add(new ConsumeMediaTypeExpression(consume));
        }
        return result;
    }

    public Set<MediaTypeExpression> getExpressions() {
        return new LinkedHashSet<>(this.expressions);
    }

    public Set<MediaType> getConsumableMediaTypes() {
        Set<MediaType> result = new LinkedHashSet<>();
        for (ConsumeMediaTypeExpression expression : this.expressions) {
            if (!expression.isNegated()) {
                result.add(expression.getMediaType());
            }
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return this.expressions.isEmpty();
    }

    @Override
    protected Collection<ConsumeMediaTypeExpression> getContent() {
        return this.expressions;
    }

    @Override
    protected String getToStringInfix() {
        return " || ";
    }

    public void setBodyRequired(boolean bodyRequired) {
        this.bodyRequired = bodyRequired;
    }

    public boolean isBodyRequired() {
        return this.bodyRequired;
    }

    @Override
    public ConsumesRequestCondition combine(ConsumesRequestCondition other) {
        return (!other.expressions.isEmpty() ? other : this);
    }

    @Override
    @Nullable
    public ConsumesRequestCondition getMatchingCondition(HttpServletRequest request) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return EMPTY_CONDITION;
        }
        if (isEmpty()) {
            return this;
        }
        if (!hasBody(request) && !this.bodyRequired) {
            return EMPTY_CONDITION;
        }
        // Common media types are cached at the level of MimeTypeUtils
        MediaType contentType;
        try {
            contentType = StringUtils.hasLength(request.getContentType()) ? MediaType.parseMediaType(request.getContentType()) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (InvalidMediaTypeException ex) {
            return null;
        }
        List<ConsumeMediaTypeExpression> result = getMatchingExpressions(contentType);
        return !CollectionUtils.isEmpty(result) ? new ConsumesRequestCondition(result) : null;
    }

    private boolean hasBody(HttpServletRequest request) {
        String contentLength = request.getHeader(HttpHeaders.CONTENT_LENGTH);
        String transferEncoding = request.getHeader(HttpHeaders.TRANSFER_ENCODING);
        return StringUtils.hasText(transferEncoding) || (StringUtils.hasText(contentLength) && !contentLength.trim().equals("0"));
    }

    @Nullable
    private List<ConsumeMediaTypeExpression> getMatchingExpressions(MediaType contentType) {
        List<ConsumeMediaTypeExpression> result = null;
        for (ConsumeMediaTypeExpression expression : this.expressions) {
            if (expression.match(contentType)) {
                result = result != null ? result : new ArrayList<>();
                result.add(expression);
            }
        }
        return result;
    }

    @Override
    public int compareTo(ConsumesRequestCondition other, HttpServletRequest request) {
        if (this.expressions.isEmpty() && other.expressions.isEmpty()) {
            return 0;
        } else if (this.expressions.isEmpty()) {
            return 1;
        } else if (other.expressions.isEmpty()) {
            return -1;
        } else {
            return this.expressions.get(0).compareTo(other.expressions.get(0));
        }
    }

    static class ConsumeMediaTypeExpression extends AbstractMediaTypeExpression {

        ConsumeMediaTypeExpression(String expression) {
            super(expression);
        }

        ConsumeMediaTypeExpression(MediaType mediaType, boolean negated) {
            super(mediaType, negated);
        }

        public final boolean match(MediaType contentType) {
            boolean match = getMediaType().includes(contentType);
            return !isNegated() == match;
        }

    }

}

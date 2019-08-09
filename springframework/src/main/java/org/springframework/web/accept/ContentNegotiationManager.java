package org.springframework.web.accept;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.*;

public class ContentNegotiationManager implements ContentNegotiationStrategy, MediaTypeFileExtensionResolver {

    private final List<ContentNegotiationStrategy> strategies = new ArrayList<>();

    private final Set<MediaTypeFileExtensionResolver> resolvers = new LinkedHashSet<>();

    public ContentNegotiationManager(ContentNegotiationStrategy... strategies) {
        this(Arrays.asList(strategies));
    }

    public ContentNegotiationManager(Collection<ContentNegotiationStrategy> strategies) {
        Assert.notEmpty(strategies, "At least one ContentNegotiationStrategy is expected");
        this.strategies.addAll(strategies);
        for (ContentNegotiationStrategy strategy : this.strategies) {
            if (strategy instanceof MediaTypeFileExtensionResolver) {
                this.resolvers.add((MediaTypeFileExtensionResolver) strategy);
            }
        }
    }

    public ContentNegotiationManager() {
        this(new HeaderContentNegotiationStrategy());
    }

    public List<ContentNegotiationStrategy> getStrategies() {
        return this.strategies;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends ContentNegotiationStrategy> T getStrategy(Class<T> strategyType) {
        for (ContentNegotiationStrategy strategy : getStrategies()) {
            if (strategyType.isInstance(strategy)) {
                return (T) strategy;
            }
        }
        return null;
    }

    public void addFileExtensionResolvers(MediaTypeFileExtensionResolver... resolvers) {
        Collections.addAll(this.resolvers, resolvers);
    }

    @Override
    public List<MediaType> resolveMediaTypes(NativeWebRequest request) throws HttpMediaTypeNotAcceptableException {
        for (ContentNegotiationStrategy strategy : this.strategies) {
            List<MediaType> mediaTypes = strategy.resolveMediaTypes(request);
            if (mediaTypes.equals(MEDIA_TYPE_ALL_LIST)) {
                continue;
            }
            return mediaTypes;
        }
        return MEDIA_TYPE_ALL_LIST;
    }

    @Override
    public List<String> resolveFileExtensions(MediaType mediaType) {
        Set<String> result = new LinkedHashSet<>();
        for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
            result.addAll(resolver.resolveFileExtensions(mediaType));
        }
        return new ArrayList<>(result);
    }

    @Override
    public List<String> getAllFileExtensions() {
        Set<String> result = new LinkedHashSet<>();
        for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
            result.addAll(resolver.getAllFileExtensions());
        }
        return new ArrayList<>(result);
    }

}

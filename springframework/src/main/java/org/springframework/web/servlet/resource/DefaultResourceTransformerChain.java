package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

class DefaultResourceTransformerChain implements ResourceTransformerChain {

    private final ResourceResolverChain resolverChain;

    @Nullable
    private final ResourceTransformer transformer;

    @Nullable
    private final ResourceTransformerChain nextChain;

    public DefaultResourceTransformerChain(ResourceResolverChain resolverChain, @Nullable List<ResourceTransformer> transformers) {
        Assert.notNull(resolverChain, "ResourceResolverChain is required");
        this.resolverChain = resolverChain;
        transformers = (transformers != null ? transformers : Collections.emptyList());
        DefaultResourceTransformerChain chain = initTransformerChain(resolverChain, new ArrayList<>(transformers));
        this.transformer = chain.transformer;
        this.nextChain = chain.nextChain;
    }

    private DefaultResourceTransformerChain initTransformerChain(ResourceResolverChain resolverChain, ArrayList<ResourceTransformer> transformers) {
        DefaultResourceTransformerChain chain = new DefaultResourceTransformerChain(resolverChain, null, null);
        ListIterator<? extends ResourceTransformer> it = transformers.listIterator(transformers.size());
        while (it.hasPrevious()) {
            chain = new DefaultResourceTransformerChain(resolverChain, it.previous(), chain);
        }
        return chain;
    }

    public DefaultResourceTransformerChain(ResourceResolverChain resolverChain, @Nullable ResourceTransformer transformer, @Nullable ResourceTransformerChain chain) {
        Assert.isTrue((transformer == null && chain == null) || (transformer != null && chain != null), "Both transformer and transformer chain must be null, or neither is");
        this.resolverChain = resolverChain;
        this.transformer = transformer;
        this.nextChain = chain;
    }

    public ResourceResolverChain getResolverChain() {
        return this.resolverChain;
    }

    @Override
    public Resource transform(HttpServletRequest request, Resource resource) throws IOException {
        return (this.transformer != null && this.nextChain != null ? this.transformer.transform(request, resource, this.nextChain) : resource);
    }

}

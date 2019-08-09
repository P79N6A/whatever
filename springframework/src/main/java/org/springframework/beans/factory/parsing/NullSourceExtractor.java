package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

public class NullSourceExtractor implements SourceExtractor {

    @Override
    @Nullable
    public Object extractSource(Object sourceCandidate, @Nullable Resource definitionResource) {
        return null;
    }

}

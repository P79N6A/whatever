package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

@FunctionalInterface
public interface SourceExtractor {

    @Nullable
    Object extractSource(Object sourceCandidate, @Nullable Resource definingResource);

}

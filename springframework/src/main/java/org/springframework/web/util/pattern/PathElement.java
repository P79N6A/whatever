package org.springframework.web.util.pattern;

import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

abstract class PathElement {

    // Score related
    protected static final int WILDCARD_WEIGHT = 100;

    protected static final int CAPTURE_VARIABLE_WEIGHT = 1;

    protected static final MultiValueMap<String, String> NO_PARAMETERS = new LinkedMultiValueMap<>();

    // Position in the pattern where this path element starts
    protected final int pos;

    // The separator used in this path pattern
    protected final char separator;

    // The next path element in the chain
    @Nullable
    protected PathElement next;

    // The previous path element in the chain
    @Nullable
    protected PathElement prev;

    PathElement(int pos, char separator) {
        this.pos = pos;
        this.separator = separator;
    }

    public abstract boolean matches(int candidatePos, MatchingContext matchingContext);

    public abstract int getNormalizedLength();

    public abstract char[] getChars();

    public int getCaptureCount() {
        return 0;
    }

    public int getWildcardCount() {
        return 0;
    }

    public int getScore() {
        return 0;
    }

    protected final boolean isNoMorePattern() {
        return this.next == null;
    }

}

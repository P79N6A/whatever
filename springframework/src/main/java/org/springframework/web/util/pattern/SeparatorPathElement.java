package org.springframework.web.util.pattern;

import org.springframework.web.util.pattern.PathPattern.MatchingContext;

class SeparatorPathElement extends PathElement {

    SeparatorPathElement(int pos, char separator) {
        super(pos, separator);
    }

    @Override
    public boolean matches(int pathIndex, MatchingContext matchingContext) {
        if (pathIndex < matchingContext.pathLength && matchingContext.isSeparator(pathIndex)) {
            if (isNoMorePattern()) {
                if (matchingContext.determineRemainingPath) {
                    matchingContext.remainingPathIndex = pathIndex + 1;
                    return true;
                } else {
                    return (pathIndex + 1 == matchingContext.pathLength);
                }
            } else {
                pathIndex++;
                return (this.next != null && this.next.matches(pathIndex, matchingContext));
            }
        }
        return false;
    }

    @Override
    public int getNormalizedLength() {
        return 1;
    }

    public String toString() {
        return "Separator(" + this.separator + ")";
    }

    public char[] getChars() {
        return new char[]{this.separator};
    }

}

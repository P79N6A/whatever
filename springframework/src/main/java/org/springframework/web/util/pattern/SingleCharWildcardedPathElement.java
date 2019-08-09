package org.springframework.web.util.pattern;

import org.springframework.http.server.PathContainer.Element;
import org.springframework.http.server.PathContainer.PathSegment;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

class SingleCharWildcardedPathElement extends PathElement {

    private final char[] text;

    private final int len;

    private final int questionMarkCount;

    private final boolean caseSensitive;

    public SingleCharWildcardedPathElement(int pos, char[] literalText, int questionMarkCount, boolean caseSensitive, char separator) {
        super(pos, separator);
        this.len = literalText.length;
        this.questionMarkCount = questionMarkCount;
        this.caseSensitive = caseSensitive;
        if (caseSensitive) {
            this.text = literalText;
        } else {
            this.text = new char[literalText.length];
            for (int i = 0; i < this.len; i++) {
                this.text[i] = Character.toLowerCase(literalText[i]);
            }
        }
    }

    @Override
    public boolean matches(int pathIndex, MatchingContext matchingContext) {
        if (pathIndex >= matchingContext.pathLength) {
            // no more path left to match this element
            return false;
        }
        Element element = matchingContext.pathElements.get(pathIndex);
        if (!(element instanceof PathSegment)) {
            return false;
        }
        String value = ((PathSegment) element).valueToMatch();
        if (value.length() != this.len) {
            // Not enough data to match this path element
            return false;
        }
        char[] data = ((PathSegment) element).valueToMatchAsChars();
        if (this.caseSensitive) {
            for (int i = 0; i < this.len; i++) {
                char ch = this.text[i];
                if ((ch != '?') && (ch != data[i])) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < this.len; i++) {
                char ch = this.text[i];
                // TODO revisit performance if doing a lot of case insensitive matching
                if ((ch != '?') && (ch != Character.toLowerCase(data[i]))) {
                    return false;
                }
            }
        }
        pathIndex++;
        if (isNoMorePattern()) {
            if (matchingContext.determineRemainingPath) {
                matchingContext.remainingPathIndex = pathIndex;
                return true;
            } else {
                if (pathIndex == matchingContext.pathLength) {
                    return true;
                } else {
                    return (matchingContext.isMatchOptionalTrailingSeparator() && (pathIndex + 1) == matchingContext.pathLength && matchingContext.isSeparator(pathIndex));
                }
            }
        } else {
            return (this.next != null && this.next.matches(pathIndex, matchingContext));
        }
    }

    @Override
    public int getWildcardCount() {
        return this.questionMarkCount;
    }

    @Override
    public int getNormalizedLength() {
        return this.len;
    }

    public String toString() {
        return "SingleCharWildcarded(" + String.valueOf(this.text) + ")";
    }

    @Override
    public char[] getChars() {
        return this.text;
    }

}

package org.springframework.web.util.pattern;

import org.springframework.http.server.PathContainer.PathSegment;
import org.springframework.lang.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CaptureVariablePathElement extends PathElement {

    private final String variableName;

    @Nullable
    private Pattern constraintPattern;

    CaptureVariablePathElement(int pos, char[] captureDescriptor, boolean caseSensitive, char separator) {
        super(pos, separator);
        int colon = -1;
        for (int i = 0; i < captureDescriptor.length; i++) {
            if (captureDescriptor[i] == ':') {
                colon = i;
                break;
            }
        }
        if (colon == -1) {
            // no constraint
            this.variableName = new String(captureDescriptor, 1, captureDescriptor.length - 2);
        } else {
            this.variableName = new String(captureDescriptor, 1, colon - 1);
            if (caseSensitive) {
                this.constraintPattern = Pattern.compile(new String(captureDescriptor, colon + 1, captureDescriptor.length - colon - 2));
            } else {
                this.constraintPattern = Pattern.compile(new String(captureDescriptor, colon + 1, captureDescriptor.length - colon - 2), Pattern.CASE_INSENSITIVE);
            }
        }
    }

    @Override
    public boolean matches(int pathIndex, PathPattern.MatchingContext matchingContext) {
        if (pathIndex >= matchingContext.pathLength) {
            // no more path left to match this element
            return false;
        }
        String candidateCapture = matchingContext.pathElementValue(pathIndex);
        if (candidateCapture.length() == 0) {
            return false;
        }
        if (this.constraintPattern != null) {
            // TODO possible optimization - only regex match if rest of pattern matches?
            // Benefit likely to vary pattern to pattern
            Matcher matcher = this.constraintPattern.matcher(candidateCapture);
            if (matcher.groupCount() != 0) {
                throw new IllegalArgumentException("No capture groups allowed in the constraint regex: " + this.constraintPattern.pattern());
            }
            if (!matcher.matches()) {
                return false;
            }
        }
        boolean match = false;
        pathIndex++;
        if (isNoMorePattern()) {
            if (matchingContext.determineRemainingPath) {
                matchingContext.remainingPathIndex = pathIndex;
                match = true;
            } else {
                // Needs to be at least one character #SPR15264
                match = (pathIndex == matchingContext.pathLength);
                if (!match && matchingContext.isMatchOptionalTrailingSeparator()) {
                    match = //(nextPos > candidateIndex) &&
                            (pathIndex + 1) == matchingContext.pathLength && matchingContext.isSeparator(pathIndex);
                }
            }
        } else {
            if (this.next != null) {
                match = this.next.matches(pathIndex, matchingContext);
            }
        }
        if (match && matchingContext.extractingVariables) {
            matchingContext.set(this.variableName, candidateCapture, ((PathSegment) matchingContext.pathElements.get(pathIndex - 1)).parameters());
        }
        return match;
    }

    public String getVariableName() {
        return this.variableName;
    }

    @Override
    public int getNormalizedLength() {
        return 1;
    }

    @Override
    public int getWildcardCount() {
        return 0;
    }

    @Override
    public int getCaptureCount() {
        return 1;
    }

    @Override
    public int getScore() {
        return CAPTURE_VARIABLE_WEIGHT;
    }

    public String toString() {
        return "CaptureVariable({" + this.variableName + (this.constraintPattern != null ? ":" + this.constraintPattern.pattern() : "") + "})";
    }

    public char[] getChars() {
        StringBuilder b = new StringBuilder();
        b.append("{");
        b.append(this.variableName);
        if (this.constraintPattern != null) {
            b.append(":").append(this.constraintPattern.pattern());
        }
        b.append("}");
        return b.toString().toCharArray();
    }

}

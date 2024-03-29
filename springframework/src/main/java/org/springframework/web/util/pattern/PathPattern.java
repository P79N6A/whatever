package org.springframework.web.util.pattern;

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.PathContainer.Element;
import org.springframework.http.server.PathContainer.Separator;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.*;

public class PathPattern implements Comparable<PathPattern> {

    private static final PathContainer EMPTY_PATH = PathContainer.parsePath("");

    public static final Comparator<PathPattern> SPECIFICITY_COMPARATOR = Comparator.nullsLast(Comparator.<PathPattern>comparingInt(p -> p.isCatchAll() ? 1 : 0).thenComparingInt(p -> p.isCatchAll() ? scoreByNormalizedLength(p) : 0).thenComparingInt(PathPattern::getScore).thenComparingInt(PathPattern::scoreByNormalizedLength));

    private final String patternString;

    private final PathPatternParser parser;

    private final char separator;

    private final boolean matchOptionalTrailingSeparator;

    private final boolean caseSensitive;

    @Nullable
    private final PathElement head;

    private int capturedVariableCount;

    private int normalizedLength;

    private boolean endsWithSeparatorWildcard = false;

    private int score;

    private boolean catchAll = false;

    PathPattern(String patternText, PathPatternParser parser, @Nullable PathElement head) {
        this.patternString = patternText;
        this.parser = parser;
        this.separator = parser.getSeparator();
        this.matchOptionalTrailingSeparator = parser.isMatchOptionalTrailingSeparator();
        this.caseSensitive = parser.isCaseSensitive();
        this.head = head;
        // Compute fields for fast comparison
        PathElement elem = head;
        while (elem != null) {
            this.capturedVariableCount += elem.getCaptureCount();
            this.normalizedLength += elem.getNormalizedLength();
            this.score += elem.getScore();
            if (elem instanceof CaptureTheRestPathElement || elem instanceof WildcardTheRestPathElement) {
                this.catchAll = true;
            }
            if (elem instanceof SeparatorPathElement && elem.next != null && elem.next instanceof WildcardPathElement && elem.next.next == null) {
                this.endsWithSeparatorWildcard = true;
            }
            elem = elem.next;
        }
    }

    public String getPatternString() {
        return this.patternString;
    }

    public boolean hasPatternSyntax() {
        return this.score > 0 || this.patternString.indexOf('?') != -1;
    }

    public boolean matches(PathContainer pathContainer) {
        if (this.head == null) {
            return !hasLength(pathContainer) || (this.matchOptionalTrailingSeparator && pathContainerIsJustSeparator(pathContainer));
        } else if (!hasLength(pathContainer)) {
            if (this.head instanceof WildcardTheRestPathElement || this.head instanceof CaptureTheRestPathElement) {
                pathContainer = EMPTY_PATH; // Will allow CaptureTheRest to bind the variable to empty
            } else {
                return false;
            }
        }
        MatchingContext matchingContext = new MatchingContext(pathContainer, false);
        return this.head.matches(0, matchingContext);
    }

    @Nullable
    public PathMatchInfo matchAndExtract(PathContainer pathContainer) {
        if (this.head == null) {
            return hasLength(pathContainer) && !(this.matchOptionalTrailingSeparator && pathContainerIsJustSeparator(pathContainer)) ? null : PathMatchInfo.EMPTY;
        } else if (!hasLength(pathContainer)) {
            if (this.head instanceof WildcardTheRestPathElement || this.head instanceof CaptureTheRestPathElement) {
                pathContainer = EMPTY_PATH; // Will allow CaptureTheRest to bind the variable to empty
            } else {
                return null;
            }
        }
        MatchingContext matchingContext = new MatchingContext(pathContainer, true);
        return this.head.matches(0, matchingContext) ? matchingContext.getPathMatchResult() : null;
    }

    @Nullable
    public PathRemainingMatchInfo matchStartOfPath(PathContainer pathContainer) {
        if (this.head == null) {
            return new PathRemainingMatchInfo(pathContainer);
        } else if (!hasLength(pathContainer)) {
            return null;
        }
        MatchingContext matchingContext = new MatchingContext(pathContainer, true);
        matchingContext.setMatchAllowExtraPath();
        boolean matches = this.head.matches(0, matchingContext);
        if (!matches) {
            return null;
        } else {
            PathRemainingMatchInfo info;
            if (matchingContext.remainingPathIndex == pathContainer.elements().size()) {
                info = new PathRemainingMatchInfo(EMPTY_PATH, matchingContext.getPathMatchResult());
            } else {
                info = new PathRemainingMatchInfo(pathContainer.subPath(matchingContext.remainingPathIndex), matchingContext.getPathMatchResult());
            }
            return info;
        }
    }

    public PathContainer extractPathWithinPattern(PathContainer path) {
        List<Element> pathElements = path.elements();
        int pathElementsCount = pathElements.size();
        int startIndex = 0;
        // Find first path element that is not a separator or a literal (i.e. the first pattern based element)
        PathElement elem = this.head;
        while (elem != null) {
            if (elem.getWildcardCount() != 0 || elem.getCaptureCount() != 0) {
                break;
            }
            elem = elem.next;
            startIndex++;
        }
        if (elem == null) {
            // There is no pattern piece
            return PathContainer.parsePath("");
        }
        // Skip leading separators that would be in the result
        while (startIndex < pathElementsCount && (pathElements.get(startIndex) instanceof Separator)) {
            startIndex++;
        }
        int endIndex = pathElements.size();
        // Skip trailing separators that would be in the result
        while (endIndex > 0 && (pathElements.get(endIndex - 1) instanceof Separator)) {
            endIndex--;
        }
        boolean multipleAdjacentSeparators = false;
        for (int i = startIndex; i < (endIndex - 1); i++) {
            if ((pathElements.get(i) instanceof Separator) && (pathElements.get(i + 1) instanceof Separator)) {
                multipleAdjacentSeparators = true;
                break;
            }
        }
        PathContainer resultPath = null;
        if (multipleAdjacentSeparators) {
            // Need to rebuild the path without the duplicate adjacent separators
            StringBuilder buf = new StringBuilder();
            int i = startIndex;
            while (i < endIndex) {
                Element e = pathElements.get(i++);
                buf.append(e.value());
                if (e instanceof Separator) {
                    while (i < endIndex && (pathElements.get(i) instanceof Separator)) {
                        i++;
                    }
                }
            }
            resultPath = PathContainer.parsePath(buf.toString());
        } else if (startIndex >= endIndex) {
            resultPath = PathContainer.parsePath("");
        } else {
            resultPath = path.subPath(startIndex, endIndex);
        }
        return resultPath;
    }

    @Override
    public int compareTo(@Nullable PathPattern otherPattern) {
        int result = SPECIFICITY_COMPARATOR.compare(this, otherPattern);
        return (result == 0 && otherPattern != null ? this.patternString.compareTo(otherPattern.patternString) : result);
    }

    public PathPattern combine(PathPattern pattern2string) {
        // If one of them is empty the result is the other. If both empty the result is ""
        if (!StringUtils.hasLength(this.patternString)) {
            if (!StringUtils.hasLength(pattern2string.patternString)) {
                return this.parser.parse("");
            } else {
                return pattern2string;
            }
        } else if (!StringUtils.hasLength(pattern2string.patternString)) {
            return this;
        }
        // /* + /hotel => /hotel
        // /*.* + /*.html => /*.html
        // However:
        // /usr + /user => /usr/user
        // /{foo} + /bar => /{foo}/bar
        if (!this.patternString.equals(pattern2string.patternString) && this.capturedVariableCount == 0 && matches(PathContainer.parsePath(pattern2string.patternString))) {
            return pattern2string;
        }
        // /hotels/* + /booking => /hotels/booking
        // /hotels/* + booking => /hotels/booking
        if (this.endsWithSeparatorWildcard) {
            return this.parser.parse(concat(this.patternString.substring(0, this.patternString.length() - 2), pattern2string.patternString));
        }
        // /hotels + /booking => /hotels/booking
        // /hotels + booking => /hotels/booking
        int starDotPos1 = this.patternString.indexOf("*.");  // Are there any file prefix/suffix things to consider?
        if (this.capturedVariableCount != 0 || starDotPos1 == -1 || this.separator == '.') {
            return this.parser.parse(concat(this.patternString, pattern2string.patternString));
        }
        // /*.html + /hotel => /hotel.html
        // /*.html + /hotel.* => /hotel.html
        String firstExtension = this.patternString.substring(starDotPos1 + 1);  // looking for the first extension
        String p2string = pattern2string.patternString;
        int dotPos2 = p2string.indexOf('.');
        String file2 = (dotPos2 == -1 ? p2string : p2string.substring(0, dotPos2));
        String secondExtension = (dotPos2 == -1 ? "" : p2string.substring(dotPos2));
        boolean firstExtensionWild = (firstExtension.equals(".*") || firstExtension.equals(""));
        boolean secondExtensionWild = (secondExtension.equals(".*") || secondExtension.equals(""));
        if (!firstExtensionWild && !secondExtensionWild) {
            throw new IllegalArgumentException("Cannot combine patterns: " + this.patternString + " and " + pattern2string);
        }
        return this.parser.parse(file2 + (firstExtensionWild ? secondExtension : firstExtension));
    }

    public boolean equals(Object other) {
        if (!(other instanceof PathPattern)) {
            return false;
        }
        PathPattern otherPattern = (PathPattern) other;
        return (this.patternString.equals(otherPattern.getPatternString()) && this.separator == otherPattern.getSeparator() && this.caseSensitive == otherPattern.caseSensitive);
    }

    public int hashCode() {
        return (this.patternString.hashCode() + this.separator) * 17 + (this.caseSensitive ? 1 : 0);
    }

    public String toString() {
        return this.patternString;
    }

    int getScore() {
        return this.score;
    }

    boolean isCatchAll() {
        return this.catchAll;
    }

    /**
     * The normalized length is trying to measure the 'active' part of the pattern. It is computed
     * by assuming all capture variables have a normalized length of 1. Effectively this means changing
     * your variable name lengths isn't going to change the length of the active part of the pattern.
     * Useful when comparing two patterns.
     */
    int getNormalizedLength() {
        return this.normalizedLength;
    }

    char getSeparator() {
        return this.separator;
    }

    int getCapturedVariableCount() {
        return this.capturedVariableCount;
    }

    String toChainString() {
        StringBuilder buf = new StringBuilder();
        PathElement pe = this.head;
        while (pe != null) {
            buf.append(pe.toString()).append(" ");
            pe = pe.next;
        }
        return buf.toString().trim();
    }

    /**
     * Return the string form of the pattern built from walking the path element chain.
     *
     * @return the string form of the pattern
     */
    String computePatternString() {
        StringBuilder buf = new StringBuilder();
        PathElement pe = this.head;
        while (pe != null) {
            buf.append(pe.getChars());
            pe = pe.next;
        }
        return buf.toString();
    }

    @Nullable
    PathElement getHeadSection() {
        return this.head;
    }

    /**
     * Join two paths together including a separator if necessary.
     * Extraneous separators are removed (if the first path
     * ends with one and the second path starts with one).
     *
     * @param path1 first path
     * @param path2 second path
     * @return joined path that may include separator if necessary
     */
    private String concat(String path1, String path2) {
        boolean path1EndsWithSeparator = (path1.charAt(path1.length() - 1) == this.separator);
        boolean path2StartsWithSeparator = (path2.charAt(0) == this.separator);
        if (path1EndsWithSeparator && path2StartsWithSeparator) {
            return path1 + path2.substring(1);
        } else if (path1EndsWithSeparator || path2StartsWithSeparator) {
            return path1 + path2;
        } else {
            return path1 + this.separator + path2;
        }
    }

    /**
     * Return if the container is not null and has more than zero elements.
     *
     * @param container a path container
     * @return {@code true} has more than zero elements
     */
    private boolean hasLength(@Nullable PathContainer container) {
        return container != null && container.elements().size() > 0;
    }

    private static int scoreByNormalizedLength(PathPattern pattern) {
        return -pattern.getNormalizedLength();
    }

    private boolean pathContainerIsJustSeparator(PathContainer pathContainer) {
        return pathContainer.value().length() == 1 && pathContainer.value().charAt(0) == this.separator;
    }

    /**
     * Holder for URI variables and path parameters (matrix variables) extracted
     * based on the pattern for a given matched path.
     */
    public static class PathMatchInfo {

        private static final PathMatchInfo EMPTY = new PathMatchInfo(Collections.emptyMap(), Collections.emptyMap());

        private final Map<String, String> uriVariables;

        private final Map<String, MultiValueMap<String, String>> matrixVariables;

        PathMatchInfo(Map<String, String> uriVars, @Nullable Map<String, MultiValueMap<String, String>> matrixVars) {
            this.uriVariables = Collections.unmodifiableMap(uriVars);
            this.matrixVariables = matrixVars != null ? Collections.unmodifiableMap(matrixVars) : Collections.emptyMap();
        }

        /**
         * Return the extracted URI variables.
         */
        public Map<String, String> getUriVariables() {
            return this.uriVariables;
        }

        /**
         * Return maps of matrix variables per path segment, keyed off by URI
         * variable name.
         */
        public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
            return this.matrixVariables;
        }

        @Override
        public String toString() {
            return "PathMatchInfo[uriVariables=" + this.uriVariables + ", " + "matrixVariables=" + this.matrixVariables + "]";
        }

    }

    /**
     * Holder for the result of a match on the start of a pattern.
     * Provides access to the remaining path not matched to the pattern as well
     * as any variables bound in that first part that was matched.
     */
    public static class PathRemainingMatchInfo {

        private final PathContainer pathRemaining;

        private final PathMatchInfo pathMatchInfo;

        PathRemainingMatchInfo(PathContainer pathRemaining) {
            this(pathRemaining, PathMatchInfo.EMPTY);
        }

        PathRemainingMatchInfo(PathContainer pathRemaining, PathMatchInfo pathMatchInfo) {
            this.pathRemaining = pathRemaining;
            this.pathMatchInfo = pathMatchInfo;
        }

        /**
         * Return the part of a path that was not matched by a pattern.
         */
        public PathContainer getPathRemaining() {
            return this.pathRemaining;
        }

        /**
         * Return variables that were bound in the part of the path that was
         * successfully matched or an empty map.
         */
        public Map<String, String> getUriVariables() {
            return this.pathMatchInfo.getUriVariables();
        }

        /**
         * Return the path parameters for each bound variable.
         */
        public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
            return this.pathMatchInfo.getMatrixVariables();
        }

    }

    /**
     * Encapsulates context when attempting a match. Includes some fixed state like the
     * candidate currently being considered for a match but also some accumulators for
     * extracted variables.
     */
    class MatchingContext {

        final PathContainer candidate;

        final List<Element> pathElements;

        final int pathLength;

        @Nullable
        private Map<String, String> extractedUriVariables;

        @Nullable
        private Map<String, MultiValueMap<String, String>> extractedMatrixVariables;

        boolean extractingVariables;

        boolean determineRemainingPath = false;

        // if determineRemaining is true, this is set to the position in
        // the candidate where the pattern finished matching - i.e. it
        // points to the remaining path that wasn't consumed
        int remainingPathIndex;

        public MatchingContext(PathContainer pathContainer, boolean extractVariables) {
            this.candidate = pathContainer;
            this.pathElements = pathContainer.elements();
            this.pathLength = this.pathElements.size();
            this.extractingVariables = extractVariables;
        }

        public void setMatchAllowExtraPath() {
            this.determineRemainingPath = true;
        }

        public boolean isMatchOptionalTrailingSeparator() {
            return matchOptionalTrailingSeparator;
        }

        public void set(String key, String value, MultiValueMap<String, String> parameters) {
            if (this.extractedUriVariables == null) {
                this.extractedUriVariables = new HashMap<>();
            }
            this.extractedUriVariables.put(key, value);
            if (!parameters.isEmpty()) {
                if (this.extractedMatrixVariables == null) {
                    this.extractedMatrixVariables = new HashMap<>();
                }
                this.extractedMatrixVariables.put(key, CollectionUtils.unmodifiableMultiValueMap(parameters));
            }
        }

        public PathMatchInfo getPathMatchResult() {
            if (this.extractedUriVariables == null) {
                return PathMatchInfo.EMPTY;
            } else {
                return new PathMatchInfo(this.extractedUriVariables, this.extractedMatrixVariables);
            }
        }

        /**
         * Return if element at specified index is a separator.
         *
         * @param pathIndex possible index of a separator
         * @return {@code true} if element is a separator
         */
        boolean isSeparator(int pathIndex) {
            return this.pathElements.get(pathIndex) instanceof Separator;
        }

        /**
         * Return the decoded value of the specified element.
         *
         * @param pathIndex path element index
         * @return the decoded value
         */
        String pathElementValue(int pathIndex) {
            Element element = (pathIndex < this.pathLength) ? this.pathElements.get(pathIndex) : null;
            if (element instanceof PathContainer.PathSegment) {
                return ((PathContainer.PathSegment) element).valueToMatch();
            }
            return "";
        }

    }

}

package org.springframework.web.util.pattern;

public class PathPatternParser {

    private boolean matchOptionalTrailingSeparator = true;

    private boolean caseSensitive = true;

    public void setMatchOptionalTrailingSeparator(boolean matchOptionalTrailingSeparator) {
        this.matchOptionalTrailingSeparator = matchOptionalTrailingSeparator;
    }

    public boolean isMatchOptionalTrailingSeparator() {
        return this.matchOptionalTrailingSeparator;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isCaseSensitive() {
        return this.caseSensitive;
    }

    char getSeparator() {
        return '/';
    }

    public PathPattern parse(String pathPattern) throws PatternParseException {
        return new InternalPathPatternParser(this).parse(pathPattern);
    }

}

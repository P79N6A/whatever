package org.springframework.http.server;

import org.springframework.util.MultiValueMap;

import java.util.List;

public interface PathContainer {

    String value();

    List<Element> elements();

    default PathContainer subPath(int index) {
        return subPath(index, elements().size());
    }

    default PathContainer subPath(int startIndex, int endIndex) {
        return DefaultPathContainer.subPath(this, startIndex, endIndex);
    }

    static PathContainer parsePath(String path) {
        return DefaultPathContainer.createFromUrlPath(path);
    }

    interface Element {

        String value();

    }

    interface Separator extends Element {
    }

    interface PathSegment extends Element {

        String valueToMatch();

        char[] valueToMatchAsChars();

        MultiValueMap<String, String> parameters();

    }

}

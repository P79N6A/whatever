package org.springframework.http.server.reactive;

import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

class TomcatHeadersAdapter implements MultiValueMap<String, String> {

    private final MimeHeaders headers;

    TomcatHeadersAdapter(MimeHeaders headers) {
        this.headers = headers;
    }

    @Override
    public String getFirst(String key) {
        return this.headers.getHeader(key);
    }

    @Override
    public void add(String key, @Nullable String value) {
        this.headers.addValue(key).setString(value);
    }

    @Override
    public void addAll(String key, List<? extends String> values) {
        values.forEach(value -> add(key, value));
    }

    @Override
    public void addAll(MultiValueMap<String, String> values) {
        values.forEach(this::addAll);
    }

    @Override
    public void set(String key, @Nullable String value) {
        this.headers.setValue(key).setString(value);
    }

    @Override
    public void setAll(Map<String, String> values) {
        values.forEach(this::set);
    }

    @Override
    public Map<String, String> toSingleValueMap() {
        Map<String, String> singleValueMap = new LinkedHashMap<>(this.headers.size());
        this.keySet().forEach(key -> singleValueMap.put(key, getFirst(key)));
        return singleValueMap;
    }

    @Override
    public int size() {
        Enumeration<String> names = this.headers.names();
        int size = 0;
        while (names.hasMoreElements()) {
            size++;
            names.nextElement();
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return (this.headers.size() == 0);
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof String) {
            return (this.headers.findHeader((String) key, 0) != -1);
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value instanceof String) {
            MessageBytes needle = MessageBytes.newInstance();
            needle.setString((String) value);
            for (int i = 0; i < this.headers.size(); i++) {
                if (this.headers.getValue(i).equals(needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @Nullable
    public List<String> get(Object key) {
        if (containsKey(key)) {
            return Collections.list(this.headers.values((String) key));
        }
        return null;
    }

    @Override
    @Nullable
    public List<String> put(String key, List<String> value) {
        List<String> previousValues = get(key);
        this.headers.removeHeader(key);
        value.forEach(v -> this.headers.addValue(key).setString(v));
        return previousValues;
    }

    @Override
    @Nullable
    public List<String> remove(Object key) {
        if (key instanceof String) {
            List<String> previousValues = get(key);
            this.headers.removeHeader((String) key);
            return previousValues;
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> map) {
        map.forEach(this::put);
    }

    @Override
    public void clear() {
        this.headers.clear();
    }

    @Override
    public Set<String> keySet() {
        Set<String> result = new HashSet<>(8);
        Enumeration<String> names = this.headers.names();
        while (names.hasMoreElements()) {
            result.add(names.nextElement());
        }
        return result;
    }

    @Override
    public Collection<List<String>> values() {
        return keySet().stream().map(this::get).collect(Collectors.toList());
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        return new AbstractSet<Entry<String, List<String>>>() {
            @Override
            public Iterator<Entry<String, List<String>>> iterator() {
                return new EntryIterator();
            }

            @Override
            public int size() {
                return headers.size();
            }
        };
    }

    @Override
    public String toString() {
        return HttpHeaders.formatHeaders(this);
    }

    private class EntryIterator implements Iterator<Entry<String, List<String>>> {

        private Enumeration<String> names = headers.names();

        @Override
        public boolean hasNext() {
            return this.names.hasMoreElements();
        }

        @Override
        public Entry<String, List<String>> next() {
            return new HeaderEntry(this.names.nextElement());
        }

    }

    private final class HeaderEntry implements Entry<String, List<String>> {

        private final String key;

        HeaderEntry(String key) {
            this.key = key;
        }

        @Override
        public String getKey() {
            return this.key;
        }

        @Nullable
        @Override
        public List<String> getValue() {
            return get(this.key);
        }

        @Nullable
        @Override
        public List<String> setValue(List<String> value) {
            List<String> previous = getValue();
            headers.removeHeader(this.key);
            addAll(this.key, value);
            return previous;
        }

    }

}

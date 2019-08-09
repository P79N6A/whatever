package org.springframework.http;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.stream.Collectors;

class ReadOnlyHttpHeaders extends HttpHeaders {

    private static final long serialVersionUID = -8578554704772377436L;

    @Nullable
    private MediaType cachedContentType;

    @Nullable
    private List<MediaType> cachedAccept;

    ReadOnlyHttpHeaders(HttpHeaders headers) {
        super(headers.headers);
    }

    @Override
    public MediaType getContentType() {
        if (this.cachedContentType != null) {
            return this.cachedContentType;
        } else {
            MediaType contentType = super.getContentType();
            this.cachedContentType = contentType;
            return contentType;
        }
    }

    @Override
    public List<MediaType> getAccept() {
        if (this.cachedAccept != null) {
            return this.cachedAccept;
        } else {
            List<MediaType> accept = super.getAccept();
            this.cachedAccept = accept;
            return accept;
        }
    }

    @Override
    public List<String> get(Object key) {
        List<String> values = this.headers.get(key);
        return (values != null ? Collections.unmodifiableList(values) : null);
    }

    @Override
    public void add(String headerName, @Nullable String headerValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(String key, List<? extends String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(MultiValueMap<String, String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(String headerName, @Nullable String headerValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAll(Map<String, String> values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> toSingleValueMap() {
        return Collections.unmodifiableMap(this.headers.toSingleValueMap());
    }

    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet(this.headers.keySet());
    }

    @Override
    public List<String> put(String key, List<String> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<List<String>> values() {
        return Collections.unmodifiableCollection(this.headers.values());
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        return Collections.unmodifiableSet(this.headers.entrySet().stream().map(AbstractMap.SimpleImmutableEntry::new).collect(Collectors.toSet()));
    }

}

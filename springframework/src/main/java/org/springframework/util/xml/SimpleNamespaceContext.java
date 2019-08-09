package org.springframework.util.xml;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.*;

public class SimpleNamespaceContext implements NamespaceContext {

    private final Map<String, String> prefixToNamespaceUri = new HashMap<>();

    private final Map<String, Set<String>> namespaceUriToPrefixes = new HashMap<>();

    private String defaultNamespaceUri = "";

    @Override
    public String getNamespaceURI(String prefix) {
        Assert.notNull(prefix, "No prefix given");
        if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
            return XMLConstants.XML_NS_URI;
        } else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        } else if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
            return this.defaultNamespaceUri;
        } else if (this.prefixToNamespaceUri.containsKey(prefix)) {
            return this.prefixToNamespaceUri.get(prefix);
        }
        return "";
    }

    @Override
    @Nullable
    public String getPrefix(String namespaceUri) {
        Set<String> prefixes = getPrefixesSet(namespaceUri);
        return (!prefixes.isEmpty() ? prefixes.iterator().next() : null);
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceUri) {
        return getPrefixesSet(namespaceUri).iterator();
    }

    private Set<String> getPrefixesSet(String namespaceUri) {
        Assert.notNull(namespaceUri, "No namespaceUri given");
        if (this.defaultNamespaceUri.equals(namespaceUri)) {
            return Collections.singleton(XMLConstants.DEFAULT_NS_PREFIX);
        } else if (XMLConstants.XML_NS_URI.equals(namespaceUri)) {
            return Collections.singleton(XMLConstants.XML_NS_PREFIX);
        } else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceUri)) {
            return Collections.singleton(XMLConstants.XMLNS_ATTRIBUTE);
        } else {
            Set<String> prefixes = this.namespaceUriToPrefixes.get(namespaceUri);
            return (prefixes != null ? Collections.unmodifiableSet(prefixes) : Collections.emptySet());
        }
    }

    public void setBindings(Map<String, String> bindings) {
        bindings.forEach(this::bindNamespaceUri);
    }

    public void bindDefaultNamespaceUri(String namespaceUri) {
        bindNamespaceUri(XMLConstants.DEFAULT_NS_PREFIX, namespaceUri);
    }

    public void bindNamespaceUri(String prefix, String namespaceUri) {
        Assert.notNull(prefix, "No prefix given");
        Assert.notNull(namespaceUri, "No namespaceUri given");
        if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
            this.defaultNamespaceUri = namespaceUri;
        } else {
            this.prefixToNamespaceUri.put(prefix, namespaceUri);
            Set<String> prefixes = this.namespaceUriToPrefixes.computeIfAbsent(namespaceUri, k -> new LinkedHashSet<>());
            prefixes.add(prefix);
        }
    }

    public void removeBinding(@Nullable String prefix) {
        if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
            this.defaultNamespaceUri = "";
        } else if (prefix != null) {
            String namespaceUri = this.prefixToNamespaceUri.remove(prefix);
            if (namespaceUri != null) {
                Set<String> prefixes = this.namespaceUriToPrefixes.get(namespaceUri);
                if (prefixes != null) {
                    prefixes.remove(prefix);
                    if (prefixes.isEmpty()) {
                        this.namespaceUriToPrefixes.remove(namespaceUri);
                    }
                }
            }
        }
    }

    public void clear() {
        this.prefixToNamespaceUri.clear();
        this.namespaceUriToPrefixes.clear();
    }

    public Iterator<String> getBoundPrefixes() {
        return this.prefixToNamespaceUri.keySet().iterator();
    }

}

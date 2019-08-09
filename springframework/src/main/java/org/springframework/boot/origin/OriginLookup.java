package org.springframework.boot.origin;

@FunctionalInterface
public interface OriginLookup<K> {

    Origin getOrigin(K key);

    @SuppressWarnings("unchecked")
    static <K> Origin getOrigin(Object source, K key) {
        if (!(source instanceof OriginLookup)) {
            return null;
        }
        try {
            return ((OriginLookup<K>) source).getOrigin(key);
        } catch (Throwable ex) {
            return null;
        }
    }

}

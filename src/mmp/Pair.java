package mmp;

public class Pair<K, V> {

    public K key;

    public V value;

    public Pair() {
    }

    public Pair(K first, V second) {
        this.key = first;
        this.value = second;
    }

    @Override
    public String toString() {
        return "Pair [key=" + key + ", value=" + value + "]";
    }

}
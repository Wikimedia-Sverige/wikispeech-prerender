package se.wikimedia.wikispeech.prerender;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class LocalCache<K, V> {

    private int maxSize;
    private Map<K,V> cache;

    public LocalCache(int maxSize) {
        this.maxSize = maxSize;
        cache = new LinkedHashMap<>(maxSize);
    }

    public V get(K key) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        V value = doGet(key);
        cache.put(key, value);
        if (cache.size() > maxSize) {
            cache.entrySet().iterator().remove();
        }
        return value;
    }

    public abstract V doGet(K key);
}

package se.wikimedia.wikispeech.prerender.service.prevalence.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Intern<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<T, T> map = new HashMap<>();

    public T intern(T value) {
        T interned = map.get(value);
        if (interned == null) {
            map.put(value, value);
            interned = value;
        }
        return interned;
    }

}

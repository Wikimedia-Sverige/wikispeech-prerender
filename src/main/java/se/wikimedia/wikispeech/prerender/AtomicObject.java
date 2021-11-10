package se.wikimedia.wikispeech.prerender;

public class AtomicObject<T> {

    private T object;

    public AtomicObject() {
    }

    public AtomicObject(T object) {
        this.object = object;
    }

    public T get() {
        return object;
    }

    public void set(T object) {
        this.object = object;
    }
}

package se.wikimedia.wikispeech.prerender;

public abstract interface Collector<T> {

    public void collect(T collected);

}

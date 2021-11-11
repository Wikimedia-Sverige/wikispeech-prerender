package se.wikimedia.wikispeech.prerender;

public abstract interface Collector<T> {

    /**
     * @param collected
     * @return false if no more data should be collected
     */
    public boolean collect(T collected);

}

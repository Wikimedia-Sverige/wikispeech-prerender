package se.wikimedia.wikispeech.prerender.site;

import java.time.LocalDateTime;

public abstract interface TitleCollector {

    /**
     * @param title
     * @param previousMustHaveBeenRenderedBefore If previously rendered, then that must have been rendered before this date in order to trigger pushing to queue
     */
    public abstract void collect(String title, LocalDateTime previousMustHaveBeenRenderedBefore);

}

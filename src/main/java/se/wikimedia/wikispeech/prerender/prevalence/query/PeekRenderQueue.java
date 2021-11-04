package se.wikimedia.wikispeech.prerender.prevalence.query;

import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.prevalence.domain.RenderQueueItem;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;

import java.util.Date;

public class PeekRenderQueue implements Query<Root, RenderQueueItem> {

    @Override
    public RenderQueueItem query(Root root, Date date) throws Exception {
        return root.getRenderQueue().peek();
    }
}

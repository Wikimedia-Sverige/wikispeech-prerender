package se.wikimedia.wikispeech.prerender.prevalence.transaction;

import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.prevalence.domain.RenderQueueItem;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;

import java.util.Date;

public class PollRenderQueue implements TransactionWithQuery<Root, RenderQueueItem> {

    private static final long serialVersionUID = 1L;

    @Override
    public RenderQueueItem executeAndQuery(Root root, Date date) throws Exception {
        return root.getRenderQueue().poll();
    }
}


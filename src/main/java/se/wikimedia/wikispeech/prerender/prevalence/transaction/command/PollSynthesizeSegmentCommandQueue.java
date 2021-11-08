package se.wikimedia.wikispeech.prerender.prevalence.transaction.command;

import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.prevalence.domain.command.Command;

import java.util.Date;

public class PollSynthesizeSegmentCommandQueue implements TransactionWithQuery<Root, Command> {

    private static final long serialVersionUID = 1L;

    @Override
    public Command executeAndQuery(Root root, Date date) throws Exception {
        return root.getSynthesizeSegmentsQueue().poll();
    }
}

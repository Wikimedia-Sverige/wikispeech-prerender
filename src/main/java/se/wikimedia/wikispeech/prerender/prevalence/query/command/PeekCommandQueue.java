package se.wikimedia.wikispeech.prerender.prevalence.query.command;

import org.prevayler.Query;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;
import se.wikimedia.wikispeech.prerender.prevalence.domain.command.Command;

import java.util.Date;

public class PeekCommandQueue implements Query<Root, Command> {

    @Override
    public Command query(Root root, Date date) throws Exception {
        return root.getCommandQueue().peek();
    }
}

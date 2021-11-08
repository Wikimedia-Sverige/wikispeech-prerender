package se.wikimedia.wikispeech.prerender.prevalence.transaction.command;

import lombok.Data;
import org.prevayler.TransactionWithQuery;
import se.wikimedia.wikispeech.prerender.prevalence.Prevalence;
import se.wikimedia.wikispeech.prerender.prevalence.domain.Root;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public abstract class PushToCommandQueue<Command extends se.wikimedia.wikispeech.prerender.prevalence.domain.command.Command>
        implements TransactionWithQuery<Root, Command> {

    private static final long serialVersionUID = 1L;

    public abstract Command commandFactory(LocalDateTime executionTime);

    @Override
    public Command executeAndQuery(Root root, Date date) throws Exception {
        Command command = commandFactory(Prevalence.toLocalDateTime(date));
        root.getCommandQueue().add(command);
        return command;
    }

}

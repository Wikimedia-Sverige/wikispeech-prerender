package se.wikimedia.wikispeech.prerender.service.prevalence.domain.command;

import lombok.Data;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

@Data
public class SegmentPage extends Command {

    private static final long serialVersionUID = 1L;

    private Wiki wiki;
    private String title;

    @Override
    public <R> R accept(CommandVisitor<R> visitor) {
        return visitor.visit(this);
    }
}

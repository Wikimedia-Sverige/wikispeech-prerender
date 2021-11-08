package se.wikimedia.wikispeech.prerender.prevalence.domain.command;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public abstract class Command implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDateTime created;

    public abstract <R> R accept(CommandVisitor<R> visitor);

}

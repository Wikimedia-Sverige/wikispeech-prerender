package se.wikimedia.wikispeech.prerender.service.prevalence.domain;

import lombok.Data;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.command.Command;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.command.SynthesizeSegmentVoice;
import se.wikimedia.wikispeech.prerender.service.prevalence.domain.state.Wiki;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

@Data
public class Root implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, Wiki> wikiByConsumerUrl = new HashMap<>();

    private Queue<Command> commandQueue = new LinkedList<>();
    private Queue<SynthesizeSegmentVoice> synthesizeSegmentsQueue = new LinkedList<>();

    private Intern<String> internedVoices = new Intern<>();
    private Intern<String> internedLanguages = new Intern<>();

}

package se.wikimedia.wikispeech.prerender.site;

import java.util.List;

public abstract class RemoteSite {

    public abstract String getName();

    public abstract String getLanguage();

    public abstract String getConsumerUrl();

    public abstract List<String> getVoices();

    public abstract void collectTitles(TitleCollector collector);

}

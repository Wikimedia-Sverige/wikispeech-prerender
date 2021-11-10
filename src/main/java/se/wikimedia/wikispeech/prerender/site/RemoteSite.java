package se.wikimedia.wikispeech.prerender.site;

import java.util.List;

public abstract class RemoteSite {

    public abstract String getName();

    public abstract String getLanguage();

    public abstract String getConsumerUrl();

    public abstract List<String> getVoices();

    public abstract void queueCommands() throws Exception;

    public abstract void start();

    public abstract void stop();

}

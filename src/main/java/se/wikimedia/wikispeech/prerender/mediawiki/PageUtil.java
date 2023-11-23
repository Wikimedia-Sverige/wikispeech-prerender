package se.wikimedia.wikispeech.prerender.mediawiki;

public class PageUtil {

    public static String normalizeTitle(String title) {
        title = title.replaceAll("_", " ");
        return title;
    }

}

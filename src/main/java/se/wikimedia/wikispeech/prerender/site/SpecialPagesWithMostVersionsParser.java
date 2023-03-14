package se.wikimedia.wikispeech.prerender.site;

import lombok.Data;
import org.codelibs.nekohtml.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import se.wikimedia.wikispeech.prerender.Collector;

import javax.xml.xpath.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpecialPagesWithMostVersionsParser {

    private XPathExpression entriesExpression;
    private XPathExpression entryHrefExpression;
    private XPathExpression entryTitleExpression;
    private XPathExpression entryVersionsCountExpression;
    private Pattern entryVersionsCountPattern = Pattern.compile("((\\d+ )+)versioner");

    public SpecialPagesWithMostVersionsParser() throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        entriesExpression = xPath.compile("//OL[@start='1' and @class='special']/LI");
        entryHrefExpression = xPath.compile("A[1]/@href");
        entryTitleExpression = xPath.compile("A[1]/@title");
        entryVersionsCountExpression = xPath.compile("A[2]/text()");
    }

    public void collect(Collector<String> collector, String url) throws XPathExpressionException, IOException, SAXException {
        DOMParser parser = new DOMParser();
        parser.parse(url);
        collect(collector, parser.getDocument());
    }

    public void collect(Collector<String> collector, InputSource html) throws XPathExpressionException, IOException, SAXException {
        DOMParser parser = new DOMParser();
        parser.parse(html);
        collect(collector, parser.getDocument());
    }

    public void collect(Collector<String> collector, Document document) throws XPathExpressionException {
        NodeList entryNodes = (NodeList) entriesExpression.evaluate(document, XPathConstants.NODESET);
        for (int entryNodesIndex = 0; entryNodesIndex < entryNodes.getLength(); entryNodesIndex++) {
            Node entryNode = entryNodes.item(entryNodesIndex);
            Entry entry = new Entry();
            entry.setHref(entryHrefExpression.evaluate(entryNode));
            entry.setTitle(entryTitleExpression.evaluate(entryNode));
            String entryVersionsCountText = entryVersionsCountExpression.evaluate(entryNode);
            entryVersionsCountText = entryVersionsCountText.replaceAll(String.valueOf((char) 160), " "); // nbsp
            Matcher versionsCountMatcher = entryVersionsCountPattern.matcher(entryVersionsCountText);
            if (!versionsCountMatcher.matches()) {
                throw new RuntimeException();
            }
            entry.setVersionsCount(Integer.parseInt(versionsCountMatcher.group(1).replaceAll("\\s+", "")));
            if (!collector.collect(entry.getTitle())) {
                break;
            }
        }
    }

    @Data
    private static class Entry {
        private String href;
        private String title;
        private int versionsCount;
    }


}

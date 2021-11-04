package se.wikimedia.wikispeech.prerender.site;

import org.codelibs.nekohtml.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.xpath.*;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class MainPageParser {

    private Duration previousMustHaveBeenRenderedBefore = Duration.ofDays(0);

    private XPathExpression frontPageLeftExpression;
    private XPathExpression hrefExpression;
    private XPathExpression titleExpression;
    private Pattern allowedHrefPattern = Pattern.compile("/wiki/[^:]+");

    public MainPageParser(String xpathPrefix) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        frontPageLeftExpression = xPath.compile(xpathPrefix + "//A[starts-with(@href, '/wiki/')]");
        hrefExpression = xPath.compile("@href");
        titleExpression = xPath.compile("@title");
    }

    public void collect(TitleCollector collector, String url) throws XPathExpressionException, IOException, SAXException {
        DOMParser parser = new DOMParser();
        parser.parse(url);
        collect(collector, parser.getDocument());
    }

    public void collect(TitleCollector collector, InputSource html) throws XPathExpressionException, IOException, SAXException {
        DOMParser parser = new DOMParser();
        parser.parse(html);
        collect(collector, parser.getDocument());
    }

    public void collect(TitleCollector collector, Document document) throws XPathExpressionException {
        collect((NodeList) frontPageLeftExpression.evaluate(document, XPathConstants.NODESET), collector);
    }

    private void collect(NodeList hrefNodes, TitleCollector collector) throws XPathExpressionException {
        for (int hrefNodeIndex = 0; hrefNodeIndex < hrefNodes.getLength(); hrefNodeIndex++) {
            Node hrefNode = hrefNodes.item(hrefNodeIndex);
            String href = hrefExpression.evaluate(hrefNode);
            if (allowedHrefPattern.matcher(href).matches()) {
                String title = titleExpression.evaluate(hrefNode);
                collector.collect(title, LocalDateTime.now().minus(previousMustHaveBeenRenderedBefore));
            }
        }
    }

}

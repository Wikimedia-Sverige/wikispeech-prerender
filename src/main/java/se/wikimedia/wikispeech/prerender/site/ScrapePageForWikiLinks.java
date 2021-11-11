package se.wikimedia.wikispeech.prerender.site;

import lombok.Data;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.codelibs.nekohtml.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import se.wikimedia.wikispeech.prerender.Collector;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

@Data
public class ScrapePageForWikiLinks {

    private String consumerUrl;
    private String title;

    private String linksExpression = "//*[@id='bodyContent']//A[starts-with(@href, '/wiki/')]";
    private String allowedHrefPattern = "/wiki/[^:]+";

    private Collector<String> collector;

    public void execute() throws Exception {

        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression linksExpression = xPath.compile(this.linksExpression);
        XPathExpression hrefExpression = xPath.compile("@href");
        XPathExpression titleExpression = xPath.compile("@title");
        Pattern allowedHrefPattern = Pattern.compile(this.allowedHrefPattern);

        DOMParser parser = new DOMParser();

        OkHttpClient client = new OkHttpClient.Builder().build();

        Request request = new Request.Builder()
                .url(HttpUrl.parse(consumerUrl).newBuilder().addQueryParameter("title", title).build())
                .addHeader("User-Agent", "WMSE Wikispeech API Java client")
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();

        if (response.code() != 200) {
            throw new IOException("Response" + response);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(49152);
        IOUtils.copy(response.body().byteStream(), baos);
        byte[] htmlBytes = baos.toByteArray();
        parser.parse(new InputSource(new ByteArrayInputStream(htmlBytes)));
        Document document = parser.getDocument();

        NodeList linkNodes = (NodeList) linksExpression.evaluate(document, XPathConstants.NODESET);
        for (int linkNodeIndex = 0; linkNodeIndex < linkNodes.getLength(); linkNodeIndex++) {
            Node linkNode = linkNodes.item(linkNodeIndex);
            String href = hrefExpression.evaluate(linkNode);
            if (allowedHrefPattern.matcher(href).matches()) {
                String title = titleExpression.evaluate(linkNode);
                if (!collector.collect(title)) {
                    break;
                }
            }
        }
    }

}

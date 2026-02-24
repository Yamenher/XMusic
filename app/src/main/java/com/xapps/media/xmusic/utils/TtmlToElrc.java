package com.xapps.media.xmusic.lyric;

import android.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class TtmlToElrc {

    private static final String NS_TTML = "http://www.w3.org/ns/ttml";
    private static final String NS_TTM = "http://www.w3.org/ns/ttml#metadata";
    private static final String NS_XML = "http://www.w3.org/XML/1998/namespace";
    private static final String NS_ITUNES = "http://music.apple.com/lyric-ttml-internal";

    public static String convert(String ttmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(ttmlContent)));

            Element root = doc.getDocumentElement();

            String timingMode = root.getAttributeNS(NS_ITUNES, "timing");
            if ("None".equals(timingMode)) {
                return null;
            }

            String mainVocalistId = findMainVocalistId(root);

            NodeList bodies = root.getElementsByTagNameNS(NS_TTML, "body");
            if (bodies.getLength() == 0) return "";
            Element body = (Element) bodies.item(0);

            StringBuilder sb = new StringBuilder();
            NodeList divs = body.getElementsByTagNameNS(NS_TTML, "div");
            for (int i = 0; i < divs.getLength(); i++) {
                Element div = (Element) divs.item(i);
                String divAgentId = div.getAttributeNS(NS_TTM, "agent");
                NodeList paragraphs = div.getElementsByTagNameNS(NS_TTML, "p");
                for (int j = 0; j < paragraphs.getLength(); j++) {
                    Element p = (Element) paragraphs.item(j);
                    processParagraph(p, sb, mainVocalistId, divAgentId);
                }
            }

            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String findMainVocalistId(Element root) {
        String mainId = "v1";
        NodeList agents = root.getElementsByTagNameNS(NS_TTM, "agent");
        for (int i = 0; i < agents.getLength(); i++) {
            Element agent = (Element) agents.item(i);
            if ("person".equals(agent.getAttribute("type"))) {
                String id = agent.getAttributeNS(NS_XML, "id");
                if (id != null && !id.isEmpty()) {
                    mainId = id;
                    break;
                }
            }
        }
        return mainId;
    }

    private static void processParagraph(
            Element p, StringBuilder sb, String mainVocalistId, String divAgentId) {
        long lineStart = parseTimestamp(p.getAttribute("begin"));
        long lineEnd = parseTimestamp(p.getAttribute("end"));

        String lineAgent = p.getAttributeNS(NS_TTM, "agent");
        if (lineAgent == null || lineAgent.isEmpty()) {
            lineAgent = divAgentId;
        }
        boolean isDuet =
                (lineAgent != null && !lineAgent.isEmpty() && !lineAgent.equals(mainVocalistId));
        String vocalistPrefix = isDuet ? "v2: " : "v1: ";

        List<Element> orderedSpans = new ArrayList<>();
        findSpansRecursive(p, orderedSpans);

        sb.append("[").append(formatTimestamp(lineStart)).append("]");
        sb.append(vocalistPrefix);

        if (orderedSpans.isEmpty()) {

            String text = p.getTextContent().trim();
            sb.append(text);
        } else {
            Set<Node> backgroundSpans = new HashSet<>();
            findBackgroundSpans(p, backgroundSpans, false);

            List<Word> mainWords = new ArrayList<>();
            List<Word> bgWords = new ArrayList<>();

            for (Element span : orderedSpans) {
                String text = span.getTextContent().trim();
                if (text.isEmpty()) continue;

                long start = parseTimestamp(span.getAttribute("begin"));
                long end = parseTimestamp(span.getAttribute("end"));

                Node next = span.getNextSibling();
                boolean part = (next != null && next.getNodeType() == Node.ELEMENT_NODE);

                Word word = new Word(text, start, end, part);

                if (backgroundSpans.contains(span)) {
                    word.text = word.text.replace("(", "").replace(")", "");
                    if (!word.text.isEmpty()) bgWords.add(word);
                } else {
                    mainWords.add(word);
                }
            }

            buildWordChain(sb, mainWords);

            if (!bgWords.isEmpty()) {
                sb.append("\n[bg: ");
                buildWordChain(sb, bgWords);
                sb.append("]");
            }
        }

        sb.append("\n");
    }

    private static void buildWordChain(StringBuilder sb, List<Word> words) {
        for (int i = 0; i < words.size(); i++) {
            Word w = words.get(i);
            boolean isLast = (i == words.size() - 1);

            sb.append("<").append(formatTimestamp(w.start)).append(">");
            sb.append(w.text);

            if (!w.part && !isLast) {
                sb.append(" ");
            }
            if (isLast) {
                sb.append("<").append(formatTimestamp(w.end)).append(">");
            }
        }
    }

    private static class Word {
        String text;
        long start, end;
        boolean part;

        Word(String t, long s, long e, boolean p) {
            text = t;
            start = s;
            end = e;
            part = p;
        }
    }

    private static void findBackgroundSpans(Node node, Set<Node> result, boolean isBgParent) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element e = (Element) node;
            if ("x-bg".equals(e.getAttributeNS(NS_TTM, "role"))) isBgParent = true;
            if (isBgParent && "span".equals(e.getLocalName())) result.add(e);
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            findBackgroundSpans(children.item(i), result, isBgParent);
        }
    }

    private static void findSpansRecursive(Node node, List<Element> result) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) child;
                if ("span".equals(e.getLocalName()) && e.hasAttribute("begin")) result.add(e);
                findSpansRecursive(child, result);
            }
        }
    }

    private static String formatTimestamp(long ms) {
        long minutes = (ms / 60000);
        long seconds = (ms % 60000) / 1000;
        long millis = (ms % 1000);
        return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis);
    }

    private static long parseTimestamp(String timespan) {
        if (timespan == null || timespan.isEmpty()) return 0;
        timespan = timespan.trim();
        try {
            if (timespan.endsWith("ms")) return (long) Float.parseFloat(timespan.replace("ms", ""));
            if (timespan.endsWith("s"))
                return (long) (Float.parseFloat(timespan.replace("s", "")) * 1000);
            if (timespan.endsWith("m"))
                return (long) (Float.parseFloat(timespan.replace("m", "")) * 60000);

            String[] parts = timespan.split(":");
            if (parts.length == 3) {
                return Math.round(
                        (Integer.parseInt(parts[0]) * 3600
                                        + Integer.parseInt(parts[1]) * 60
                                        + Double.parseDouble(parts[2]))
                                * 1000);
            } else if (parts.length == 2) {
                return Math.round(
                        (Integer.parseInt(parts[0]) * 60 + Double.parseDouble(parts[1])) * 1000);
            } else {
                return Math.round(Double.parseDouble(timespan) * 1000);
            }
        } catch (Exception e) {
            return 0;
        }
    }
}

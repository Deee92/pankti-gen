package se.kth.castor.panktigen;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.ls.*;

public class ObjectXMLParser {
    static List<String> receivingObjectXML = new ArrayList<>();
    static List<String> paramObjectsXML = new ArrayList<>();
    static List<String> returnedObjectXML = new ArrayList<>();

    public static InputStream addRootElementToXMLFile(File inputFile) throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(inputFile);
        List<InputStream> streams =
                Arrays.asList(
                        new ByteArrayInputStream("<root>".getBytes()),
                        fis,
                        new ByteArrayInputStream("</root>".getBytes()));
        return new SequenceInputStream(Collections.enumeration(streams));
    }

    public static void parseXML(String filePath) {
        try {
            File inputFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            InputStream wellFormedXML = addRootElementToXMLFile(inputFile);

            Document doc = dBuilder.parse(wellFormedXML);

            DOMImplementationLS ls = (DOMImplementationLS) doc.getImplementation();
            LSSerializer ser = ls.createLSSerializer();

            doc.getDocumentElement().normalize();
            String documentRoot = doc.getDocumentElement().getNodeName();
            System.out.println("Root element:" + documentRoot);
            NodeList childNodes = doc.getDocumentElement().getChildNodes();
            System.out.println("Number of object nodes: " + childNodes.getLength());

            for (int i = 0; i < childNodes.getLength(); i++) {
                Node thisNode = childNodes.item(i);
                String rawXMLForObject = ser.writeToString(thisNode);
                rawXMLForObject = rawXMLForObject.replaceAll("(\\<\\?xml version=\"1\\.0\" encoding=\"UTF-16\"\\?>)", "");
                rawXMLForObject = rawXMLForObject.replaceAll("\\s", "");
                System.out.println(rawXMLForObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

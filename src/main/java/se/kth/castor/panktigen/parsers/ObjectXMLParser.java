package se.kth.castor.panktigen.parsers;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

import org.w3c.dom.ls.*;

public class ObjectXMLParser {
    Set<SerializedObject> serializedObjects = new HashSet<>();
    private static final String receivingObjectFilePostfix = "-receiving.xml";
    private static final String paramObjectsFilePostfix = "-params.xml";
    private static final String returnedObjectFilePostfix = "-returned.xml";

    public InputStream addRootElementToXMLFile(File inputFile) throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(inputFile);
        List<InputStream> streams =
                Arrays.asList(
                        new ByteArrayInputStream("<root>".getBytes()),
                        fis,
                        new ByteArrayInputStream("</root>".getBytes()));
        return new SequenceInputStream(Collections.enumeration(streams));
    }

    public File findXMLFileByObjectType(String basePath, String type) {
        return new File(basePath + type);
    }

    public List<String> parseXMLInFile(File inputFile) throws Exception {
        List<String> rawXMLObjects = new ArrayList<>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        InputStream wellFormedXML = addRootElementToXMLFile(inputFile);

        Document doc = dBuilder.parse(wellFormedXML);

        DOMImplementationLS ls = (DOMImplementationLS) doc.getImplementation();
        LSSerializer ser = ls.createLSSerializer();

        Node rootNode = doc.getDocumentElement();
        rootNode.normalize();
        NodeList childNodes = rootNode.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node thisNode = childNodes.item(i);
            String rawXMLForObject = ser.writeToString(thisNode);
            rawXMLForObject = rawXMLForObject.replaceAll("(\\<\\?xml version=\"1\\.0\" encoding=\"UTF-16\"\\?>)", "");
            rawXMLForObject = rawXMLForObject.replaceAll("\"","\\\\\"");
            rawXMLForObject = rawXMLForObject.replaceAll("[ ]{2,}", "");
            rawXMLForObject = rawXMLForObject.replaceAll("\\n", "");
            rawXMLObjects.add(rawXMLForObject);
        }
        return rawXMLObjects;
    }

    public Set<SerializedObject> parseXML(String basePath) {
        try {
            File receivingObjectFile = findXMLFileByObjectType(basePath, receivingObjectFilePostfix);
            List<String> receivingObjects = parseXMLInFile(receivingObjectFile);
            File returnedObjectFile = findXMLFileByObjectType(basePath, returnedObjectFilePostfix);
            List<String> returnedObjects = parseXMLInFile(returnedObjectFile);
            File paramObjectsFile = findXMLFileByObjectType(basePath, paramObjectsFilePostfix);
            List<String> paramObjects = parseXMLInFile(paramObjectsFile);

            for (int i = 0; i < receivingObjects.size(); i++) {
                if (!receivingObjects.get(i).isEmpty() && !returnedObjects.get(i).isEmpty()) {
                    SerializedObject serializedObject =
                            new SerializedObject(
                                    receivingObjects.get(i),
                                    returnedObjects.get(i),
                                    paramObjects.get(i));
                    serializedObjects.add(serializedObject);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serializedObjects;
    }
}

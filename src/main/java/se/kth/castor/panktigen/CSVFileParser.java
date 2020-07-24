package se.kth.castor.panktigen;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;

public class CSVFileParser {
    public static void parseCSVFile(String filePath) {
        try {
            Reader in = new FileReader(filePath);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
            for (CSVRecord record : records) {
                String parentFQN = record.get("parent-FQN");
                String methodName = record.get("method-name");
                String paramList = record.get("param-list");
                String returnType = record.get("return-type");
                if (parentFQN.equals("org.apache.fontbox.cmap.CodespaceRange") && (methodName.equals("isFullMatch"))) {
                    System.out.println(record);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

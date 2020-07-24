package se.kth.castor.panktigen.parsers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.*;

public class CSVFileParser {
    public static List<InstrumentedMethod> parseCSVFile(String filePath) {
        List<InstrumentedMethod> instrumentedMethods = new ArrayList<>();
        try {
            Reader in = new FileReader(filePath);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
            for (CSVRecord record : records) {
                String parentFQN = record.get("parent-FQN");
                String methodName = record.get("method-name");
                String params = record.get("param-list");
                params = params.replace("[", "");
                params = params.replace("]", "");
                List<String> paramList = new ArrayList<>();
                if (!params.isEmpty()) {
                    paramList = new ArrayList<>(Arrays.asList(params.split(",")));
                }
                String returnType = record.get("return-type");
                instrumentedMethods.add(new InstrumentedMethod(parentFQN, methodName, paramList, returnType));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(instrumentedMethods);
        return instrumentedMethods;
    }
}

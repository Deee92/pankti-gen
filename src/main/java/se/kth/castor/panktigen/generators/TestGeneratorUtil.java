package se.kth.castor.panktigen.generators;

import spoon.reflect.code.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TestGeneratorUtil {
    public CtLocalVariable<String> addStringVariableToTestMethod(Factory factory, String fieldName, String fieldValue) {
        CtExpression<String> variableExpression = factory.createCodeSnippetExpression(
                "\"" + fieldValue + "\""
        );
        return factory.createLocalVariable(
                factory.createCtTypeReference(String.class),
                fieldName,
                variableExpression
        );
    }

    public CtLocalVariable<?> addClassLoaderVariableToTestMethod(Factory factory) {
        CtExpression<String> variableExpression = factory.createCodeSnippetExpression(
                "getClass().getClassLoader()"
        );
        return factory.createLocalVariable(
                factory.createCtTypeReference(ClassLoader.class),
                "classLoader",
                variableExpression
        );
    }

    public List<CtStatement> addScannerVariableToTestMethod(Factory factory, String fileName) {
        List<CtStatement> fileAndScannerStatements = new ArrayList<>();
        // Create file
        CtExpression<String> fileVariableExpression = factory.createCodeSnippetExpression(
                "new File(classLoader.getResource(\"" + fileName + "\").getFile())"
        );
        CtLocalVariable<?> fileVariable = factory.createLocalVariable(
                factory.createCtTypeReference(File.class),
                "file",
                fileVariableExpression);
        // Create scanner
        CtExpression<String> scannerVariableExpression = factory.createCodeSnippetExpression(
                "new Scanner(file)"
        );
        CtLocalVariable<?> scannerVariable =  factory.createLocalVariable(
                factory.createCtTypeReference(Scanner.class),
                "scanner",
                scannerVariableExpression
        );
        fileAndScannerStatements.add(fileVariable);
        fileAndScannerStatements.add(scannerVariable);
        return fileAndScannerStatements;
    }

    public CtLocalVariable<String> readStringFromScanner(Factory factory) {
        CtExpression<String> variableExpression = factory.createCodeSnippetExpression(
                "scanner.useDelimiter(\"\\\\A\").next()"
        );
        return factory.createLocalVariable(
                factory.createCtTypeReference(String.class),
                "receivingXML",
                variableExpression
        );
    }

    public String findObjectBoxType(CtTypeReference typeReference) {
        if (typeReference.isPrimitive())
            return typeReference.box().getSimpleName();
        else return typeReference.getQualifiedName().replaceAll("\\$", ".");
    }
}

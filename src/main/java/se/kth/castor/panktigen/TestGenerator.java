package se.kth.castor.panktigen;

import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;

import java.util.*;

public class TestGenerator {
    List<CtType<?>> classTypes = new ArrayList<>();
    private static Factory factory;
    private static final String X_STREAM_REFERENCE = "com.thoughtworks.xstream.XStream";
    private static final String JUNIT_REFERENCE = "org.junit.Test";
    private static final String JUNIT_ASSERT_REFERENCE = "org.junit.Assert";
    private static final String TEST_CLASS_PREFIX = "Test";
    private static final String TEST_CLASS_POSTFIX = "PanktiGen";

    public CtClass<?> generateTestClass(CtPackage ctPackage, String className) {
        CtClass<?> generatedClass = factory.createClass(ctPackage, TEST_CLASS_PREFIX + className + TEST_CLASS_POSTFIX);
        generatedClass.addModifier(ModifierKind.PUBLIC);
        return generatedClass;
    }

    public void addImportsToGeneratedClass(CtClass<?> generatedClass) {
        generatedClass.getFactory().createUnresolvedImport(X_STREAM_REFERENCE, false);
        generatedClass.getFactory().createUnresolvedImport(JUNIT_REFERENCE, false);
        generatedClass.getFactory().createUnresolvedImport(JUNIT_ASSERT_REFERENCE, false);
    }

    public CtField<?> addXStreamFieldToGeneratedClass() throws ClassNotFoundException {
        String xStreamFieldValue = "new XStream()";
        CtField<?> xStreamField = factory.createCtField(
                "xStream",
                factory.createCtTypeReference(Class.forName(X_STREAM_REFERENCE)),
                xStreamFieldValue
        );
        xStreamField.addModifier(ModifierKind.STATIC);
        return xStreamField;
    }

    public CtField<?> addStringFieldToGeneratedClass(String fieldName, String fieldValue) {
        CtField<String> generatedField = factory.createCtField(
                fieldName,
                factory.createCtTypeReference(String.class),
                fieldValue.replaceAll("\\s", ""));
        generatedField.addModifier(ModifierKind.STATIC);
        return generatedField;
    }

    public CtLocalVariable<String> addStringVariableToTestMethod(String fieldName, String fieldValue) {
        CtExpression<String> variableExpression = factory.createCodeSnippetExpression(fieldValue.replaceAll("\\s", ""));
        return factory.createLocalVariable(
                factory.createCtTypeReference(String.class),
                fieldName,
                variableExpression
        );
    }

    @SuppressWarnings("unchecked")
    public CtInvocation<?> generateAssertionInTestMethod() throws ClassNotFoundException {
        CtExpression<?> assertEqualsExpected = factory.createCodeSnippetExpression("returnedObject");
        CtExpression<?> assertEqualsActual = factory.createCodeSnippetExpression("receivingObject.isFullMatch(paramObject1, paramObject2)");

        CtExecutableReference<?> executableReference = factory.createExecutableReference();
        executableReference.setStatic(true);
        executableReference.setDeclaringType(factory.createCtTypeReference(Class.forName(JUNIT_ASSERT_REFERENCE)));
        executableReference.setSimpleName("assertEquals");
        System.out.println("executableReference: " + executableReference);
        CtInvocation assertEqualsInvocation = factory.createInvocation();
        assertEqualsInvocation.setExecutable(executableReference);
        assertEqualsInvocation.setTarget(factory.createTypeAccess(factory.createCtTypeReference(Class.forName(JUNIT_ASSERT_REFERENCE))));
        assertEqualsInvocation.setArguments(Arrays.asList(assertEqualsExpected, assertEqualsActual));
        System.out.println(assertEqualsInvocation);
        return assertEqualsInvocation;
    }

    public CtMethod<?> generateTestMethod(String methodName) throws ClassNotFoundException {
        CtMethod<?> generatedMethod = factory.createMethod();
        generatedMethod.setSimpleName("test" + methodName.substring(0, 1).toUpperCase() + methodName.substring(1));
        CtAnnotation<?> testAnnotation = factory.createAnnotation(factory.createCtTypeReference(Class.forName(JUNIT_REFERENCE)));
        generatedMethod.addAnnotation(testAnnotation);
        generatedMethod.setModifiers(Collections.singleton(ModifierKind.PUBLIC));
        generatedMethod.setType(factory.createCtTypeReference(void.class));
        CtStatement generatedStatement1 = factory.createCodeSnippetStatement(
                String.format(
                        "%s receivingObject = (%s) xStream.fromXML(receivingXML)",
                        "org.apache.fontbox.cmap.CodespaceRange",
                        "org.apache.fontbox.cmap.CodespaceRange"));
        CtStatement generatedStatement2 = factory.createCodeSnippetStatement(
                String.format(
                        "%s returnedObject = (%s) xStream.fromXML(returnedXML)",
                        "boolean",
                        "Boolean"));
        CtStatement generatedStatement3 = factory.createCodeSnippetStatement(
                String.format(
                        "%s paramObjects = (%s) xStream.fromXML(paramsXML)",
                        "Object[]",
                        "Object[]"));
        CtStatement generatedStatement4 = factory.createCodeSnippetStatement(
                String.format(
                        "%s paramObject1 = (%s) paramObjects[0]",
                        "byte[]",
                        "Byte[]"));
        CtStatement generatedStatement5 = factory.createCodeSnippetStatement(
                String.format(
                        "%s paramObject2 = (%s) paramObjects[1]",
                        "int",
                        "Integer"));

        CtBlock<?> methodBody = factory.createBlock();
        methodBody.addStatement(generatedStatement1);
        methodBody.addStatement(generatedStatement2);
        methodBody.addStatement(generatedStatement3);
        methodBody.addStatement(generatedStatement4);
        methodBody.addStatement(generatedStatement5);

        generatedMethod.setBody(methodBody);

        CtInvocation<?> assertEqualsInvocation = generateAssertionInTestMethod();
        generatedMethod.getBody().addStatement(assertEqualsInvocation);

        return generatedMethod;
    }

    public CtClass<?> generateFullTestClass(CtType<?> type, CtMethod<?> method) {
        factory = type.getFactory();
        CtClass<?> generatedClass = generateTestClass(type.getPackage(), type.getSimpleName());
        addImportsToGeneratedClass(generatedClass);

        String receivingXML = "\"" +
                "<org.apache.fontbox.cmap.CodespaceRange>\n" +
                "  <start>\n" +
                "    <int>0</int>\n" +
                "    <int>0</int>\n" +
                "  </start>\n" +
                "  <end>\n" +
                "    <int>255</int>\n" +
                "    <int>255</int>\n" +
                "  </end>\n" +
                "  <codeLength>2</codeLength>\n" +
                "</org.apache.fontbox.cmap.CodespaceRange>" +
                "\"";
        String paramsXML = "\"" +
                "<object-array>\n" +
                "  <byte-array>ACs=</byte-array>\n" +
                "  <int>2</int>\n" +
                "</object-array>"
                + "\"";
        String returnedXML = "\"" +
                "<boolean>true</boolean>"
                + "\"";

        // Add XStream field to class
        try {
            generatedClass.addField(addXStreamFieldToGeneratedClass());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        // Add String fields for object XML
//        generatedClass.addField(addStringFieldToGeneratedClass("receivingXML", receivingXML));
//        generatedClass.addField(addStringFieldToGeneratedClass("paramsXML", paramsXML));
//        generatedClass.addField(addStringFieldToGeneratedClass("returnedXML", returnedXML));
        // Create @Test method
        try {
            CtMethod<?> generatedMethod = generateTestMethod(method.getSimpleName());
            generatedMethod.getBody().addStatement(addStringVariableToTestMethod("receivingXML", receivingXML));
            generatedMethod.getBody().addStatement(addStringVariableToTestMethod("paramsXML", paramsXML));
            generatedMethod.getBody().addStatement(addStringVariableToTestMethod("returnedXML", returnedXML));
            generatedClass.addMethod(generatedMethod);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return generatedClass;
    }

    public List<CtType<?>> getNonAbstractClassTypes(CtModel ctModel) {
        List<CtType<?>> types = (List<CtType<?>>) ctModel.getAllTypes();
        System.out.println(types.size());
        for (CtType<?> type : types) {
            if (type.isClass() && !type.isAbstract()) {
                String fqn = type.getPackage() + "." + type.getSimpleName();
                if (fqn.equals("org.apache.fontbox.cmap.CodespaceRange")) {
                    CtMethod<?> method = type.getMethodsByName("isFullMatch").get(0);
                    System.out.println("Generating test method for: " + method.getPath());
                    CtClass<?> generatedClass = generateFullTestClass(type, method);
                    System.out.println("Generated test class: " + generatedClass.getQualifiedName());
                }
                classTypes.add(type);
            }
        }
        CSVFileParser.parseCSVFile("/home/user/two-methods.csv");
        ObjectXMLParser.parseXML("/home/user/pdfbox-object-data/17-pdfbox-receiving.xml");
        ObjectXMLParser.parseXML("/home/user/pdfbox-object-data/17-pdfbox-params.xml");
        ObjectXMLParser.parseXML("/home/user/pdfbox-object-data/17-pdfbox-returned.xml");
        return classTypes;
    }
}

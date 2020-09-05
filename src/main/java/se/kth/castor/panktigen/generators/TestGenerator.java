package se.kth.castor.panktigen.generators;

import se.kth.castor.panktigen.parsers.*;
import spoon.MavenLauncher;
import spoon.compiler.SpoonResource;
import spoon.compiler.SpoonResourceHelper;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.support.reflect.code.CtTryImpl;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

public class TestGenerator {
    private static Factory factory;
    private static final String XSTREAM_REFERENCE = "com.thoughtworks.xstream.XStream";
    private static final String XSTREAM_CONSTRUCTOR = "new XStream()";
    private static final String JUNIT_REFERENCE = "org.junit.Test";
    private static final String JUNIT_ASSERT_REFERENCE = "org.junit.Assert";
    private static final String JAVA_UTIL_ARRAYS_REFERENCE = "java.util.Arrays";
    private static final String JAVA_UTIL_SCANNER_REFERENCE = "java.util.Scanner";
    private static final String JAVA_IO_FILE_REFERENCE = "java.io.File";

    private static final String TEST_CLASS_PREFIX = "Test";
    private static final String TEST_CLASS_POSTFIX = "PanktiGen";
    private static int numberOfTestCasesGenerated;

    private final TestGeneratorUtil testGenUtil = new TestGeneratorUtil();

    public String getGeneratedClassName(CtPackage ctPackage, String className) {
        return String.format("%s.%s%s%s", ctPackage, TEST_CLASS_PREFIX, className, TEST_CLASS_POSTFIX);
    }

    public CtClass<?> generateTestClass(CtPackage ctPackage, String className) {
        CtClass<?> generatedClass = factory.createClass(ctPackage, TEST_CLASS_PREFIX + className + TEST_CLASS_POSTFIX);
        generatedClass.addModifier(ModifierKind.PUBLIC);
        return generatedClass;
    }

    public void addImportsToGeneratedClass(CtClass<?> generatedClass) {
        generatedClass.getFactory().createUnresolvedImport(XSTREAM_REFERENCE, false);
        generatedClass.getFactory().createUnresolvedImport(JUNIT_REFERENCE, false);
        generatedClass.getFactory().createUnresolvedImport(JUNIT_ASSERT_REFERENCE, false);
        generatedClass.getFactory().createUnresolvedImport(JAVA_UTIL_ARRAYS_REFERENCE, false);
        generatedClass.getFactory().createUnresolvedImport(JAVA_UTIL_SCANNER_REFERENCE, false);
        generatedClass.getFactory().createUnresolvedImport(JAVA_IO_FILE_REFERENCE, false);
    }

    public CtField<?> addXStreamFieldToGeneratedClass() throws ClassNotFoundException {
        CtField<?> xStreamField = factory.createCtField(
                "xStream",
                factory.createCtTypeReference(Class.forName(XSTREAM_REFERENCE)),
                XSTREAM_CONSTRUCTOR
        );
        xStreamField.addModifier(ModifierKind.STATIC);
        return xStreamField;
    }

    @SuppressWarnings("unchecked")
    public CtInvocation<?> generateAssertionInTestMethod(CtMethod<?> method) throws ClassNotFoundException {
        CtExpression<?> assertExpectedObject = factory.createCodeSnippetExpression("returnedObject");

        StringBuilder arguments = new StringBuilder();
        for (int i = 1; i <= method.getParameters().size(); i++) {
            arguments.append("paramObject").append(i);
            if (i != method.getParameters().size()) {
                arguments.append(", ");
            }
        }

        CtExpression<?> assertActualObject = factory.createCodeSnippetExpression(
                String.format("receivingObject.%s(%s)",
                        method.getSimpleName(),
                        arguments.toString()));

        CtExecutableReference<?> executableReferenceForAssertion = factory.createExecutableReference();
        executableReferenceForAssertion.setStatic(true);
        executableReferenceForAssertion.setDeclaringType(factory.createCtTypeReference(Class.forName(JUNIT_ASSERT_REFERENCE)));
        CtInvocation assertInvocation = factory.createInvocation();

        if (method.getType().isArray()) {
            // if method returns an array, Assert.assertTrue(Arrays.equals(expected, actual))
            executableReferenceForAssertion.setSimpleName("assertTrue");
            assertInvocation.setExecutable(executableReferenceForAssertion);
            assertInvocation.setTarget(factory.createTypeAccess(factory.createCtTypeReference(Class.forName(JUNIT_ASSERT_REFERENCE))));
            CtInvocation arraysEqualsInvocation = factory.createInvocation();
            CtExecutableReference<?> executableReferenceForArraysEquals = factory.createExecutableReference();
            executableReferenceForArraysEquals.setStatic(true);
            executableReferenceForArraysEquals.setDeclaringType(factory.createCtTypeReference(Class.forName(JAVA_UTIL_ARRAYS_REFERENCE)));
            executableReferenceForArraysEquals.setSimpleName("equals");
            arraysEqualsInvocation.setExecutable(executableReferenceForArraysEquals);
            arraysEqualsInvocation.setTarget(factory.createTypeAccess(factory.createCtTypeReference(Class.forName(JAVA_UTIL_ARRAYS_REFERENCE))));
            arraysEqualsInvocation.setArguments(Arrays.asList(assertExpectedObject, assertActualObject));
            assertInvocation.setArguments(Collections.singletonList(arraysEqualsInvocation));
        } else {
            // Assert.assertEquals(expected, actual)
            executableReferenceForAssertion.setSimpleName("assertEquals");
            assertInvocation.setExecutable(executableReferenceForAssertion);
            assertInvocation.setTarget(factory.createTypeAccess(factory.createCtTypeReference(Class.forName(JUNIT_ASSERT_REFERENCE))));
            assertInvocation.setArguments(Arrays.asList(assertExpectedObject, assertActualObject));
        }
        return assertInvocation;
    }

    public String createLongXMLStringFile(String methodIdentifier, String longXML, MavenLauncher launcher) {
        String fileName = "";
        try {
            File longXMLFile = new File("./tmp/object-data/" + methodIdentifier + ".txt");
            longXMLFile.getParentFile().mkdirs();
            FileWriter myWriter = new FileWriter(longXMLFile);
            myWriter.write(longXML.replaceAll("\\\\\"", "\""));
            myWriter.close();
            SpoonResource newResource = SpoonResourceHelper.createResource(longXMLFile);
            launcher.addInputResource(longXMLFile.getAbsolutePath());
            fileName = newResource.getName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }

    public CtTryImpl createTryBlockToReadXML(String fileName) {
        CtTryImpl tryBlock = (CtTryImpl) factory.createTry();
        CtStatement classLoaderDeclaration = testGenUtil.addClassLoaderVariableToTestMethod(factory);
        List<CtStatement> scannerDeclaration = testGenUtil.addScannerVariableToTestMethod(factory, fileName);
        CtStatement stringReadFromScanner = testGenUtil.readStringFromScanner(factory);
        CtBlock<?> tryBody = factory.createBlock();
        tryBody.addStatement(classLoaderDeclaration);
        scannerDeclaration.forEach(tryBody::addStatement);
        tryBody.addStatement(stringReadFromScanner);
        tryBlock.setBody(tryBody);
        return tryBlock;
    }

    public CtStatement parseReceivingObject(String receivingObjectType) {
        return factory.createCodeSnippetStatement(String.format(
                "%s receivingObject = (%s) xStream.fromXML(receivingXML)",
                receivingObjectType,
                receivingObjectType));
    }

    public CtStatement parseReturnedObject(String returnedObjectType, CtMethod<?> method) {
        return factory.createCodeSnippetStatement(String.format(
                "%s returnedObject = (%s) xStream.fromXML(returnedXML)",
                returnedObjectType,
                testGenUtil.findObjectBoxType(method.getType())));
    }

    public List<CtStatement> addAndParseMethodParams(String paramsXML, CtMethod<?> method) {
        List<CtStatement> paramStatements = new ArrayList<>();
        CtStatement paramsXMLStringDeclaration = testGenUtil.addStringVariableToTestMethod(factory, "paramsXML", paramsXML);

        CtStatement parseParamObjects = factory.createCodeSnippetStatement(
                String.format(
                        "%s paramObjects = (%s) xStream.fromXML(paramsXML)",
                        "Object[]",
                        "Object[]"));

        paramStatements.add(paramsXMLStringDeclaration);
        paramStatements.add(parseParamObjects);

        List<CtParameter<?>> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            CtStatement parseParamObject = factory.createCodeSnippetStatement(
                    String.format("%s paramObject%d = (%s) paramObjects[%d]",
                            parameters.get(i).getType().getQualifiedName(),
                            i + 1,
                            testGenUtil.findObjectBoxType(parameters.get(i).getType()),
                            i));
            paramStatements.add(parseParamObject);
        }
        return paramStatements;
    }

    public CtMethod<?> generateTestMethod(CtMethod<?> method,
                                          int methodCounter,
                                          InstrumentedMethod instrumentedMethod,
                                          SerializedObject serializedObject,
                                          MavenLauncher launcher) throws ClassNotFoundException {
        CtMethod<?> generatedMethod = factory.createMethod();
        generatedMethod.setSimpleName("test" + method.getSimpleName().substring(0, 1).toUpperCase() + method.getSimpleName().substring(1) + methodCounter);
        CtAnnotation<?> testAnnotation = factory.createAnnotation(factory.createCtTypeReference(Class.forName(JUNIT_REFERENCE)));
        generatedMethod.addAnnotation(testAnnotation);
        generatedMethod.setModifiers(Collections.singleton(ModifierKind.PUBLIC));
        generatedMethod.setType(factory.createCtTypeReference(void.class));

        // Get serialized objects as XML strings
        String receivingXML = serializedObject.getReceivingObject();
        String receivingObjectType = serializedObject.getObjectType(receivingXML);
        String returnedXML = serializedObject.getReturnedObject();
        String returnedObjectType = instrumentedMethod.getReturnType();

        CtBlock<?> methodBody = factory.createBlock();

        // If receivingXML string is too long, read string from a resource file
        if (receivingXML.length() > 10000) {
            String methodIdentifier = instrumentedMethod.getFullMethodPath() + methodCounter;
            String fileName = createLongXMLStringFile(methodIdentifier, receivingXML, launcher);
            CtTryImpl tryBlock = createTryBlockToReadXML(fileName);
            CtBlock<?> tryBody = tryBlock.getBody();

            CtStatement returnedXMLStringDeclaration = testGenUtil.addStringVariableToTestMethod(factory, "returnedXML", returnedXML);
            CtStatement parseReceivingObject = parseReceivingObject(receivingObjectType);
            CtStatement parseReturnedObject = parseReturnedObject(returnedObjectType, method);

            tryBody.addStatement(returnedXMLStringDeclaration);
            tryBody.addStatement(parseReceivingObject);
            tryBody.addStatement(parseReturnedObject);

            if (instrumentedMethod.hasParams()) {
                String paramsXML = serializedObject.getParamObjects();
                List<CtStatement> paramStatements = addAndParseMethodParams(paramsXML, method);
                paramStatements.forEach(tryBody::addStatement);
            }
            CtInvocation<?> assertionInvocation = generateAssertionInTestMethod(method);
            tryBody.addStatement(assertionInvocation);
            tryBlock.setBody(tryBody);
            CtBlock<?> catchBlock = factory.createBlock();
            CtStatement failAssertionStatement = factory.createCodeSnippetStatement("Assert.fail()");
            CtStatement stackTraceStatement = factory.createCodeSnippetStatement("e.printStackTrace()");
            catchBlock.addStatement(failAssertionStatement);
            catchBlock.addStatement(stackTraceStatement);
            tryBlock.addCatcher(factory.createCtCatch("e", Exception.class, catchBlock));
            methodBody.addStatement(tryBlock);
        } else {
            CtStatement receivingXMLStringDeclaration = testGenUtil.addStringVariableToTestMethod(factory, "receivingXML", receivingXML);
            CtStatement returnedXMLStringDeclaration = testGenUtil.addStringVariableToTestMethod(factory, "returnedXML", returnedXML);
            methodBody.addStatement(receivingXMLStringDeclaration);
            methodBody.addStatement(returnedXMLStringDeclaration);
            methodBody.addStatement(parseReceivingObject(receivingObjectType));
            methodBody.addStatement(parseReturnedObject(returnedObjectType, method));
            if (instrumentedMethod.hasParams()) {
                String paramsXML = serializedObject.getParamObjects();
                List<CtStatement> paramStatements = addAndParseMethodParams(paramsXML, method);
                paramStatements.forEach(methodBody::addStatement);
            }
            CtInvocation<?> assertionInvocation = generateAssertionInTestMethod(method);
            methodBody.addStatement(assertionInvocation);
        }

        generatedMethod.setBody(methodBody);
        return generatedMethod;
    }

    public CtClass<?> generateFullTestClass(CtType<?> type,
                                            CtMethod<?> method,
                                            InstrumentedMethod instrumentedMethod,
                                            MavenLauncher launcher) throws ClassNotFoundException {
        factory = type.getFactory();
        CtClass<?> generatedClass = factory.Class().get(getGeneratedClassName(type.getPackage(), type.getSimpleName()));
        if (generatedClass == null) {
            generatedClass = generateTestClass(type.getPackage(), type.getSimpleName());
            addImportsToGeneratedClass(generatedClass);
            generatedClass.addField(addXStreamFieldToGeneratedClass());
        }
        String methodPath = instrumentedMethod.getFullMethodPath();
        ObjectXMLParser objectXMLParser = new ObjectXMLParser();
        Set<SerializedObject> serializedObjects = objectXMLParser.parseXML("/home/user/object-data/" + methodPath, instrumentedMethod.hasParams());
        System.out.println("Number of unique pairs/triples of object values: " + serializedObjects.size());
        numberOfTestCasesGenerated += serializedObjects.size();

        // Create @Test method
        int methodCounter = 1;
        for (SerializedObject serializedObject : serializedObjects) {
            CtMethod<?> generatedMethod = generateTestMethod(method, methodCounter, instrumentedMethod, serializedObject, launcher);
            generatedClass.addMethod(generatedMethod);
            methodCounter++;
        }
        return generatedClass;
    }

    public List<CtType<?>> getTypesToProcess(CtModel ctModel) {
        return ctModel.getAllTypes().
                stream().
                filter(CtType::isClass).
                collect(Collectors.toList());
    }

    private CtMethod<?> findMethodToGenerateTestMethodsFor(List<CtMethod<?>> methodsByName, InstrumentedMethod instrumentedMethod) {
        if (methodsByName.size() > 1) {
            // match parameter list for overloaded methods
            for (CtMethod<?> method : methodsByName) {
                List<String> paramTypes = method.getParameters().stream().
                        map(parameter -> parameter.getType().getQualifiedName()).
                        collect(Collectors.toList());
                if (Arrays.equals(paramTypes.toArray(), instrumentedMethod.getParamList().toArray())) {
                    System.out.println("matched params: " + paramTypes);
                    return method;
                }
            }
        }
        return methodsByName.get(0);
    }

    public int process(CtModel ctModel, MavenLauncher launcher) {
        // Get list of instrumented methods from CSV file
        List<InstrumentedMethod> instrumentedMethods = CSVFileParser.parseCSVFile("/home/user/two-methods.csv");
        System.out.println("Number of instrumented methods: " + instrumentedMethods.size());
        List<CtType<?>> types = getTypesToProcess(ctModel);

        for (CtType<?> type : types) {
            for (InstrumentedMethod instrumentedMethod : instrumentedMethods) {
                if (type.getQualifiedName().equals(instrumentedMethod.getParentFQN())) {
                    List<CtMethod<?>> methodsByName = type.getMethodsByName(instrumentedMethod.getMethodName());
                    if (methodsByName.size() > 0) {
                        CtMethod<?> methodToGenerateTestsFor = findMethodToGenerateTestMethodsFor(methodsByName, instrumentedMethod);
                        System.out.println("Generating test method for: " + methodToGenerateTestsFor.getPath());
                        try {
                            CtClass<?> generatedClass = generateFullTestClass(type, methodToGenerateTestsFor, instrumentedMethod, launcher);
                            System.out.println("Generated test class: " + generatedClass.getQualifiedName());
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return numberOfTestCasesGenerated;
    }
}

package se.kth.castor.panktigen.generators;

import se.kth.castor.panktigen.parsers.CSVFileParser;
import se.kth.castor.panktigen.parsers.InstrumentedMethod;
import se.kth.castor.panktigen.parsers.ObjectXMLParser;
import se.kth.castor.panktigen.parsers.SerializedObject;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.*;

public class TestGenerator {
    private static Factory factory;
    private static final String XSTREAM_REFERENCE = "com.thoughtworks.xstream.XStream";
    private static final String XSTREAM_CONSTRUCTOR = "new XStream()";
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
        generatedClass.getFactory().createUnresolvedImport(XSTREAM_REFERENCE, false);
        generatedClass.getFactory().createUnresolvedImport(JUNIT_REFERENCE, false);
        generatedClass.getFactory().createUnresolvedImport(JUNIT_ASSERT_REFERENCE, false);
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

    public CtLocalVariable<String> addStringVariableToTestMethod(String fieldName, String fieldValue) {
        CtExpression<String> variableExpression = factory.createCodeSnippetExpression("\"" + fieldValue + "\"");
        return factory.createLocalVariable(
                factory.createCtTypeReference(String.class),
                fieldName,
                variableExpression
        );
    }

    public String findObjectBoxType(CtTypeReference typeReference) {
        if (typeReference.isPrimitive())
            return typeReference.box().getSimpleName();
        else return typeReference.getQualifiedName();
    }

    @SuppressWarnings("unchecked")
    public CtInvocation<?> generateAssertionInTestMethod() throws ClassNotFoundException {
        CtExpression<?> assertEqualsExpected = factory.createCodeSnippetExpression("returnedObject");
        CtExpression<?> assertEqualsActual = factory.createCodeSnippetExpression("receivingObject.isFullMatch(paramObject1, paramObject2)");

        CtExecutableReference<?> executableReference = factory.createExecutableReference();
        executableReference.setStatic(true);
        executableReference.setDeclaringType(factory.createCtTypeReference(Class.forName(JUNIT_ASSERT_REFERENCE)));
        executableReference.setSimpleName("assertEquals");
        // System.out.println("executableReference: " + executableReference);
        CtInvocation assertEqualsInvocation = factory.createInvocation();
        assertEqualsInvocation.setExecutable(executableReference);
        assertEqualsInvocation.setTarget(factory.createTypeAccess(factory.createCtTypeReference(Class.forName(JUNIT_ASSERT_REFERENCE))));
        assertEqualsInvocation.setArguments(Arrays.asList(assertEqualsExpected, assertEqualsActual));
        // System.out.println(assertEqualsInvocation);
        return assertEqualsInvocation;
    }

    public CtMethod<?> generateTestMethod(CtMethod<?> method,
                                          int methodCounter,
                                          InstrumentedMethod instrumentedMethod,
                                          SerializedObject serializedObject) throws ClassNotFoundException {
        CtMethod<?> generatedMethod = factory.createMethod();
        generatedMethod.setSimpleName("test" + method.getSimpleName().substring(0, 1).toUpperCase() + method.getSimpleName().substring(1) + methodCounter);
        CtAnnotation<?> testAnnotation = factory.createAnnotation(factory.createCtTypeReference(Class.forName(JUNIT_REFERENCE)));
        generatedMethod.addAnnotation(testAnnotation);
        generatedMethod.setModifiers(Collections.singleton(ModifierKind.PUBLIC));
        generatedMethod.setType(factory.createCtTypeReference(void.class));

        String receivingXML = serializedObject.getReceivingObject();
        String receivingObjectType = serializedObject.getObjectType(receivingXML);
        String returnedXML = serializedObject.getReturnedObjects();
        String returnedObjectType = instrumentedMethod.getReturnType();

        CtStatement receivingXMLStringDeclaration = addStringVariableToTestMethod("receivingXML", receivingXML);
        // System.out.println(receivingXMLStringDeclaration);
        CtStatement returnedXMLStringDeclaration = addStringVariableToTestMethod("returnedXML", returnedXML);
        // System.out.println(returnedXMLStringDeclaration);
        CtStatement parseReceivingObject = factory.createCodeSnippetStatement(
                String.format(
                        "%s receivingObject = (%s) xStream.fromXML(receivingXML)",
                        receivingObjectType,
                        receivingObjectType));
        CtStatement parseReturnedObject = factory.createCodeSnippetStatement(
                String.format(
                        "%s returnedObject = (%s) xStream.fromXML(returnedXML)",
                        returnedObjectType,
                        findObjectBoxType(method.getType())));

        CtBlock<?> methodBody = factory.createBlock();
        methodBody.addStatement(receivingXMLStringDeclaration);

        methodBody.addStatement(returnedXMLStringDeclaration);
        methodBody.addStatement(parseReceivingObject);
        methodBody.addStatement(parseReturnedObject);

        if (instrumentedMethod.hasParams()) {
            String paramsXML = serializedObject.getParamObjects();
            CtStatement paramsXMLStringDeclaration = addStringVariableToTestMethod("paramsXML", paramsXML);

            CtStatement parseParamObjects = factory.createCodeSnippetStatement(
                    String.format(
                            "%s paramObjects = (%s) xStream.fromXML(paramsXML)",
                            "Object[]",
                            "Object[]"));

            methodBody.addStatement(paramsXMLStringDeclaration);
            methodBody.addStatement(parseParamObjects);

            List<CtParameter<?>> parameters = method.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                CtStatement parseParamObject = factory.createCodeSnippetStatement(
                        String.format("%s paramObject%d = (%s) paramObjects[%d]",
                                parameters.get(i).getType().getQualifiedName(),
                                i + 1,
                                findObjectBoxType(parameters.get(i).getType()),
                                i));
                methodBody.addStatement(parseParamObject);
            }
        }
        generatedMethod.setBody(methodBody);

        CtInvocation<?> assertEqualsInvocation = generateAssertionInTestMethod();
        generatedMethod.getBody().addStatement(assertEqualsInvocation);
        // System.out.println("Final method body: " + generatedMethod.getBody());

        return generatedMethod;
    }

    public CtClass<?> generateFullTestClass(CtType<?> type, CtMethod<?> method, InstrumentedMethod instrumentedMethod) {
        factory = type.getFactory();
        CtClass<?> generatedClass = generateTestClass(type.getPackage(), type.getSimpleName());
        addImportsToGeneratedClass(generatedClass);

        String methodPath = instrumentedMethod.getFullMethodPath();
        Set<SerializedObject> serializedObjects = ObjectXMLParser.parseXML("/home/user/object-data/" + methodPath);
        System.out.println("Number of unique pairs/triples of object values: " + serializedObjects.size());

        try {
            // Add XStream field to class
            generatedClass.addField(addXStreamFieldToGeneratedClass());
            // Create @Test method
            int methodCounter = 1;
            for (SerializedObject serializedObject: serializedObjects) {
                CtMethod<?> generatedMethod = generateTestMethod(method, methodCounter, instrumentedMethod, serializedObject);
                generatedClass.addMethod(generatedMethod);
                methodCounter++;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return generatedClass;
    }

    public List<CtType<?>> getTypesToProcess(CtModel ctModel) {
        List<CtType<?>> types = new ArrayList<>();
        for (CtType<?> type : ctModel.getAllTypes()) {
            if (type.isClass() && !type.isAbstract())
                types.add(type);
        }
        return types;
    }

    public void process(CtModel ctModel) {
        List<InstrumentedMethod> instrumentedMethods = CSVFileParser.parseCSVFile("/home/user/one-method.csv");
        List<CtType<?>> types = getTypesToProcess(ctModel);

        for (CtType<?> type : types) {
            String fqn = type.getPackage() + "." + type.getSimpleName();
            if (instrumentedMethods.size() > 0) {
                for (InstrumentedMethod instrumentedMethod : instrumentedMethods) {
                    if (fqn.equals(instrumentedMethod.getParentFQN())) {
                        List<CtMethod<?>> methodsByName = type.getMethodsByName(instrumentedMethod.getMethodName());
                        if (methodsByName.size() == 1) {
                            System.out.println("Generating test method for: " + methodsByName.get(0).getPath());
                            CtClass<?> generatedClass = generateFullTestClass(type, methodsByName.get(0), instrumentedMethod);
                            System.out.println("Generated test class: " + generatedClass.getQualifiedName());
                        }
                    }
                }
            }
        }
    }
}

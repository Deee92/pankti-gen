package se.kth.castor.panktigen.parsers;

import java.util.List;

public class InstrumentedMethod {
    String parentFQN;
    String methodName;
    List<String> paramList;
    String returnType;

    public InstrumentedMethod(String parentFQN, String methodName, List<String> paramList, String returnType) {
        this.parentFQN = parentFQN;
        this.methodName = methodName;
        this.paramList = paramList;
        this.returnType = returnType;
    }

    public String getParentFQN() {
        return parentFQN;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getParamList() {
        return paramList;
    }

    public String getReturnType() {
        return returnType;
    }

    public boolean hasParams() {
        return this.getParamList().size() > 0;
    }

    public String getFullMethodPath() {
        return this.parentFQN + "." + this.getMethodName();
    }

    @Override
    public String toString() {
        return "InstrumentedMethod{" +
                "parentFQN='" + parentFQN + '\'' +
                ", methodName='" + methodName + '\'' +
                ", paramList=" + paramList +
                ", returnType='" + returnType + '\'' +
                '}';
    }
}

package org.maldroid;

import java.util.HashMap;
import java.util.Map;

public class MethodCallInfo {
    private String methodName;
    private Map<String, Integer> calledMethods;

    public MethodCallInfo(String methodName) {
        this.methodName = methodName;
        this.calledMethods = new HashMap<>();
    }

    public String getMethodName() { return methodName; }
    public Map<String, Integer> getCalledMethods() { return calledMethods; }

    public void addCalledMethod(String calledMethod) {
        this.calledMethods.put(calledMethod, this.calledMethods.getOrDefault(calledMethod, 0) + 1);
    }

    @Override
    public String toString() {
        return "MethodCallInfo{methodName='" + methodName + "', calledMethods=" + calledMethods + '}';
    }
}

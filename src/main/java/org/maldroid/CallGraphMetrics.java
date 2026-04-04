package org.maldroid;

import java.util.*;
import soot.*;
import soot.jimple.*;

public class CallGraphMetrics {

    public static double[] calculateCoupling(List<MethodCallInfo> methodCallInfos) {
        Map<String, Set<String>> callGraph = new HashMap<>();
        for (MethodCallInfo info : methodCallInfos) {
            String methodName = info.getMethodName();
            callGraph.putIfAbsent(methodName, new HashSet<>());
            callGraph.get(methodName).addAll(info.getCalledMethods().keySet());
        }

        Map<String, Integer> directCoupling = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : callGraph.entrySet())
            directCoupling.put(entry.getKey(), entry.getValue().size());

        Map<String, Integer> indirectCoupling = new HashMap<>();
        for (String method : callGraph.keySet()) {
            Set<String> visited = new HashSet<>();
            calculateTransitiveCoupling(callGraph, method, visited);
            indirectCoupling.put(method, visited.size() - 1);
        }

        int totalMethods = directCoupling.size();
        double avgDirect = totalMethods == 0 ? 0 :
                (double) directCoupling.values().stream().mapToInt(Integer::intValue).sum() / totalMethods;
        double avgIndirect = totalMethods == 0 ? 0 :
                (double) indirectCoupling.values().stream().mapToInt(Integer::intValue).sum() / totalMethods;

        System.out.printf("Overall Direct Coupling: %.2f%n", avgDirect);
        System.out.printf("Overall Indirect Coupling: %.2f%n", avgIndirect);
        return new double[]{avgDirect, avgIndirect};
    }

    private static void calculateTransitiveCoupling(Map<String, Set<String>> callGraph, String method, Set<String> visited) {
        if (visited.contains(method)) return;
        visited.add(method);
        Set<String> calledMethods = callGraph.get(method);
        if (calledMethods != null)
            for (String called : calledMethods)
                calculateTransitiveCoupling(callGraph, called, visited);
    }

    public static double calculateCohesion(List<MethodCallInfo> methodCallInfos) {
        Map<String, Integer> directCalls = new HashMap<>();
        Map<String, Integer> totalCalls = new HashMap<>();
        for (MethodCallInfo info : methodCallInfos) {
            String methodName = info.getMethodName();
            Map<String, Integer> called = info.getCalledMethods();
            directCalls.put(methodName, called.size());
            totalCalls.put(methodName, called.values().stream().mapToInt(Integer::intValue).sum());
        }

        double totalDensity = 0;
        int count = directCalls.size();
        for (String method : directCalls.keySet()) {
            int d = directCalls.getOrDefault(method, 0);
            int t = totalCalls.getOrDefault(method, 0);
            totalDensity += t == 0 ? 0 : (double) d / t;
        }
        double avg = count == 0 ? 0 : totalDensity / count;
        System.out.printf("Overall Cohesion: %.2f%n", avg);
        return avg;
    }

    public static AnalyzeResult analyzeConstantsAndVariables(SootMethod method) {
        Map<String, String> constants = new HashMap<>();
        Map<String, String> variables = new HashMap<>();
        boolean hasActiveBody = false;
        try {
            if (method.hasActiveBody()) {
                hasActiveBody = true;
                Body body = method.retrieveActiveBody();
                if (body instanceof JimpleBody) {
                    for (Unit stmt : ((JimpleBody) body).getUnits()) {
                        if (stmt instanceof AssignStmt) {
                            AssignStmt assign = (AssignStmt) stmt;
                            Value right = assign.getRightOp();
                            Value left = assign.getLeftOp();
                            if (right instanceof IntConstant || right instanceof StringConstant)
                                constants.put(left.toString(), right.toString());
                            if (left instanceof Local)
                                variables.put(left.toString(), right.toString());
                        } else if (stmt instanceof StaticFieldRef) {
                            variables.put(stmt.toString(), "Static Field");
                        } else if (stmt instanceof FieldRef) {
                            variables.put(stmt.toString(), "Field");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing method " + method + ": " + e.getMessage());
        }
        return new AnalyzeResult(constants, variables, hasActiveBody);
    }
}

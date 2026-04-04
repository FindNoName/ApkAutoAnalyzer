package org.maldroid;

import soot.SootClass;
import soot.SootMethod;
import java.util.HashMap;
import java.util.Map;

public class StatsCallInfo {
    private SootClass sootClass;
    private Map<SootMethod, AnalyzeResult> methodResults;

    public StatsCallInfo(SootClass sootClass) {
        this.sootClass = sootClass;
        this.methodResults = new HashMap<>();
    }

    public SootClass getSootClass() { return sootClass; }

    public void addMethodResult(SootMethod method, AnalyzeResult result) {
        methodResults.put(method, result);
    }

    public Map<SootMethod, AnalyzeResult> getMethodResults() { return methodResults; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Class: ").append(sootClass.getName()).append("\n");
        for (Map.Entry<SootMethod, AnalyzeResult> e : methodResults.entrySet())
            sb.append("  Method: ").append(e.getKey().getSignature()).append("\n")
              .append("    ").append(e.getValue()).append("\n");
        return sb.toString();
    }
}

package org.maldroid;

public class CallGraphFeatureResult {
    // Legacy metrics kept for backward compatibility
    public double legacyCohesion;
    public double legacyDirectCoupling;
    public double legacyIndirectCoupling;

    // Graph structure metrics
    public int appMethodCount;
    public int systemMethodCount;
    public int callEdgeCount;
    public double avgOutDegree;
    public int maxOutDegree;
    public double reachableMethodCountAvg;
    public int sccCount;
    public double largestSccRatio;

    // Dependency / behavior metrics
    public double appToSystemCallRatio;
    public int sensitiveApiTypeCount;
    public String sensitiveApiTypes = "";
    public int sensitiveApiCallCount;
    public int reflectionCallCount;
    public int dynamicLoadCallCount;
    public int nativeLoadCallCount;
    public int runtimeExecCallCount;
    public int componentEntryCount;
}

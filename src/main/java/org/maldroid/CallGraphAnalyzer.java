package org.maldroid;

import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CallGraphAnalyzer {

    public static final String UNSUCCESSFUL_TRY_PATH = "D:\\DaChuang\\data_set\\unsuccessful_try\\flowAdroidCG_unseccess";

    private static final Map<String, Boolean> visited = new HashMap<>();
    static final Map<SootClass, StatsCallInfo> statsMap = new HashMap<>();

    /**
     * Analyzes a single APK and returns call graph features.
     * Returns null on failure after retries.
     */
    public static CallGraphFeatureResult analyze(String apkPath, String androidPlatformPath) {
        soot.G.reset();
        Scene.v().releaseCallGraph();
        Scene.v().releasePointsToAnalysis();
        Scene.v().releaseReachableMethods();
        statsMap.clear();
        visited.clear();
        MethodCallRecorder.clear();
        System.gc();

        final int maxRetries = 2;
        int currentTry = 0;
        final int timeout = 60;

        while (currentTry <= maxRetries) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<CallGraphFeatureResult> future = executor.submit(() -> analyzeOnce(apkPath, androidPlatformPath));
                CallGraphFeatureResult result = future.get(timeout, TimeUnit.SECONDS);
                if (result != null) {
                    return result;
                }

                currentTry++;
                if (currentTry <= maxRetries) {
                    System.out.println("Attempt " + currentTry + " failed for " + apkPath + ". Retrying...");
                    Scene.v().releaseCallGraph();
                    System.gc();
                }

            } catch (TimeoutException e) {
                System.err.println("Timeout analyzing: " + apkPath);
                currentTry++;
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error analyzing " + apkPath + ": " + e.getMessage());
                currentTry++;
            } finally {
                executor.shutdownNow();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        System.err.println("Executor did not terminate in time.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        System.out.println("All attempts failed for: " + apkPath);
        return null;
    }

    private static CallGraphFeatureResult analyzeOnce(String apkPath, String androidPlatformPath) {
        Map<String, SootMethod> methodRegistry = new HashMap<>();

        SetupApplication app = new SetupApplication(androidPlatformPath, apkPath);
        soot.G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Scene.v().loadNecessaryClasses();

        app.setCallbackFile(CallGraphAnalyzer.class.getResource("/AndroidCallbacks.txt").getFile());
        app.constructCallgraph();

        SootMethod entryPoint;
        try {
            entryPoint = app.getDummyMainMethod();
        } catch (NullPointerException e) {
            System.out.println("Could not retrieve dummy main method.");
            return null;
        }
        if (entryPoint == null) {
            System.out.println("No entry point found for: " + apkPath);
            return null;
        }

        CallGraph cg = Scene.v().getCallGraph();
        visit(cg, entryPoint, methodRegistry);

        for (SootClass sc : Scene.v().getClasses()) {
            if (sc.isApplicationClass() && !sc.isAbstract() && !sc.isInterface()) {
                StatsCallInfo stats = new StatsCallInfo(sc);
                for (SootMethod method : sc.getMethods()) {
                    if (method.isConcrete() && !method.isAbstract()) {
                        AnalyzeResult result = CallGraphMetrics.analyzeConstantsAndVariables(method);
                        stats.addMethodResult(method, result);
                    }
                }
                statsMap.put(sc, stats);
            }
        }

        CallGraphFeatureResult result = buildFeatureResult(MethodCallRecorder.getMethodCallInfoList(), methodRegistry);

        Scene.v().releaseCallGraph();
        Scene.v().releasePointsToAnalysis();
        Scene.v().releaseReachableMethods();

        return result;
    }

    public static void handleFailedApk(String apkPath) {
        Path sourcePath = Paths.get(apkPath);
        Path targetPath = Paths.get(UNSUCCESSFUL_TRY_PATH, sourcePath.getFileName().toString());
        try {
            if (!Files.exists(targetPath)) {
                Files.copy(sourcePath, targetPath);
                System.out.println("Copied failed APK to: " + targetPath);
            }
        } catch (IOException e) {
            System.err.println("Failed to copy unsuccessful APK: " + e.getMessage());
        }
    }

    private static void visit(CallGraph cg, SootMethod method, Map<String, SootMethod> methodRegistry) {
        String identifier = method.getSignature();
        visited.put(identifier, true);
        methodRegistry.put(identifier, method);

        MethodCallInfo currentMethodInfo = new MethodCallInfo(identifier);

        Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(method));
        while (targets.hasNext()) {
            SootMethod callee = (SootMethod) targets.next();
            if (callee == null) {
                continue;
            }

            String calleeId = callee.getSignature();
            methodRegistry.put(calleeId, callee);
            currentMethodInfo.addCalledMethod(calleeId);

            if (!visited.containsKey(calleeId)) {
                visit(cg, callee, methodRegistry);
            }
        }

        MethodCallRecorder.recordMethodCallInfo(currentMethodInfo);
    }

    private static CallGraphFeatureResult buildFeatureResult(List<MethodCallInfo> methodCallInfos, Map<String, SootMethod> methodRegistry) {
        CallGraphFeatureResult result = new CallGraphFeatureResult();

        result.legacyCohesion = CallGraphMetrics.calculateCohesion(methodCallInfos);
        double[] coupling = CallGraphMetrics.calculateCoupling(methodCallInfos);
        result.legacyDirectCoupling = coupling[0];
        result.legacyIndirectCoupling = coupling[1];

        Map<String, Set<String>> adjacency = new HashMap<>();
        Set<String> appNodes = new HashSet<>();
        Set<String> systemNodes = new HashSet<>();
        Set<String> sensitiveCategories = new LinkedHashSet<>();
        Set<String> appComponentClasses = new HashSet<>();

        int callEdgeCount = 0;
        int totalAppOutgoingEdges = 0;
        int appToSystemEdges = 0;
        int sensitiveApiCallCount = 0;
        int reflectionCallCount = 0;
        int dynamicLoadCallCount = 0;
        int nativeLoadCallCount = 0;
        int runtimeExecCallCount = 0;

        for (Map.Entry<String, SootMethod> entry : methodRegistry.entrySet()) {
            if (!isSyntheticMethod(entry.getValue())) {
                adjacency.putIfAbsent(entry.getKey(), new HashSet<>());
                if (isApplicationMethod(entry.getValue())) {
                    appNodes.add(entry.getKey());
                    if (isAndroidComponent(entry.getValue().getDeclaringClass())) {
                        appComponentClasses.add(entry.getValue().getDeclaringClass().getName());
                    }
                } else {
                    systemNodes.add(entry.getKey());
                }
            }
        }

        for (MethodCallInfo info : methodCallInfos) {
            String sourceSig = info.getMethodName();
            SootMethod sourceMethod = methodRegistry.get(sourceSig);
            if (sourceMethod == null || isSyntheticMethod(sourceMethod)) {
                continue;
            }

            adjacency.putIfAbsent(sourceSig, new HashSet<>());

            for (String targetSig : info.getCalledMethods().keySet()) {
                SootMethod targetMethod = methodRegistry.get(targetSig);
                if (targetMethod == null || isSyntheticMethod(targetMethod)) {
                    continue;
                }

                Set<String> outgoing = adjacency.computeIfAbsent(sourceSig, k -> new HashSet<>());
                boolean isNewEdge = outgoing.add(targetSig);
                adjacency.putIfAbsent(targetSig, new HashSet<>());

                if (!isNewEdge) {
                    continue;
                }

                callEdgeCount++;

                if (isApplicationMethod(sourceMethod) && !isApplicationMethod(targetMethod)) {
                    appToSystemEdges++;
                }

                if (isApplicationMethod(sourceMethod)) {
                    totalAppOutgoingEdges++;
                    Set<String> matchedCategories = SensitiveApiCatalog.matchCategories(targetMethod);
                    if (!matchedCategories.isEmpty()) {
                        sensitiveApiCallCount++;
                        sensitiveCategories.addAll(matchedCategories);
                        if (matchedCategories.contains(SensitiveApiCatalog.CATEGORY_REFLECTION)) {
                            reflectionCallCount++;
                        }
                        if (matchedCategories.contains(SensitiveApiCatalog.CATEGORY_DYNAMIC_LOADING)) {
                            dynamicLoadCallCount++;
                        }
                        if (matchedCategories.contains(SensitiveApiCatalog.CATEGORY_NATIVE_LOADING)) {
                            nativeLoadCallCount++;
                        }
                        if (matchedCategories.contains(SensitiveApiCatalog.CATEGORY_RUNTIME_EXEC)) {
                            runtimeExecCallCount++;
                        }
                    }
                }
            }
        }

        result.appMethodCount = appNodes.size();
        result.systemMethodCount = systemNodes.size();
        result.callEdgeCount = callEdgeCount;
        result.avgOutDegree = computeAverageOutDegree(adjacency, appNodes);
        result.maxOutDegree = computeMaxOutDegree(adjacency, appNodes);
        result.reachableMethodCountAvg = computeAverageReachableCount(adjacency, appNodes);
        int[] sccStats = computeStronglyConnectedComponents(adjacency, appNodes);
        result.sccCount = sccStats[0];
        result.largestSccRatio = result.appMethodCount > 0 ? (double) sccStats[1] / result.appMethodCount : 0.0;
        result.appToSystemCallRatio = totalAppOutgoingEdges > 0 ? (double) appToSystemEdges / totalAppOutgoingEdges : 0.0;
        result.sensitiveApiTypeCount = sensitiveCategories.size();
        result.sensitiveApiTypes = String.join(", ", sensitiveCategories);
        result.sensitiveApiCallCount = sensitiveApiCallCount;
        result.reflectionCallCount = reflectionCallCount;
        result.dynamicLoadCallCount = dynamicLoadCallCount;
        result.nativeLoadCallCount = nativeLoadCallCount;
        result.runtimeExecCallCount = runtimeExecCallCount;
        result.componentEntryCount = appComponentClasses.size();

        System.out.printf("App Methods: %d, System Methods: %d, Edges: %d%n",
                result.appMethodCount, result.systemMethodCount, result.callEdgeCount);
        System.out.printf("Sensitive API Types: %d, Sensitive API Calls: %d%n",
                result.sensitiveApiTypeCount, result.sensitiveApiCallCount);

        return result;
    }

    private static double computeAverageOutDegree(Map<String, Set<String>> adjacency, Set<String> appNodes) {
        if (appNodes.isEmpty()) {
            return 0.0;
        }
        int total = 0;
        for (String node : appNodes) {
            total += adjacency.getOrDefault(node, Collections.emptySet()).size();
        }
        return (double) total / appNodes.size();
    }

    private static int computeMaxOutDegree(Map<String, Set<String>> adjacency, Set<String> appNodes) {
        int max = 0;
        for (String node : appNodes) {
            max = Math.max(max, adjacency.getOrDefault(node, Collections.emptySet()).size());
        }
        return max;
    }

    private static double computeAverageReachableCount(Map<String, Set<String>> adjacency, Set<String> appNodes) {
        if (appNodes.isEmpty()) {
            return 0.0;
        }
        long totalReachable = 0;
        for (String node : appNodes) {
            totalReachable += computeReachableCount(adjacency, node);
        }
        return (double) totalReachable / appNodes.size();
    }

    private static int computeReachableCount(Map<String, Set<String>> adjacency, String start) {
        Set<String> seen = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (!seen.add(current)) {
                continue;
            }
            for (String next : adjacency.getOrDefault(current, Collections.emptySet())) {
                if (!seen.contains(next)) {
                    stack.push(next);
                }
            }
        }
        return Math.max(0, seen.size() - 1);
    }

    private static int[] computeStronglyConnectedComponents(Map<String, Set<String>> adjacency, Set<String> appNodes) {
        TarjanState state = new TarjanState();
        for (String node : appNodes) {
            if (!state.indices.containsKey(node)) {
                strongConnect(node, adjacency, appNodes, state);
            }
        }
        return new int[]{state.sccCount, state.maxSccSize};
    }

    private static void strongConnect(String node, Map<String, Set<String>> adjacency, Set<String> appNodes, TarjanState state) {
        state.indices.put(node, state.index);
        state.lowLinks.put(node, state.index);
        state.index++;
        state.stack.push(node);
        state.onStack.add(node);

        for (String next : adjacency.getOrDefault(node, Collections.emptySet())) {
            if (!appNodes.contains(next)) {
                continue;
            }
            if (!state.indices.containsKey(next)) {
                strongConnect(next, adjacency, appNodes, state);
                state.lowLinks.put(node, Math.min(state.lowLinks.get(node), state.lowLinks.get(next)));
            } else if (state.onStack.contains(next)) {
                state.lowLinks.put(node, Math.min(state.lowLinks.get(node), state.indices.get(next)));
            }
        }

        if (state.lowLinks.get(node).equals(state.indices.get(node))) {
            int size = 0;
            String current;
            do {
                current = state.stack.pop();
                state.onStack.remove(current);
                size++;
            } while (!node.equals(current));
            state.sccCount++;
            state.maxSccSize = Math.max(state.maxSccSize, size);
        }
    }

    private static boolean isApplicationMethod(SootMethod method) {
        return method != null
                && method.getDeclaringClass() != null
                && method.getDeclaringClass().isApplicationClass();
    }

    private static boolean isSyntheticMethod(SootMethod method) {
        if (method == null) {
            return true;
        }
        String declaringClass = method.getDeclaringClass().getName();
        return method.getName().contains("dummyMainMethod")
                || declaringClass.contains("dummyMainClass");
    }

    private static boolean isAndroidComponent(SootClass sootClass) {
        if (sootClass == null) {
            return false;
        }

        Set<String> componentBaseClasses = new HashSet<>();
        componentBaseClasses.add("android.app.Activity");
        componentBaseClasses.add("android.app.Service");
        componentBaseClasses.add("android.content.BroadcastReceiver");
        componentBaseClasses.add("android.content.ContentProvider");
        componentBaseClasses.add("android.app.Application");

        SootClass current = sootClass;
        while (current != null) {
            if (componentBaseClasses.contains(current.getName())) {
                return true;
            }
            try {
                current = current.hasSuperclass() ? current.getSuperclass() : null;
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }

    private static class TarjanState {
        private int index = 0;
        private int sccCount = 0;
        private int maxSccSize = 0;
        private final Map<String, Integer> indices = new HashMap<>();
        private final Map<String, Integer> lowLinks = new HashMap<>();
        private final Deque<String> stack = new ArrayDeque<>();
        private final Set<String> onStack = new HashSet<>();
    }
}

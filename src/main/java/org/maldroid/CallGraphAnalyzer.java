package org.maldroid;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

import soot.*;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.options.Options;

public class CallGraphAnalyzer {

    public static final String ANDROID_PLATFORM_PATH = "D:/Android SDK/platforms";
    public static final String UNSUCCESSFUL_TRY_PATH = "D:\\DaChuang\\data_set\\unsuccessful_try\\flowAdroidCG_unseccess";

    private static final Map<String, Boolean> visited = new HashMap<>();
    private static final CGExporter cge = new CGExporter();
    static final Map<SootClass, StatsCallInfo> statsMap = new HashMap<>();

    /**
     * Analyzes a single APK and returns [cohesion, directCoupling, indirectCoupling].
     * Returns null on failure after retries.
     */
    public static double[] analyze(String apkPath) {
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
                Future<double[]> future = executor.submit(() -> {
                    SetupApplication app = new SetupApplication(ANDROID_PLATFORM_PATH, apkPath);
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
                    visit(cg, entryPoint);

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

                    List<MethodCallInfo> methodCallInfos = MethodCallRecorder.getMethodCallInfoList();
                    double cohesion = CallGraphMetrics.calculateCohesion(methodCallInfos);
                    double[] coupling = CallGraphMetrics.calculateCoupling(methodCallInfos);

                    Scene.v().releaseCallGraph();
                    Scene.v().releasePointsToAnalysis();
                    Scene.v().releaseReachableMethods();

                    return new double[]{cohesion, coupling[0], coupling[1]};
                });

                double[] result = future.get(timeout, TimeUnit.SECONDS);
                if (result != null) return result;

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
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS))
                        System.err.println("Executor did not terminate in time.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        System.out.println("All attempts failed for: " + apkPath);
        return null;
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

    private static void visit(CallGraph cg, SootMethod m) {
        String identifier = m.getSignature();
        visited.put(identifier, true);

        MethodCallInfo currentMethodInfo = new MethodCallInfo(identifier);
        cge.createNode(identifier);

        Iterator<MethodOrMethodContext> ptargets = new Targets(cg.edgesInto(m));
        if (ptargets != null) {
            while (ptargets.hasNext()) {
                SootMethod p = (SootMethod) ptargets.next();
                if (p != null && !visited.containsKey(p.getSignature()))
                    visit(cg, p);
            }
        }

        Iterator<MethodOrMethodContext> ctargets = new Targets(cg.edgesOutOf(m));
        if (ctargets != null) {
            while (ctargets.hasNext()) {
                SootMethod c = (SootMethod) ctargets.next();
                if (c == null) continue;
                String cID = c.getSignature();
                cge.createNode(cID);
                currentMethodInfo.addCalledMethod(cID);
                cge.linkNodeByID(identifier, cID);
                if (!visited.containsKey(cID))
                    visit(cg, c);
            }
        }

        MethodCallRecorder.recordMethodCallInfo(currentMethodInfo);
    }
}

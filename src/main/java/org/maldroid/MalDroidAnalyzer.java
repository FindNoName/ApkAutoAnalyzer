package org.maldroid;

import java.io.File;

public class MalDroidAnalyzer {

    // ===== 默认配置（可通过命令行参数覆盖）=====
    private static final String DEFAULT_ANDROID_PLATFORM_PATH = System.getenv("ANDROID_HOME") != null
            ? System.getenv("ANDROID_HOME") + "/platforms"
            : "";
    private static final String DEFAULT_APK_FOLDER_PATH = "D:/DaChuang/data_set/Androzoo";
    private static final String DEFAULT_EXCEL_FILE_PATH = "output/results.xlsx";

    private static final ExcelReportWriter writer = new ExcelReportWriter();

    /**
     * 用法: java -jar maldroid-analyzer.jar [apkFolder] [excelOutput] [androidPlatforms]
     *   apkFolder        - 待分析的 APK 文件夹路径（默认: apks）
     *   excelOutput      - 输出 Excel 文件路径（默认: output/results.xlsx）
     *   androidPlatforms - Android SDK platforms 目录（默认: $ANDROID_HOME/platforms）
     */
    public static void main(String[] args) {
        String apkFolderPath = DEFAULT_APK_FOLDER_PATH;
        String excelFilePath = DEFAULT_EXCEL_FILE_PATH;
        String androidPlatforms = DEFAULT_ANDROID_PLATFORM_PATH;

        System.out.println("APK folder:        " + apkFolderPath);
        System.out.println("Excel output:      " + excelFilePath);
        System.out.println("Android platforms: " + androidPlatforms);

        writer.loadOrInit(excelFilePath);
        writer.removeDuplicateRows();
        try {
            traverseAndAnalyze(new File(apkFolderPath), androidPlatforms);
        } finally {
            writer.removeDuplicateRows();
            if (!writer.flushPending()) {
                writer.save(excelFilePath);
            }
        }
    }

    private static void traverseAndAnalyze(File folder, String androidPlatforms) {
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Folder not found: " + folder.getAbsolutePath());
            return;
        }
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                traverseAndAnalyze(file, androidPlatforms);
            } else if (file.getName().toLowerCase().endsWith(".apk")) {
                String apkPath = file.getAbsolutePath();
                String apkName = file.getName();

                if (writer.isAlreadyAnalyzed(apkName)) {
                    System.out.println("Skipping already analyzed: " + apkName);
                    continue;
                }

                System.out.println("Analyzing: " + apkPath);
                ApkAnalysisResult result = new ApkAnalysisResult(apkPath);
                result.maliciousLabel = SampleLabelResolver.inferFromFile(file);

                // Module 1: Call Graph (cohesion + coupling)
                try {
                    CallGraphFeatureResult cgMetrics = CallGraphAnalyzer.analyze(apkPath, androidPlatforms);
                    if (cgMetrics != null) {
                        result.cohesion = cgMetrics.legacyCohesion;
                        result.directCoupling = cgMetrics.legacyDirectCoupling;
                        result.indirectCoupling = cgMetrics.legacyIndirectCoupling;
                        result.appMethodCount = cgMetrics.appMethodCount;
                        result.systemMethodCount = cgMetrics.systemMethodCount;
                        result.callEdgeCount = cgMetrics.callEdgeCount;
                        result.avgOutDegree = cgMetrics.avgOutDegree;
                        result.maxOutDegree = cgMetrics.maxOutDegree;
                        result.reachableMethodCountAvg = cgMetrics.reachableMethodCountAvg;
                        result.sccCount = cgMetrics.sccCount;
                        result.largestSccRatio = cgMetrics.largestSccRatio;
                        result.appToSystemCallRatio = cgMetrics.appToSystemCallRatio;
                        result.sensitiveApiTypeCount = cgMetrics.sensitiveApiTypeCount;
                        result.sensitiveApiTypes = cgMetrics.sensitiveApiTypes;
                        result.sensitiveApiCallCount = cgMetrics.sensitiveApiCallCount;
                        result.reflectionCallCount = cgMetrics.reflectionCallCount;
                        result.dynamicLoadCallCount = cgMetrics.dynamicLoadCallCount;
                        result.nativeLoadCallCount = cgMetrics.nativeLoadCallCount;
                        result.runtimeExecCallCount = cgMetrics.runtimeExecCallCount;
                        result.componentEntryCount = cgMetrics.componentEntryCount;
                    } else {
                        CallGraphAnalyzer.handleFailedApk(apkPath);
                    }
                } catch (Exception e) {
                    System.err.println("CallGraph failed for " + apkName + ": " + e.getMessage());
                }

                // Module 2: Permission Analysis
                try {
                    PermissionAnalyzer.PermissionResult perm = PermissionAnalyzer.analyze(apkPath);
                    result.totalPermissions        = perm.totalPermissions;
                    result.dangerousPermissionCount = perm.dangerousPermissionCount;
                    result.dangerousPermissionRatio = perm.dangerousPermissionRatio;
                    result.highRiskPermissionCount  = perm.highRiskPermissionCount;
                    result.smsPermissionFlag        = perm.smsPermissionFlag;
                    result.locationPermissionFlag   = perm.locationPermissionFlag;
                    result.phonePermissionFlag      = perm.phonePermissionFlag;
                    result.audioPermissionFlag      = perm.audioPermissionFlag;
                    result.storagePermissionFlag    = perm.storagePermissionFlag;
                    result.cameraPermissionFlag     = perm.cameraPermissionFlag;
                } catch (Exception e) {
                    System.err.println("Permission analysis failed for " + apkName + ": " + e.getMessage());
                }

                // Module 4: Manifest Analysis
                try {
                    ManifestAnalyzer.ManifestResult mf = ManifestAnalyzer.analyze(apkPath);
                    result.activityCount        = mf.activityCount;
                    result.serviceCount         = mf.serviceCount;
                    result.receiverCount        = mf.receiverCount;
                    result.providerCount        = mf.providerCount;
                    result.hasDebuggable        = mf.hasDebuggable;
                    result.hasAllowBackup       = mf.hasAllowBackup;
                    result.hasCleartextTraffic  = mf.hasCleartextTraffic;
                    result.minSdk               = mf.minSdk;
                    result.targetSdk            = mf.targetSdk;
                    result.customPermissionCount = mf.customPermissionCount;
                } catch (Exception e) {
                    System.err.println("Manifest analysis failed for " + apkName + ": " + e.getMessage());
                }

                // Module 5: String Features
                try {
                    StringFeatureExtractor.StringFeatureResult sf = StringFeatureExtractor.extract(apkPath);
                    result.urlCount               = sf.urlCount;
                    result.ipCount                = sf.ipCount;
                    result.suspiciousKeywordCount = sf.suspiciousKeywordCount;
                    result.highEntropyStringCount = sf.highEntropyStringCount;
                } catch (Exception e) {
                    System.err.println("String feature extraction failed for " + apkName + ": " + e.getMessage());
                }

                // Module 3: GLCM Texture Features
                try {
                    ImageFeatureResult imageFeatures = GlcmFeatureExtractor.extract(apkPath);
                    if (imageFeatures != null) {
                        result.dexCount = imageFeatures.dexCount;
                        result.dexTotalSize = imageFeatures.dexTotalSize;
                        result.glcmContrast = imageFeatures.glcmContrastMean;
                        result.glcmContrastStd = imageFeatures.glcmContrastStd;
                        result.glcmDissimilarity = imageFeatures.glcmDissimilarityMean;
                        result.glcmDissimilarityStd = imageFeatures.glcmDissimilarityStd;
                        result.glcmHomogeneity = imageFeatures.glcmHomogeneityMean;
                        result.glcmHomogeneityStd = imageFeatures.glcmHomogeneityStd;
                        result.glcmEnergy = imageFeatures.glcmEnergyMean;
                        result.glcmEnergyStd = imageFeatures.glcmEnergyStd;
                        result.glcmCorrelation = imageFeatures.glcmCorrelationMean;
                        result.glcmCorrelationStd = imageFeatures.glcmCorrelationStd;
                        result.glcmAsm = imageFeatures.glcmAsmMean;
                        result.glcmAsmStd = imageFeatures.glcmAsmStd;
                        result.glcmEntropyMean = imageFeatures.glcmEntropyMean;
                        result.glcmEntropyStd = imageFeatures.glcmEntropyStd;
                        result.byteEntropyMean = imageFeatures.byteEntropyMean;
                        result.byteEntropyStd = imageFeatures.byteEntropyStd;
                        result.grayMean = imageFeatures.grayMean;
                        result.grayStd = imageFeatures.grayStd;
                        result.edgeDensityMean = imageFeatures.edgeDensityMean;
                        result.edgeDensityStd = imageFeatures.edgeDensityStd;
                    }
                } catch (Exception e) {
                    System.err.println("GLCM failed for " + apkName + ": " + e.getMessage());
                }

                writer.writeRow(result);
                System.out.println("Done: " + apkName);
            }
        }
    }
}

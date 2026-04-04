package org.maldroid;

import java.io.File;

public class MalDroidAnalyzer {

    // ===== 默认配置（可通过命令行参数覆盖）=====
    private static final String DEFAULT_ANDROID_PLATFORM_PATH = System.getenv("ANDROID_HOME") != null
            ? System.getenv("ANDROID_HOME") + "/platforms"
            : "";
    private static final String DEFAULT_APK_FOLDER_PATH = "apks";
    private static final String DEFAULT_EXCEL_FILE_PATH = "output/results.xlsx";
    private static final String DEFAULT_GEXF_OUTPUT_DIR = "output/callgraphs";

    private static final ExcelReportWriter writer = new ExcelReportWriter();

    /**
     * 用法: java -jar maldroid-analyzer.jar [apkFolder] [excelOutput] [androidPlatforms] [gexfOutputDir]
     *   apkFolder        - 待分析的 APK 文件夹路径（默认: apks）
     *   excelOutput      - 输出 Excel 文件路径（默认: output/results.xlsx）
     *   androidPlatforms - Android SDK platforms 目录（默认: $ANDROID_HOME/platforms）
     *   gexfOutputDir    - 调用图 GEXF 输出目录（默认: output/callgraphs）
     */
    public static void main(String[] args) {
        String apkFolderPath    = args.length > 0 ? args[0] : DEFAULT_APK_FOLDER_PATH;
        String excelFilePath    = args.length > 1 ? args[1] : DEFAULT_EXCEL_FILE_PATH;
        String androidPlatforms = args.length > 2 ? args[2] : DEFAULT_ANDROID_PLATFORM_PATH;
        String gexfOutputDir    = args.length > 3 ? args[3] : DEFAULT_GEXF_OUTPUT_DIR;

        System.out.println("APK folder:        " + apkFolderPath);
        System.out.println("Excel output:      " + excelFilePath);
        System.out.println("Android platforms: " + androidPlatforms);
        System.out.println("GEXF output dir:   " + gexfOutputDir);

        writer.loadOrInit(excelFilePath);
        writer.removeDuplicateRows();

        traverseAndAnalyze(new File(apkFolderPath));

        writer.removeDuplicateRows();
        writer.save(excelFilePath);
    }

    private static void traverseAndAnalyze(File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Folder not found: " + folder.getAbsolutePath());
            return;
        }
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                traverseAndAnalyze(file);
            } else if (file.getName().toLowerCase().endsWith(".apk")) {
                String apkPath = file.getAbsolutePath();
                String apkName = file.getName();

                if (writer.isAlreadyAnalyzed(apkName)) {
                    System.out.println("Skipping already analyzed: " + apkName);
                    continue;
                }

                System.out.println("Analyzing: " + apkPath);
                ApkAnalysisResult result = new ApkAnalysisResult(apkPath);

                // Module 1: Call Graph (cohesion + coupling)
                try {
                    double[] cgMetrics = CallGraphAnalyzer.analyze(apkPath);
                    if (cgMetrics != null) {
                        result.cohesion = cgMetrics[0];
                        result.directCoupling = cgMetrics[1];
                        result.indirectCoupling = cgMetrics[2];
                    } else {
                        CallGraphAnalyzer.handleFailedApk(apkPath);
                    }
                } catch (Exception e) {
                    System.err.println("CallGraph failed for " + apkName + ": " + e.getMessage());
                }

                // Module 2: Permission Analysis
                try {
                    PermissionAnalyzer.PermissionResult perm = PermissionAnalyzer.analyze(apkPath);
                    result.totalPermissions = perm.totalPermissions;
                    result.permissionGroups = perm.permissionGroups;
                    result.permissionNames = perm.permissionNames;
                    result.matchedPermissions = perm.matchedPermissions;
                    result.permissionRatio = perm.permissionRatio;
                } catch (Exception e) {
                    System.err.println("Permission analysis failed for " + apkName + ": " + e.getMessage());
                }

                // Module 3: GLCM Texture Features
                try {
                    double[] glcm = GlcmFeatureExtractor.extract(apkPath);
                    if (glcm != null) {
                        result.glcmContrast = glcm[0];
                        result.glcmDissimilarity = glcm[1];
                        result.glcmHomogeneity = glcm[2];
                        result.glcmEnergy = glcm[3];
                        result.glcmCorrelation = glcm[4];
                        result.glcmAsm = glcm[5];
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

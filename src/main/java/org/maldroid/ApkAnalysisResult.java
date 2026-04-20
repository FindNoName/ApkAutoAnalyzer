package org.maldroid;

public class ApkAnalysisResult {
    // Identity
    public String apkPath;
    public Integer maliciousLabel;

    // Module: CallGraphAnalyzer
    public double cohesion;
    public double directCoupling;
    public double indirectCoupling;
    public int appMethodCount;
    public int systemMethodCount;
    public int callEdgeCount;
    public double avgOutDegree;
    public int maxOutDegree;
    public double reachableMethodCountAvg;
    public int sccCount;
    public double largestSccRatio;
    public double appToSystemCallRatio;
    public int sensitiveApiTypeCount;
    public String sensitiveApiTypes;
    public int sensitiveApiCallCount;
    public int reflectionCallCount;
    public int dynamicLoadCallCount;
    public int nativeLoadCallCount;
    public int runtimeExecCallCount;
    public int componentEntryCount;

    // Module: PermissionAnalyzer
    public int totalPermissions;
    public int dangerousPermissionCount;
    public double dangerousPermissionRatio;
    public int highRiskPermissionCount;
    public int smsPermissionFlag;
    public int locationPermissionFlag;
    public int phonePermissionFlag;
    public int audioPermissionFlag;
    public int storagePermissionFlag;
    public int cameraPermissionFlag;

    // Module: ManifestAnalyzer
    public int activityCount;
    public int serviceCount;
    public int receiverCount;
    public int providerCount;
    public int hasDebuggable;
    public int hasAllowBackup;
    public int hasCleartextTraffic;
    public int minSdk;
    public int targetSdk;
    public int customPermissionCount;

    // Module: StringFeatureExtractor
    public int urlCount;
    public int ipCount;
    public int suspiciousKeywordCount;
    public int highEntropyStringCount;

    // Module: GlcmFeatureExtractor
    public int dexCount;
    public long dexTotalSize;
    public double glcmContrast;
    public double glcmContrastStd;
    public double glcmDissimilarity;
    public double glcmDissimilarityStd;
    public double glcmHomogeneity;
    public double glcmHomogeneityStd;
    public double glcmEnergy;
    public double glcmEnergyStd;
    public double glcmCorrelation;
    public double glcmCorrelationStd;
    public double glcmAsm;
    public double glcmAsmStd;
    public double glcmEntropyMean;
    public double glcmEntropyStd;
    public double byteEntropyMean;
    public double byteEntropyStd;
    public double grayMean;
    public double grayStd;
    public double edgeDensityMean;
    public double edgeDensityStd;

    public ApkAnalysisResult(String apkPath) {
        this.apkPath = apkPath;
        this.sensitiveApiTypes = "";
    }
}

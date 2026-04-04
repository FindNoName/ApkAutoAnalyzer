package org.maldroid;

public class ApkAnalysisResult {
    // Identity
    public String apkPath;

    // Module: CallGraphAnalyzer
    public double cohesion;
    public double directCoupling;
    public double indirectCoupling;

    // Module: PermissionAnalyzer
    public int totalPermissions;
    public String permissionGroups;
    public String permissionNames;
    public int matchedPermissions;
    public double permissionRatio;

    // Module: GlcmFeatureExtractor
    public double glcmContrast;
    public double glcmDissimilarity;
    public double glcmHomogeneity;
    public double glcmEnergy;
    public double glcmCorrelation;
    public double glcmAsm;

    public ApkAnalysisResult(String apkPath) {
        this.apkPath = apkPath;
        this.permissionGroups = "";
        this.permissionNames = "";
    }
}

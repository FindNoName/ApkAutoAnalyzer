package org.maldroid;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermissionAnalyzer {

    public static class PermissionResult {
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
    }

    private static final Set<String> DANGEROUS_PERMISSIONS = new HashSet<>(Arrays.asList(
        "android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR",
        "android.permission.CAMERA",
        "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS", "android.permission.GET_ACCOUNTS",
        "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_PHONE_STATE", "android.permission.CALL_PHONE",
        "android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG",
        "android.permission.ADD_VOICEMAIL", "android.permission.USE_SIP", "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.BODY_SENSORS",
        "android.permission.SEND_SMS", "android.permission.RECEIVE_SMS",
        "android.permission.READ_SMS", "android.permission.RECEIVE_WAP_PUSH", "android.permission.RECEIVE_MMS",
        "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"
    ));

    private static final Set<String> HIGH_RISK_PERMISSIONS = new HashSet<>(Arrays.asList(
        "android.permission.READ_SMS", "android.permission.SEND_SMS", "android.permission.RECEIVE_SMS",
        "android.permission.READ_CALL_LOG", "android.permission.READ_CONTACTS",
        "android.permission.READ_PHONE_STATE", "android.permission.RECORD_AUDIO",
        "android.permission.RECEIVE_BOOT_COMPLETED", "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.REQUEST_INSTALL_PACKAGES", "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.BIND_DEVICE_ADMIN", "android.permission.CHANGE_COMPONENT_ENABLED_STATE",
        "android.permission.MOUNT_UNMOUNT_FILESYSTEMS", "android.permission.WRITE_SETTINGS"
    ));

    private static final Set<String> SMS_PERMISSIONS = new HashSet<>(Arrays.asList(
        "android.permission.READ_SMS", "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS", "android.permission.RECEIVE_MMS"
    ));
    private static final Set<String> LOCATION_PERMISSIONS = new HashSet<>(Arrays.asList(
        "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"
    ));
    private static final Set<String> PHONE_PERMISSIONS = new HashSet<>(Arrays.asList(
        "android.permission.READ_PHONE_STATE", "android.permission.CALL_PHONE",
        "android.permission.READ_CALL_LOG", "android.permission.PROCESS_OUTGOING_CALLS"
    ));
    private static final Set<String> AUDIO_PERMISSIONS = new HashSet<>(Arrays.asList(
        "android.permission.RECORD_AUDIO"
    ));
    private static final Set<String> STORAGE_PERMISSIONS = new HashSet<>(Arrays.asList(
        "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"
    ));
    private static final Set<String> CAMERA_PERMISSIONS = new HashSet<>(Arrays.asList(
        "android.permission.CAMERA"
    ));

    public static PermissionResult analyze(String apkPath) throws IOException {
        PermissionResult result = new PermissionResult();
        try (ApkFile apkFile = new ApkFile(new File(apkPath))) {
            ApkMeta meta = apkFile.getApkMeta();
            List<String> permissions = meta.getUsesPermissions();
            Set<String> permSet = new HashSet<>(permissions);

            result.totalPermissions = permissions.size();

            for (String p : permSet) {
                if (DANGEROUS_PERMISSIONS.contains(p)) result.dangerousPermissionCount++;
                if (HIGH_RISK_PERMISSIONS.contains(p)) result.highRiskPermissionCount++;
            }

            result.dangerousPermissionRatio = result.totalPermissions > 0
                ? Math.round((double) result.dangerousPermissionCount / result.totalPermissions * 100.0) / 100.0
                : 0.0;

            result.smsPermissionFlag     = containsAny(permSet, SMS_PERMISSIONS)      ? 1 : 0;
            result.locationPermissionFlag = containsAny(permSet, LOCATION_PERMISSIONS) ? 1 : 0;
            result.phonePermissionFlag   = containsAny(permSet, PHONE_PERMISSIONS)    ? 1 : 0;
            result.audioPermissionFlag   = containsAny(permSet, AUDIO_PERMISSIONS)    ? 1 : 0;
            result.storagePermissionFlag = containsAny(permSet, STORAGE_PERMISSIONS)  ? 1 : 0;
            result.cameraPermissionFlag  = containsAny(permSet, CAMERA_PERMISSIONS)   ? 1 : 0;
        }
        return result;
    }

    private static boolean containsAny(Set<String> permSet, Set<String> target) {
        for (String p : target) {
            if (permSet.contains(p)) return true;
        }
        return false;
    }
}

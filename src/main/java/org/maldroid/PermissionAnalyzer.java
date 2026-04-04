package org.maldroid;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermissionAnalyzer {

    public static class PermissionResult {
        public int totalPermissions;
        public String permissionGroups;
        public String permissionNames;
        public int matchedPermissions;
        public double permissionRatio;
    }

    private static final List<Set<String>> PERMISSION_GROUPS = Arrays.asList(
        new HashSet<>(Arrays.asList("android.permission.ACCESS_WIFI_STATE", "android.permission.ACCESS_NETWORK_STATE")),
        new HashSet<>(Arrays.asList("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION")),
        new HashSet<>(Arrays.asList("android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN")),
        new HashSet<>(Arrays.asList("android.permission.MANAGE_ACCOUNTS")),
        new HashSet<>(Arrays.asList("android.permission.AUTHENTICATE_ACCOUNTS", "android.permission.GET_ACCOUNTS",
                "android.permission.MANAGE_ACCOUNTS", "android.permission.USE_CREDENTIALS")),
        new HashSet<>(Arrays.asList("android.permission.CLEAR_APP_CACHE", "android.permission.GET_PACKAGE_SIZE")),
        new HashSet<>(Arrays.asList("android.permission.READ_SMS", "android.permission.RECEIVE_SMS", "android.permission.SEND_SMS")),
        new HashSet<>(Arrays.asList("android.permission.READ_SYNC_SETTINGS", "android.permission.WRITE_SYNC_SETTINGS")),
        new HashSet<>(Arrays.asList("android.permission.SUBSCRIBED_FEEDS_READ", "android.permission.SUBSCRIBED_FEEDS_WRITE"))
    );

    public static PermissionResult analyze(String apkPath) throws IOException {
        PermissionResult result = new PermissionResult();
        try (ApkFile apkFile = new ApkFile(new File(apkPath))) {
            ApkMeta meta = apkFile.getApkMeta();
            List<String> permissions = meta.getUsesPermissions();

            result.totalPermissions = permissions.size();

            Set<String> permSet = new HashSet<>(permissions);
            List<String> matchedNames = new ArrayList<>();
            List<String> matchedGroups = new ArrayList<>();

            for (int g = 0; g < PERMISSION_GROUPS.size(); g++) {
                Set<String> group = PERMISSION_GROUPS.get(g);
                for (String perm : group) {
                    if (permSet.contains(perm)) {
                        matchedNames.add(perm);
                        matchedGroups.add("组" + (g + 1));
                        break; // one representative per group
                    }
                }
            }

            result.matchedPermissions = matchedNames.size();
            result.permissionGroups = String.join(", ", matchedGroups);
            result.permissionNames = String.join(", ", matchedNames);
            result.permissionRatio = result.totalPermissions > 0
                    ? Math.round((double) result.matchedPermissions / result.totalPermissions * 100.0) / 100.0
                    : 0.0;
        }
        return result;
    }
}

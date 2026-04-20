package org.maldroid;

import soot.SootMethod;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SensitiveApiCatalog {

    public static final String CATEGORY_SMS = "SMS";
    public static final String CATEGORY_DEVICE_ID = "DEVICE_ID";
    public static final String CATEGORY_LOCATION = "LOCATION";
    public static final String CATEGORY_CONTACTS = "CONTACTS";
    public static final String CATEGORY_CAMERA_AUDIO = "CAMERA_AUDIO";
    public static final String CATEGORY_WEBVIEW_BRIDGE = "WEBVIEW_BRIDGE";
    public static final String CATEGORY_REFLECTION = "REFLECTION";
    public static final String CATEGORY_DYNAMIC_LOADING = "DYNAMIC_LOADING";
    public static final String CATEGORY_NATIVE_LOADING = "NATIVE_LOADING";
    public static final String CATEGORY_RUNTIME_EXEC = "RUNTIME_EXEC";

    private static final List<ApiPattern> PATTERNS = Arrays.asList(
            new ApiPattern(CATEGORY_SMS, "android.telephony.SmsManager", null),
            new ApiPattern(CATEGORY_DEVICE_ID, "android.telephony.TelephonyManager", null),
            new ApiPattern(CATEGORY_DEVICE_ID, "android.provider.Settings$Secure", "getString"),
            new ApiPattern(CATEGORY_LOCATION, "android.location.LocationManager", null),
            new ApiPattern(CATEGORY_LOCATION, "com.google.android.gms.location.FusedLocationProviderClient", null),
            new ApiPattern(CATEGORY_CONTACTS, "android.provider.ContactsContract", null),
            new ApiPattern(CATEGORY_CONTACTS, "android.provider.CallLog", null),
            new ApiPattern(CATEGORY_CAMERA_AUDIO, "android.hardware.Camera", null),
            new ApiPattern(CATEGORY_CAMERA_AUDIO, "android.hardware.camera2.CameraManager", null),
            new ApiPattern(CATEGORY_CAMERA_AUDIO, "android.media.MediaRecorder", null),
            new ApiPattern(CATEGORY_CAMERA_AUDIO, "android.media.AudioRecord", null),
            new ApiPattern(CATEGORY_WEBVIEW_BRIDGE, "android.webkit.WebView", "addJavascriptInterface"),
            new ApiPattern(CATEGORY_REFLECTION, "java.lang.Class", "forName"),
            new ApiPattern(CATEGORY_REFLECTION, "java.lang.reflect.Method", "invoke"),
            new ApiPattern(CATEGORY_REFLECTION, "java.lang.reflect.Constructor", "newInstance"),
            new ApiPattern(CATEGORY_DYNAMIC_LOADING, "dalvik.system.DexClassLoader", null),
            new ApiPattern(CATEGORY_DYNAMIC_LOADING, "dalvik.system.PathClassLoader", null),
            new ApiPattern(CATEGORY_NATIVE_LOADING, "java.lang.System", "load"),
            new ApiPattern(CATEGORY_NATIVE_LOADING, "java.lang.System", "loadLibrary"),
            new ApiPattern(CATEGORY_RUNTIME_EXEC, "java.lang.Runtime", "exec"),
            new ApiPattern(CATEGORY_RUNTIME_EXEC, "java.lang.ProcessBuilder", "start")
    );

    private SensitiveApiCatalog() {
    }

    public static Set<String> matchCategories(SootMethod method) {
        Set<String> matched = new LinkedHashSet<>();
        if (method == null) {
            return matched;
        }

        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();

        for (ApiPattern pattern : PATTERNS) {
            if (!className.equals(pattern.className)) {
                continue;
            }
            if (pattern.methodName == null || pattern.methodName.equals(methodName)) {
                matched.add(pattern.category);
            }
        }
        return matched;
    }

    public static boolean hasCategory(SootMethod method, String category) {
        return matchCategories(method).contains(category);
    }

    private static final class ApiPattern {
        private final String category;
        private final String className;
        private final String methodName;

        private ApiPattern(String category, String className, String methodName) {
            this.category = category;
            this.className = className;
            this.methodName = methodName;
        }
    }
}

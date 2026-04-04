package org.maldroid;

import java.util.ArrayList;
import java.util.List;

public class MethodCallRecorder {
    private static List<MethodCallInfo> methodCallInfosList = new ArrayList<>();

    public static void recordMethodCallInfo(MethodCallInfo info) {
        methodCallInfosList.add(info);
    }

    public static List<MethodCallInfo> getMethodCallInfoList() {
        return methodCallInfosList;
    }

    public static void clear() {
        methodCallInfosList.clear();
    }
}

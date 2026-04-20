package org.maldroid;

import java.io.File;

public final class SampleLabelResolver {

    private static final String BENIGN_DIR_NAME = "good";
    private static final String MALICIOUS_DIR_NAME = "bad";

    private SampleLabelResolver() {
    }

    public static Integer inferFromPath(String apkPath) {
        if (apkPath == null || apkPath.trim().isEmpty()) {
            return null;
        }
        return inferFromFile(new File(apkPath));
    }

    public static Integer inferFromFile(File apkFile) {
        if (apkFile == null) {
            return null;
        }

        File current = apkFile.isDirectory() ? apkFile : apkFile.getParentFile();
        while (current != null) {
            String folderName = current.getName();
            if (MALICIOUS_DIR_NAME.equalsIgnoreCase(folderName)) {
                return 1;
            }
            if (BENIGN_DIR_NAME.equalsIgnoreCase(folderName)) {
                return 0;
            }
            current = current.getParentFile();
        }

        return null;
    }
}

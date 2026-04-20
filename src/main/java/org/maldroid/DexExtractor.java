package org.maldroid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DexExtractor {

    private DexExtractor() {
    }

    public static List<byte[]> extractDexFiles(String apkPath) throws IOException {
        List<DexEntry> dexEntries = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(apkPath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!isDexEntry(name)) {
                    continue;
                }

                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    dexEntries.add(new DexEntry(name, readAllBytes(inputStream)));
                }
            }
        }

        Collections.sort(dexEntries, Comparator.comparing(d -> d.name));

        List<byte[]> dexFiles = new ArrayList<>();
        for (DexEntry dexEntry : dexEntries) {
            dexFiles.add(dexEntry.bytes);
        }
        return dexFiles;
    }

    private static boolean isDexEntry(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase();
        return normalized.matches("classes\\d*\\.dex");
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private static final class DexEntry {
        private final String name;
        private final byte[] bytes;

        private DexEntry(String name, byte[] bytes) {
            this.name = name;
            this.bytes = bytes;
        }
    }
}

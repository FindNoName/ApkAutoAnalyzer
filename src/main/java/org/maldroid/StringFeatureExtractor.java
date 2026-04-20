package org.maldroid;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringFeatureExtractor {

    public static class StringFeatureResult {
        public int urlCount;
        public int ipCount;
        public int suspiciousKeywordCount;
        public int highEntropyStringCount;
    }

    private static final Pattern URL_PATTERN =
        Pattern.compile("https?://[\\w\\-./:%?=&@#]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern IP_PATTERN =
        Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Set<String> SUSPICIOUS_KEYWORDS = new HashSet<>(Arrays.asList(
        "su", "/system/bin", "/system/xbin", "chmod", "chown", "mount",
        "busybox", "superuser", "root", "exploit", "payload",
        "cmd.exe", "powershell", "/bin/sh", "/bin/bash",
        "base64_decode", "eval(", "Runtime.exec"
    ));

    public static StringFeatureResult extract(String apkPath) throws IOException {
        StringFeatureResult result = new StringFeatureResult();
        List<byte[]> dexFiles = DexExtractor.extractDexFiles(apkPath);
        for (byte[] dex : dexFiles) {
            String text = extractStringsFromDex(dex);
            countFeatures(text, result);
        }
        return result;
    }

    /** Extract printable ASCII strings (length >= 6) from raw DEX bytes. */
    private static String extractStringsFromDex(byte[] dex) {
        StringBuilder sb = new StringBuilder();
        StringBuilder cur = new StringBuilder();
        for (byte b : dex) {
            char c = (char) (b & 0xFF);
            if (c >= 0x20 && c < 0x7F) {
                cur.append(c);
            } else {
                if (cur.length() >= 6) {
                    sb.append(cur).append('\n');
                }
                cur.setLength(0);
            }
        }
        if (cur.length() >= 6) sb.append(cur);
        return sb.toString();
    }

    private static void countFeatures(String text, StringFeatureResult result) {
        // URLs
        Matcher m = URL_PATTERN.matcher(text);
        while (m.find()) result.urlCount++;

        // IPs
        m = IP_PATTERN.matcher(text);
        while (m.find()) result.ipCount++;

        // Suspicious keywords
        String lower = text.toLowerCase();
        for (String kw : SUSPICIOUS_KEYWORDS) {
            int idx = 0;
            while ((idx = lower.indexOf(kw.toLowerCase(), idx)) != -1) {
                result.suspiciousKeywordCount++;
                idx += kw.length();
            }
        }

        // High-entropy strings (per line, length >= 8, entropy >= 4.5)
        for (String line : text.split("\n")) {
            if (line.length() >= 8 && shannonEntropy(line) >= 4.5) {
                result.highEntropyStringCount++;
            }
        }
    }

    private static double shannonEntropy(String s) {
        int[] freq = new int[256];
        for (char c : s.toCharArray()) freq[c & 0xFF]++;
        double entropy = 0;
        int len = s.length();
        for (int f : freq) {
            if (f > 0) {
                double p = (double) f / len;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }
}

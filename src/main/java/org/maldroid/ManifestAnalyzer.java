package org.maldroid;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ManifestAnalyzer {

    public static class ManifestResult {
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
    }

    public static ManifestResult analyze(String apkPath) throws IOException {
        ManifestResult result = new ManifestResult();
        try (ApkFile apkFile = new ApkFile(new File(apkPath))) {
            // SDK versions from ApkMeta
            ApkMeta meta = apkFile.getApkMeta();
            result.minSdk    = safeInt(meta.getMinSdkVersion());
            result.targetSdk = safeInt(meta.getTargetSdkVersion());
            result.customPermissionCount = meta.getPermissions() != null ? meta.getPermissions().size() : 0;

            // Parse XML for the rest
            String xml = apkFile.getManifestXml();
            parseXml(xml, result);
        }
        return result;
    }

    private static void parseXml(String xml, ManifestResult result) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attrs) {
                    switch (qName) {
                        case "activity":      result.activityCount++;  break;
                        case "service":       result.serviceCount++;   break;
                        case "receiver":      result.receiverCount++;  break;
                        case "provider":      result.providerCount++;  break;
                        case "application":
                            if ("true".equals(attrs.getValue("android:debuggable")))
                                result.hasDebuggable = 1;
                            if (!"false".equals(attrs.getValue("android:allowBackup")))
                                result.hasAllowBackup = 1;
                            if ("true".equals(attrs.getValue("android:usesCleartextTraffic")))
                                result.hasCleartextTraffic = 1;
                            break;
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("ManifestAnalyzer XML parse error: " + e.getMessage());
        }
    }

    private static int safeInt(String s) {
        if (s == null || s.isEmpty()) return 0;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}

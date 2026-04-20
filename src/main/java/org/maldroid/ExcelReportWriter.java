package org.maldroid;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExcelReportWriter {

    private static final String[] HEADERS = {
            "APK Path",
            "Cohesion",
            "Direct Coupling",
            "Indirect Coupling",
            "App Method Count",
            "System Method Count",
            "Call Edge Count",
            "Avg Out Degree",
            "Max Out Degree",
            "Reachable Method Count Avg",
            "SCC Count",
            "Largest SCC Ratio",
            "App To System Call Ratio",
            "Sensitive API Type Count",
            "Sensitive API Types",
            "Sensitive API Call Count",
            "Reflection Call Count",
            "Dynamic Load Call Count",
            "Native Load Call Count",
            "Runtime Exec Call Count",
            "Component Entry Count",
            "Total Permissions",
            "Dangerous Permission Count",
            "Dangerous Permission Ratio",
            "High Risk Permission Count",
            "SMS Permission Flag",
            "Location Permission Flag",
            "Phone Permission Flag",
            "Audio Permission Flag",
            "Storage Permission Flag",
            "Camera Permission Flag",
            "Activity Count",
            "Service Count",
            "Receiver Count",
            "Provider Count",
            "Has Debuggable",
            "Has Allow Backup",
            "Has Cleartext Traffic",
            "Min SDK",
            "Target SDK",
            "Custom Permission Count",
            "URL Count",
            "IP Count",
            "Suspicious Keyword Count",
            "High Entropy String Count",
            "DEX Count",
            "DEX Total Size",
            "GLCM Contrast Mean",
            "GLCM Contrast Std",
            "GLCM Dissimilarity Mean",
            "GLCM Dissimilarity Std",
            "GLCM Homogeneity Mean",
            "GLCM Homogeneity Std",
            "GLCM Energy Mean",
            "GLCM Energy Std",
            "GLCM Correlation Mean",
            "GLCM Correlation Std",
            "GLCM ASM Mean",
            "GLCM ASM Std",
            "GLCM Entropy Mean",
            "GLCM Entropy Std",
            "Byte Entropy Mean",
            "Byte Entropy Std",
            "Gray Mean",
            "Gray Std",
            "Edge Density Mean",
            "Edge Density Std",
            "Malicious Label"
    };

    private static final int PATH_COLUMN_INDEX = 0;
    private static final int MALICIOUS_LABEL_COLUMN_INDEX = HEADERS.length - 1;

    private Workbook workbook;
    private Sheet sheet;
    private int rowNum = 0;
    private final Set<String> analyzedNames = new HashSet<>();
    private int pendingCount = 0;
    private String currentExcelPath;

    public void loadOrInit(String excelFilePath) {
        this.currentExcelPath = excelFilePath;
        File excelFile = new File(excelFilePath);
        if (excelFile.exists()) {
            try (FileInputStream fis = new FileInputStream(excelFile)) {
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheetAt(0);
                rowNum = sheet.getLastRowNum() + 1;
                ensureHeader();
                backfillMaliciousLabels();
                loadAnalyzedNames();
                rowNum = Math.max(rowNum, 1);
                System.out.println("Loaded existing Excel file, row count: " + rowNum);
                return;
            } catch (IOException e) {
                System.err.println("Failed to load Excel file, creating a new one: " + e.getMessage());
            }
        }
        initNew();
    }

    private void initNew() {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Results");
        rowNum = 0;
        Row header = sheet.createRow(rowNum++);
        writeHeader(header);
        System.out.println("Initialized new Excel file");
    }

    private void ensureHeader() {
        if (sheet == null) {
            return;
        }
        Row header = sheet.getRow(0);
        if (header == null) {
            header = sheet.createRow(0);
        }
        writeHeader(header);
    }

    private void writeHeader(Row header) {
        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }
    }

    public synchronized void writeRow(ApkAnalysisResult r) {
        analyzedNames.add(new File(r.apkPath).getName());
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(r.apkPath);
        row.createCell(1).setCellValue(r.cohesion);
        row.createCell(2).setCellValue(r.directCoupling);
        row.createCell(3).setCellValue(r.indirectCoupling);
        row.createCell(4).setCellValue(r.appMethodCount);
        row.createCell(5).setCellValue(r.systemMethodCount);
        row.createCell(6).setCellValue(r.callEdgeCount);
        row.createCell(7).setCellValue(r.avgOutDegree);
        row.createCell(8).setCellValue(r.maxOutDegree);
        row.createCell(9).setCellValue(r.reachableMethodCountAvg);
        row.createCell(10).setCellValue(r.sccCount);
        row.createCell(11).setCellValue(r.largestSccRatio);
        row.createCell(12).setCellValue(r.appToSystemCallRatio);
        row.createCell(13).setCellValue(r.sensitiveApiTypeCount);
        row.createCell(14).setCellValue(r.sensitiveApiTypes);
        row.createCell(15).setCellValue(r.sensitiveApiCallCount);
        row.createCell(16).setCellValue(r.reflectionCallCount);
        row.createCell(17).setCellValue(r.dynamicLoadCallCount);
        row.createCell(18).setCellValue(r.nativeLoadCallCount);
        row.createCell(19).setCellValue(r.runtimeExecCallCount);
        row.createCell(20).setCellValue(r.componentEntryCount);
        row.createCell(21).setCellValue(r.totalPermissions);
        row.createCell(22).setCellValue(r.dangerousPermissionCount);
        row.createCell(23).setCellValue(r.dangerousPermissionRatio);
        row.createCell(24).setCellValue(r.highRiskPermissionCount);
        row.createCell(25).setCellValue(r.smsPermissionFlag);
        row.createCell(26).setCellValue(r.locationPermissionFlag);
        row.createCell(27).setCellValue(r.phonePermissionFlag);
        row.createCell(28).setCellValue(r.audioPermissionFlag);
        row.createCell(29).setCellValue(r.storagePermissionFlag);
        row.createCell(30).setCellValue(r.cameraPermissionFlag);
        row.createCell(31).setCellValue(r.activityCount);
        row.createCell(32).setCellValue(r.serviceCount);
        row.createCell(33).setCellValue(r.receiverCount);
        row.createCell(34).setCellValue(r.providerCount);
        row.createCell(35).setCellValue(r.hasDebuggable);
        row.createCell(36).setCellValue(r.hasAllowBackup);
        row.createCell(37).setCellValue(r.hasCleartextTraffic);
        row.createCell(38).setCellValue(r.minSdk);
        row.createCell(39).setCellValue(r.targetSdk);
        row.createCell(40).setCellValue(r.customPermissionCount);
        row.createCell(41).setCellValue(r.urlCount);
        row.createCell(42).setCellValue(r.ipCount);
        row.createCell(43).setCellValue(r.suspiciousKeywordCount);
        row.createCell(44).setCellValue(r.highEntropyStringCount);
        row.createCell(45).setCellValue(r.dexCount);
        row.createCell(46).setCellValue(r.dexTotalSize);
        row.createCell(47).setCellValue(r.glcmContrast);
        row.createCell(48).setCellValue(r.glcmContrastStd);
        row.createCell(49).setCellValue(r.glcmDissimilarity);
        row.createCell(50).setCellValue(r.glcmDissimilarityStd);
        row.createCell(51).setCellValue(r.glcmHomogeneity);
        row.createCell(52).setCellValue(r.glcmHomogeneityStd);
        row.createCell(53).setCellValue(r.glcmEnergy);
        row.createCell(54).setCellValue(r.glcmEnergyStd);
        row.createCell(55).setCellValue(r.glcmCorrelation);
        row.createCell(56).setCellValue(r.glcmCorrelationStd);
        row.createCell(57).setCellValue(r.glcmAsm);
        row.createCell(58).setCellValue(r.glcmAsmStd);
        row.createCell(59).setCellValue(r.glcmEntropyMean);
        row.createCell(60).setCellValue(r.glcmEntropyStd);
        row.createCell(61).setCellValue(r.byteEntropyMean);
        row.createCell(62).setCellValue(r.byteEntropyStd);
        row.createCell(63).setCellValue(r.grayMean);
        row.createCell(64).setCellValue(r.grayStd);
        row.createCell(65).setCellValue(r.edgeDensityMean);
        row.createCell(66).setCellValue(r.edgeDensityStd);
        if (r.maliciousLabel != null) {
            row.createCell(MALICIOUS_LABEL_COLUMN_INDEX).setCellValue(r.maliciousLabel);
        }

        pendingCount++;
        if (pendingCount >= 100) {
            saveAndReload();
        }
    }

    public synchronized void save(String excelFilePath) {
        currentExcelPath = excelFilePath;
        try (FileOutputStream fos = new FileOutputStream(excelFilePath)) {
            workbook.write(fos);
            pendingCount = 0;
            System.out.println("Excel saved: " + excelFilePath);
        } catch (IOException e) {
            System.err.println("Failed to save Excel: " + e.getMessage());
        }
    }

    public synchronized boolean flushPending() {
        if (pendingCount <= 0) {
            return false;
        }
        if (currentExcelPath == null || currentExcelPath.trim().isEmpty()) {
            System.err.println("Skipping pending flush because Excel path is not set");
            return false;
        }
        System.out.println("Flushing remaining " + pendingCount + " records...");
        save(currentExcelPath);
        return true;
    }

    public void removeDuplicateRows() {
        Set<String> seen = new HashSet<>();
        List<Integer> toRemove = new ArrayList<>();
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getCell(PATH_COLUMN_INDEX) != null) {
                String name = new File(row.getCell(PATH_COLUMN_INDEX).getStringCellValue()).getName();
                if (seen.contains(name)) {
                    toRemove.add(i);
                } else {
                    seen.add(name);
                }
            }
        }

        for (int i = toRemove.size() - 1; i >= 0; i--) {
            int idx = toRemove.get(i);
            if (idx < sheet.getLastRowNum()) {
                sheet.shiftRows(idx + 1, sheet.getLastRowNum(), -1);
            } else {
                Row row = sheet.getRow(idx);
                if (row != null) {
                    sheet.removeRow(row);
                }
            }
            rowNum--;
        }

        System.out.println("Removed duplicate rows: " + toRemove.size());
    }

    private void loadAnalyzedNames() {
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getCell(PATH_COLUMN_INDEX) != null) {
                analyzedNames.add(new File(row.getCell(PATH_COLUMN_INDEX).getStringCellValue()).getName());
            }
        }
    }

    public boolean isAlreadyAnalyzed(String apkFileName) {
        return analyzedNames.contains(apkFileName);
    }

    private void backfillMaliciousLabels() {
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || row.getCell(PATH_COLUMN_INDEX) == null) {
                continue;
            }

            if (row.getCell(MALICIOUS_LABEL_COLUMN_INDEX) != null
                    && !row.getCell(MALICIOUS_LABEL_COLUMN_INDEX).toString().trim().isEmpty()) {
                continue;
            }

            Integer maliciousLabel = SampleLabelResolver.inferFromPath(
                    row.getCell(PATH_COLUMN_INDEX).getStringCellValue()
            );
            if (maliciousLabel != null) {
                row.createCell(MALICIOUS_LABEL_COLUMN_INDEX).setCellValue(maliciousLabel);
            }
        }
    }

    private void saveAndReload() {
        System.out.println("Saving batch of " + pendingCount + " records...");
        save(currentExcelPath);

        try {
            workbook.close();
        } catch (IOException e) {
            System.err.println("Failed to close workbook: " + e.getMessage());
        }

        try (FileInputStream fis = new FileInputStream(currentExcelPath)) {
            workbook = new XSSFWorkbook(fis);
            sheet = workbook.getSheetAt(0);
            rowNum = sheet.getLastRowNum() + 1;
            System.out.println("Reloaded Excel, current row count: " + rowNum);
        } catch (IOException e) {
            System.err.println("Failed to reload Excel: " + e.getMessage());
        }
    }
}

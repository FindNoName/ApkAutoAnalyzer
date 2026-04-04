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

    private Workbook workbook;
    private Sheet sheet;
    private int rowNum = 0;

    public void loadOrInit(String excelFilePath) {
        File excelFile = new File(excelFilePath);
        if (excelFile.exists()) {
            try (FileInputStream fis = new FileInputStream(excelFile)) {
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheetAt(0);
                rowNum = sheet.getLastRowNum() + 1;
                System.out.println("已加载现有 Excel 文件，当前行号: " + rowNum);
            } catch (IOException e) {
                System.err.println("加载 Excel 失败，初始化新文件: " + e.getMessage());
                initNew();
            }
        } else {
            initNew();
        }
    }

    private void initNew() {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Results");
        rowNum = 0;
        Row header = sheet.createRow(rowNum++);
        header.createCell(0).setCellValue("APK Path");
        header.createCell(1).setCellValue("Cohesion");
        header.createCell(2).setCellValue("Direct Coupling");
        header.createCell(3).setCellValue("Indirect Coupling");
        header.createCell(4).setCellValue("Total Permissions");
        header.createCell(5).setCellValue("Permission Groups");
        header.createCell(6).setCellValue("Permission Names");
        header.createCell(7).setCellValue("Matched Permissions");
        header.createCell(8).setCellValue("Permission Ratio");
        header.createCell(9).setCellValue("GLCM Contrast");
        header.createCell(10).setCellValue("GLCM Dissimilarity");
        header.createCell(11).setCellValue("GLCM Homogeneity");
        header.createCell(12).setCellValue("GLCM Energy");
        header.createCell(13).setCellValue("GLCM Correlation");
        header.createCell(14).setCellValue("GLCM ASM");
        System.out.println("已初始化新 Excel 文件");
    }

    public synchronized void writeRow(ApkAnalysisResult r) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(r.apkPath);
        row.createCell(1).setCellValue(r.cohesion);
        row.createCell(2).setCellValue(r.directCoupling);
        row.createCell(3).setCellValue(r.indirectCoupling);
        row.createCell(4).setCellValue(r.totalPermissions);
        row.createCell(5).setCellValue(r.permissionGroups);
        row.createCell(6).setCellValue(r.permissionNames);
        row.createCell(7).setCellValue(r.matchedPermissions);
        row.createCell(8).setCellValue(r.permissionRatio);
        row.createCell(9).setCellValue(r.glcmContrast);
        row.createCell(10).setCellValue(r.glcmDissimilarity);
        row.createCell(11).setCellValue(r.glcmHomogeneity);
        row.createCell(12).setCellValue(r.glcmEnergy);
        row.createCell(13).setCellValue(r.glcmCorrelation);
        row.createCell(14).setCellValue(r.glcmAsm);
    }

    public void save(String excelFilePath) {
        try (FileOutputStream fos = new FileOutputStream(excelFilePath)) {
            workbook.write(fos);
            System.out.println("Excel 已保存: " + excelFilePath);
        } catch (IOException e) {
            System.err.println("保存 Excel 失败: " + e.getMessage());
        }
    }

    public void removeDuplicateRows() {
        Set<String> seen = new HashSet<>();
        List<Integer> toRemove = new ArrayList<>();
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getCell(0) != null) {
                String name = new File(row.getCell(0).getStringCellValue()).getName();
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
                Row r = sheet.getRow(idx);
                if (r != null) sheet.removeRow(r);
            }
            rowNum--;
        }
        System.out.println("已删除 " + toRemove.size() + " 行重复数据");
    }

    public boolean isAlreadyAnalyzed(String apkFileName) {
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getCell(0) != null) {
                String name = new File(row.getCell(0).getStringCellValue()).getName();
                if (name.equals(apkFileName)) return true;
            }
        }
        return false;
    }
}

package tw.com.ispan.eeit.ho_back.support_search;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import tw.com.ispan.eeit.ho_back.support.SupportBean;

@Component
public class SupportReportExcelBuilder {
        public byte[] buildExcel(List<SupportBean> list) {

                try (Workbook wb = new XSSFWorkbook()) {

                        Sheet sheet = wb.createSheet("Support Report");

                        // 標題列
                        Row header = sheet.createRow(0);
                        String[] titles = {
                                        "案件編號", "用戶ID", "用戶姓名", "案件主旨",
                                        "分類", "狀態", "結案代碼",
                                        "結案人員ID", "結案人員姓名",
                                        "建立時間", "結案時間"
                        };

                        for (int i = 0; i < titles.length; i++) {
                                header.createCell(i).setCellValue(titles[i]);
                                sheet.setColumnWidth(i, 5000);
                        }

                        int rowIdx = 1;

                        for (SupportBean s : list) {

                                Row row = sheet.createRow(rowIdx++);

                                // 1. 案件編號
                                row.createCell(0).setCellValue(
                                                s.getCaseCode() != null ? s.getCaseCode() : "—");

                                // 2. 用戶 ID
                                row.createCell(1).setCellValue(
                                                s.getUser() != null && s.getUser().getId() != null
                                                                ? s.getUser().getId().toString()
                                                                : "—");

                                // 3. 用戶名稱
                                row.createCell(2).setCellValue(
                                                s.getUser() != null ? s.getUser().getFirstName() : "—");

                                // 4. 案件主旨
                                row.createCell(3).setCellValue(
                                                s.getTitle() != null ? s.getTitle() : "—");

                                // 5. 分類
                                row.createCell(4).setCellValue(
                                                s.getSCategory() != null ? s.getSCategory().getName() : "—");

                                // 6. 狀態
                                String statusText = "—";
                                if (s.getStatus() != null) {
                                        statusText = s.getStatus() == 0 ? "未處理"
                                                        : s.getStatus() == 1 ? "處理中"
                                                                        : "已結案";
                                }
                                row.createCell(5).setCellValue(statusText);

                                // 7. 結案代碼
                                row.createCell(6).setCellValue(
                                                s.getReason() != null ? s.getReason().getCode() : "—");

                                // 8. 結案人員 ID
                                row.createCell(7).setCellValue(
                                                s.getUpdatedBy() != null && s.getUpdatedBy().getId() != null
                                                                ? s.getUpdatedBy().getId().toString()
                                                                : "—");

                                // 9. 結案人員姓名
                                row.createCell(8).setCellValue(
                                                s.getUpdatedBy() != null ? s.getUpdatedBy().getFirstName() : "—");

                                // 10. 建立時間
                                row.createCell(9).setCellValue(
                                                s.getCreatedTime() != null ? s.getCreatedTime().toString() : "—");

                                // 11. 結案時間
                                row.createCell(10).setCellValue(
                                                s.getUpdatedTime() != null ? s.getUpdatedTime().toString() : "—");
                        }

                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        wb.write(out);
                        return out.toByteArray();

                } catch (Exception ex) {
                        ex.printStackTrace();
                }

                return new byte[0];
        }
}
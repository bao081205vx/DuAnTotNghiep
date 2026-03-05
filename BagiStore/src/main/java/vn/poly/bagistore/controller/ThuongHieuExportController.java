package vn.poly.bagistore.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.poly.bagistore.Service.ThuongHieuService;
import vn.poly.bagistore.model.ThuongHieu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/thuong-hieu")
public class ThuongHieuExportController {

    @Autowired
    private ThuongHieuService thuongHieuService;

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportThuongHieuToExcel() throws IOException {
        List<ThuongHieu> thuongHieuList = thuongHieuService.findAllThuongHieu();

        // Tạo workbook Excel
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Danh sách thương hiệu");

        // Tạo header
        Row headerRow = sheet.createRow(0);
        String[] columns = {"STT", "Mã thương hiệu", "Tên thương hiệu", "Ngày tạo", "Trạng thái"};

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerCellStyle.setFont(headerFont);

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerCellStyle);
        }

        // Điền dữ liệu
        int rowNum = 1;
        for (ThuongHieu thuongHieu : thuongHieuList) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(rowNum - 1);
            row.createCell(1).setCellValue(thuongHieu.getMaThuongHieu());
            row.createCell(2).setCellValue(thuongHieu.getTenThuongHieu());
            row.createCell(3).setCellValue(thuongHieu.getNgayTao().toString());
            row.createCell(4).setCellValue(thuongHieu.getTrangThai() ? "Hoạt động" : "Ngừng hoạt động");
        }

        // Tự động điều chỉnh kích thước cột
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Ghi workbook ra ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=danh-sach-thuong-hieu.xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(inputStream));
    }
}
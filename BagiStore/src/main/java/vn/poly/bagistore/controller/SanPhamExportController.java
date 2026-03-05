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
import vn.poly.bagistore.Service.SanPhamService;
import vn.poly.bagistore.dto.ProductSummaryDTO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/san-pham")
public class SanPhamExportController {

    @Autowired
    private SanPhamService sanPhamService;

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportProductsToExcel() throws IOException {
        List<ProductSummaryDTO> products = sanPhamService.getProductSummaries();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Danh sách sản phẩm");

        String[] columns = {"Mã sản phẩm", "Tên sản phẩm", "Thương hiệu", "Chất liệu", "Số lượng tồn", "Trạng thái"};

        Row headerRow = sheet.createRow(0);
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

        int rowNum = 1;
        for (ProductSummaryDTO p : products) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(p.getMaSanPham() == null ? "" : p.getMaSanPham());
            row.createCell(1).setCellValue(p.getTenSanPham() == null ? "" : p.getTenSanPham());
            row.createCell(2).setCellValue(p.getThuongHieu() == null ? "" : p.getThuongHieu());
            row.createCell(3).setCellValue(p.getChatLieu() == null ? "" : p.getChatLieu());
            row.createCell(4).setCellValue(p.getSoLuongTon() == null ? 0 : p.getSoLuongTon());
            row.createCell(5).setCellValue(p.getTrangThai() != null && p.getTrangThai() ? "Hoạt động" : "Ngưng hoạt động");
        }

        for (int i = 0; i < columns.length; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=danh-sach-san-pham.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}

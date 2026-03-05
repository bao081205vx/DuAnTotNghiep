package vn.poly.bagistore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductSummaryDTO {
    private Integer id;
    private String maSanPham;
    private String tenSanPham;
    private String thuongHieu;
    private String chatLieu;
    private Integer soLuongTon;
    private Boolean trangThai;
}

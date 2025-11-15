package vn.poly.bagistore.dto;

import lombok.Data;

@Data
public class SanPhamChiTietDTO {
    private String ma;
    private String tenSanPham;
    private String mau;
    private String size;
    private Double gia;
    private String anh;
    private String trangThai;
}

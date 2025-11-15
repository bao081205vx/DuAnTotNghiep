package vn.poly.bagistore.dto;

import lombok.Data;

@Data
public class VariantCreateDTO {
    private Integer idMauSac;
    private Integer idKichThuoc;
    private String tenMauSac;
    private String tenKichThuoc;
    private Double gia;
    private Integer soLuong;
    private Boolean trangThai;
    private Integer idTrongLuong;
    private String tenTrongLuong;
    private String anhDuongDan;

    // Thêm constructor mặc định
    public VariantCreateDTO() {}
}
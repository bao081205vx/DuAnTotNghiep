package vn.poly.bagistore.dto;

import lombok.Data;

@Data
public class VariantUpdateDTO {
    private String maVariant;
    private String tenMauSac;
    private String tenKichThuoc;
    private String tenTrongLuong;
    private Integer soLuong;
    private Double gia;
    private Boolean trangThai;
    private String moTa;
    private String anhDuongDan;
    private String maMau;
}
package vn.poly.bagistore.dto;

import lombok.Data;

@Data
public class InvoiceItemCreateDTO {
    private Integer variantId;
    private Integer soLuong;
    private Double donGia;
    private String moTa;
}

package vn.poly.bagistore.dto;

import lombok.Data;
import java.util.List;

@Data
public class InvoiceCreateDTO {
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String loaiHoaDon;
    private String ghiChu;
    private Boolean isPickup;
    private Double tongTien;
    private Double tongTienSauGiam;
    private Integer phuongThucId;
    private Integer phieuGiamGiaId;
    private List<InvoiceItemCreateDTO> items;
}

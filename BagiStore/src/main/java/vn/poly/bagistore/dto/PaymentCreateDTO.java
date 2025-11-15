package vn.poly.bagistore.dto;

public class PaymentCreateDTO {
    private Double soTien;
    private Integer phuongThucId;

    public Double getSoTien() { return soTien; }
    public void setSoTien(Double soTien) { this.soTien = soTien; }
    public Integer getPhuongThucId() { return phuongThucId; }
    public void setPhuongThucId(Integer phuongThucId) { this.phuongThucId = phuongThucId; }
}

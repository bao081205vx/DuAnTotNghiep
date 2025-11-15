package vn.poly.bagistore.dto;

import java.time.LocalDateTime;

public class PaymentHistoryDTO {
    private Integer id;
    private Double soTien;
    private String hinhThuc;
    private LocalDateTime ngayThanhToan;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Double getSoTien() { return soTien; }
    public void setSoTien(Double soTien) { this.soTien = soTien; }
    public String getHinhThuc() { return hinhThuc; }
    public void setHinhThuc(String hinhThuc) { this.hinhThuc = hinhThuc; }
    public LocalDateTime getNgayThanhToan() { return ngayThanhToan; }
    public void setNgayThanhToan(LocalDateTime ngayThanhToan) { this.ngayThanhToan = ngayThanhToan; }
}
